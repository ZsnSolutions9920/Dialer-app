const express = require('express');
const VoiceResponse = require('twilio').twiml.VoiceResponse;
const config = require('../config');
const agentService = require('../services/agentService');
const callService = require('../services/callService');
const contactService = require('../services/contactService');
const phoneListService = require('../services/phoneListService');
const minutesService = require('../services/minutesService');
const validateTwilio = require('../middleware/validateTwilio');
const logger = require('../utils/logger');

const router = express.Router();

// Main voice webhook - handles both outbound (from TwiML App) and inbound calls
router.post('/voice', validateTwilio, async (req, res) => {
  const twiml = new VoiceResponse();
  const { To, From, CallSid, Direction } = req.body;

  logger.info({ To, From, CallSid, Direction }, 'Voice webhook hit');

  try {
    // Find the agent by their Twilio identity (needed for both outbound caller ID and tracking)
    const fromIdentity = From ? From.replace('client:', '') : null;
    const agent = fromIdentity ? await agentService.findByIdentity(fromIdentity) : null;
    const agentPhone = agent?.twilio_phone_number || null;

    // Mobile app joining a conference for inbound call answer
    if (From && From.startsWith('client:') && To && To.startsWith('conference:')) {
      const conferenceName = To.replace('conference:', '');
      logger.info({ conferenceName, agent: agent?.id }, 'Mobile client joining conference');

      const dial = twiml.dial();
      dial.conference(
        {
          startConferenceOnEnter: true,
          endConferenceOnExit: true,
          statusCallback: `${config.serverBaseUrl}/api/twilio/conference-status`,
          statusCallbackEvent: 'start end join leave',
        },
        conferenceName
      );

      if (agent) {
        await agentService.updateStatus(agent.id, 'on_call');
        const io = req.app.get('io');
        if (io) {
          io.emit('agent:status', { id: agent.id, status: 'on_call' });
        }
      }

      res.type('text/xml');
      return res.send(twiml.toString());
    }

    // Outbound call from agent (From is a browser client, To is a phone number)
    if (From && From.startsWith('client:') && To && !To.startsWith('client:')) {
      // Block outbound calls if agent has no assigned Twilio number
      if (!agentPhone) {
        logger.warn({ agentId: agent?.id, fromIdentity }, 'Agent has no Twilio phone number assigned');
        twiml.say('You do not have a phone number assigned. Please contact your administrator.');
        twiml.hangup();
        res.type('text/xml');
        return res.send(twiml.toString());
      }

      // Check minutes balance if agent is linked to a customer
      if (agent) {
        const customerId = await minutesService.getCustomerIdForAgent(agent.id);
        if (customerId) {
          const remaining = await minutesService.getRemaining(customerId);
          if (remaining <= 0) {
            logger.warn({ agentId: agent.id, customerId }, 'No minutes remaining');
            twiml.say('You have no minutes remaining. Please purchase more minutes to continue making calls.');
            twiml.hangup();
            res.type('text/xml');
            return res.send(twiml.toString());
          }
          logger.info({ agentId: agent.id, customerId, remaining }, 'Minutes check passed');
        }
      }

      const conferenceName = `conf_${CallSid}`;

      // Put the agent into a conference
      const dial = twiml.dial({ callerId: agentPhone });
      dial.conference(
        {
          startConferenceOnEnter: true,
          endConferenceOnExit: true,
          record: 'record-from-start',
          recordingStatusCallback: `${config.serverBaseUrl}/api/twilio/recording-status`,
          recordingStatusCallbackEvent: 'completed',
          statusCallback: `${config.serverBaseUrl}/api/twilio/conference-status`,
          statusCallbackEvent: 'start end join leave',
          waitUrl: `${config.serverBaseUrl}/api/twilio/hold-music`,
        },
        conferenceName
      );

      // Track the active call and log BEFORE sending TwiML so the record
      // exists when the conference-start webhook fires
      if (agent) {
        await callService.createActiveCall({
          callSid: CallSid,
          conferenceName,
          agentId: agent.id,
          direction: 'outbound',
          from: agentPhone,
          to: To,
          status: 'in-progress',
        });

        await callService.createCallLog({
          callSid: CallSid,
          direction: 'outbound',
          agentId: agent.id,
          from: agentPhone,
          to: To,
          status: 'initiated',
        });

        await agentService.updateStatus(agent.id, 'on_call');

        const io = req.app.get('io');
        if (io) {
          io.emit('agent:status', { id: agent.id, status: 'on_call' });
        }
      }

      // Dial the external number into the conference via REST API
      const twilioClient = require('../services/twilioClient');
      twilioClient.conferences(conferenceName)
        .participants
        .create({
          to: To,
          from: agentPhone,
          earlyMedia: true,
          endConferenceOnExit: true,
          statusCallback: `${config.serverBaseUrl}/api/twilio/conference-status`,
          statusCallbackEvent: ['initiated', 'ringing', 'answered', 'completed'],
        })
        .then(async (participant) => {
          logger.info({ conferenceName, participantSid: participant.callSid, to: To, from: agentPhone }, 'External participant added');

          if (agent) {
            const io = req.app.get('io');
            if (io) {
              io.to(`agent:${agent.id}`).emit('call:outbound', {
                callSid: CallSid,
                conferenceName,
                agentId: agent.id,
                to: To,
                participantCallSid: participant.callSid,
              });
            }
          }
        })
        .catch((err) => {
          logger.error({ err, to: To, from: agentPhone, conferenceName, agentId: agent?.id }, 'Failed to add external participant');

          // Notify the agent about the failure via Socket.IO
          if (agent) {
            const io2 = req.app.get('io');
            if (io2) {
              const errMessage = err.message || 'Failed to connect the call';
              io2.to(`agent:${agent.id}`).emit('call:error', {
                conferenceName,
                message: `Call to ${To} failed: ${errMessage}`,
                code: err.code || err.status,
              });
            }
          }
        });

      res.type('text/xml');
      return res.send(twiml.toString());
    }

    // Inbound call — look up agent by the called number
    if (Direction === 'inbound' || To === config.twilio.phoneNumber || await agentService.findByPhoneNumber(To)) {
      logger.info({ From, CallSid, To }, 'Inbound call received');

      const ownerAgent = await agentService.findByPhoneNumber(To);

      if (ownerAgent) {
        // Route to the specific agent who owns the called number
        if (ownerAgent.status === 'available') {
          const dial = twiml.dial({
            callerId: From,
            timeout: 30,
            record: 'record-from-answer',
            recordingStatusCallback: `${config.serverBaseUrl}/api/twilio/recording-status`,
            recordingStatusCallbackEvent: 'completed',
            action: `${config.serverBaseUrl}/api/twilio/voice-action`,
          });
          dial.client(ownerAgent.twilio_identity);
        } else {
          twiml.say('We are sorry, the agent you are trying to reach is not available right now. Please try again later.');
          twiml.hangup();
        }
      } else {
        // Fallback: no agent owns this number — ring all available agents (shared number behavior)
        const availableAgents = await agentService.listAvailable();

        if (availableAgents.length === 0) {
          twiml.say('We are sorry, no agents are available right now. Please try again later.');
          twiml.hangup();
          res.type('text/xml');
          return res.send(twiml.toString());
        }

        const dial = twiml.dial({
          callerId: From,
          timeout: 30,
          record: 'record-from-answer',
          recordingStatusCallback: `${config.serverBaseUrl}/api/twilio/recording-status`,
          recordingStatusCallbackEvent: 'completed',
          action: `${config.serverBaseUrl}/api/twilio/voice-action`,
        });

        for (const a of availableAgents) {
          dial.client(a.twilio_identity);
        }
      }

      // Log the inbound call
      await callService.createCallLog({
        callSid: CallSid,
        direction: 'inbound',
        from: From,
        to: To,
        status: 'ringing',
        agentId: ownerAgent ? ownerAgent.id : undefined,
      });

      // Look up caller name from phone list entries and contacts
      let callerName = null;
      try {
        const agentIdForLookup = ownerAgent ? ownerAgent.id : null;
        // Check phone list entries first (power dial sheets)
        callerName = await phoneListService.findNameByPhone(From, agentIdForLookup);
        // Fall back to contacts table
        if (!callerName) {
          const contact = await contactService.getContactByPhone(From);
          if (contact) callerName = contact.name;
        }
      } catch (err) {
        logger.warn({ err, From }, 'Failed to look up caller name');
      }

      // Notify the relevant agent(s) of incoming call via Socket.IO
      const io = req.app.get('io');
      if (io) {
        if (ownerAgent) {
          io.to(`agent:${ownerAgent.id}`).emit('call:incoming', {
            callSid: CallSid,
            from: From,
            callerName,
          });
        } else {
          // Shared number — notify all agents
          io.emit('call:incoming', {
            callSid: CallSid,
            from: From,
            callerName,
          });
        }
      }

      res.type('text/xml');
      return res.send(twiml.toString());
    }

    // Default
    twiml.say('Unexpected call routing. Please contact support.');
    res.type('text/xml');
    res.send(twiml.toString());
  } catch (err) {
    logger.error(err, 'Error in voice webhook');
    twiml.say('An error occurred. Please try again.');
    res.type('text/xml');
    res.send(twiml.toString());
  }
});

// Action URL after inbound dial completes - redirect accepted call into conference
router.post('/voice-action', validateTwilio, async (req, res) => {
  const twiml = new VoiceResponse();
  const { DialCallStatus, CallSid, DialCallSid, From } = req.body;

  logger.info({ DialCallStatus, CallSid, DialCallSid }, 'Voice action callback');

  try {
    if (DialCallStatus === 'completed' || DialCallStatus === 'answered') {
      // The call was answered - it will have already been handled
      // Just acknowledge
      twiml.hangup();
    } else {
      // No one answered
      twiml.say('We are sorry, no agents are available right now. Please try again later.');
      twiml.hangup();

      const missedLog = await callService.updateCallLog(CallSid, {
        status: 'no-answer',
        endedAt: new Date(),
      });

      // Notify agent(s) about the missed inbound call
      if (missedLog) {
        const io = req.app.get('io');
        if (io) {
          if (missedLog.agent_id) {
            io.to(`agent:${missedLog.agent_id}`).emit('call:missed', {
              callSid: CallSid,
              from: missedLog.from_number,
            });
          } else {
            // Shared number - notify all agents
            io.emit('call:missed', {
              callSid: CallSid,
              from: missedLog.from_number,
            });
          }
        }
      }
    }
  } catch (err) {
    logger.error(err, 'Error in voice-action');
    twiml.hangup();
  }

  res.type('text/xml');
  res.send(twiml.toString());
});

// Conference status callback
router.post('/conference-status', validateTwilio, async (req, res) => {
  const {
    ConferenceSid,
    FriendlyName,
    StatusCallbackEvent,
    CallSid,
    Muted,
    Hold,
  } = req.body;

  logger.info({ ConferenceSid, FriendlyName, StatusCallbackEvent, CallSid }, 'Conference status event');

  try {
    const io = req.app.get('io');

    if (StatusCallbackEvent === 'conference-start') {
      // Update active calls with conference SID
      const activeCall = await callService.getActiveCallByConference(FriendlyName);
      if (activeCall) {
        await callService.createActiveCall({
          callSid: activeCall.call_sid,
          conferenceSid: ConferenceSid,
          conferenceName: FriendlyName,
          agentId: activeCall.agent_id,
          direction: activeCall.direction,
          from: activeCall.from_number,
          to: activeCall.to_number,
          status: 'in-progress',
        });

        await callService.updateCallLog(activeCall.call_sid, {
          conferenceSid: ConferenceSid,
          status: 'in-progress',
        });

        if (io) {
          io.to(`agent:${activeCall.agent_id}`).emit('call:conference-started', {
            conferenceSid: ConferenceSid,
            conferenceName: FriendlyName,
          });
        }
      }
    }

    if (StatusCallbackEvent === 'participant-join') {
      const activeCall = await callService.getActiveCallByConference(FriendlyName);
      if (io && activeCall) {
        io.to(`agent:${activeCall.agent_id}`).emit('call:participant-joined', { conferenceSid: ConferenceSid, callSid: CallSid, conferenceName: FriendlyName });
      }
    }

    if (StatusCallbackEvent === 'participant-leave') {
      const activeCall = await callService.getActiveCallByConference(FriendlyName);
      if (io && activeCall) {
        io.to(`agent:${activeCall.agent_id}`).emit('call:participant-left', { conferenceSid: ConferenceSid, callSid: CallSid, conferenceName: FriendlyName });
      }
    }

    if (StatusCallbackEvent === 'conference-end') {
      // Clean up active calls
      await callService.removeActiveCallsByConference(FriendlyName);

      // Update call log
      const { rows } = await require('../db/pool').query(
        'SELECT * FROM call_logs WHERE conference_sid = $1',
        [ConferenceSid]
      );
      for (const log of rows) {
        if (!log.ended_at) {
          const now = new Date();
          const durationSeconds = Math.round((now - new Date(log.started_at)) / 1000);
          await callService.updateCallLog(log.call_sid, {
            status: 'completed',
            endedAt: now,
            durationSeconds,
          });

          // Deduct minutes from customer balance
          if (log.agent_id && durationSeconds > 0) {
            try {
              const customerId = await minutesService.getCustomerIdForAgent(log.agent_id);
              if (customerId) {
                const minutesUsed = Math.ceil(durationSeconds / 60); // round up
                const deducted = await minutesService.deduct(customerId, minutesUsed);
                logger.info({ customerId, agentId: log.agent_id, durationSeconds, minutesUsed, deducted }, 'Minutes deducted');
              }
            } catch (deductErr) {
              logger.error(deductErr, 'Failed to deduct minutes');
            }
          }
        }
        if (log.agent_id) {
          await agentService.updateStatus(log.agent_id, 'available');
          if (io) {
            io.emit('agent:status', { id: log.agent_id, status: 'available' });
          }
        }
      }

      // Notify the agent(s) involved that the call ended
      for (const log of rows) {
        if (log.agent_id && io) {
          io.to(`agent:${log.agent_id}`).emit('call:ended', { conferenceSid: ConferenceSid, conferenceName: FriendlyName });
        }
      }

      // Fetch recording from Twilio API after a delay (recording takes time to process)
      const callSidsToCheck = rows.map((log) => log.call_sid).filter(Boolean);
      setTimeout(async () => {
        try {
          const twilioClient = require('../services/twilioClient');
          const recordings = await twilioClient.recordings.list({
            conferenceSid: ConferenceSid,
            limit: 1,
          });
          if (recordings.length > 0) {
            const rec = recordings[0];
            const recordingUrl = `https://api.twilio.com/2010-04-01/Accounts/${config.twilio.accountSid}/Recordings/${rec.sid}`;
            for (const callSid of callSidsToCheck) {
              await callService.updateCallLog(callSid, {
                recordingSid: rec.sid,
                recordingUrl,
              });
            }
            logger.info({ conferenceSid: ConferenceSid, recordingSid: rec.sid }, 'Recording saved from API');
          }
        } catch (err) {
          logger.error(err, 'Failed to fetch recording from Twilio API');
        }
      }, 15000);
    }
  } catch (err) {
    logger.error(err, 'Error handling conference status');
  }

  res.sendStatus(200);
});

// Hold music TwiML
router.post('/hold-music', validateTwilio, (req, res) => {
  const twiml = new VoiceResponse();
  twiml.play({ loop: 0 }, 'https://api.twilio.com/cowbell.mp3');
  res.type('text/xml');
  res.send(twiml.toString());
});

// Recording status callback - Twilio calls this when a recording is ready
router.post('/recording-status', validateTwilio, async (req, res) => {
  const { RecordingSid, RecordingUrl, CallSid, ConferenceSid } = req.body;

  logger.info({ RecordingSid, RecordingUrl, CallSid, ConferenceSid }, 'Recording status callback');

  try {
    if (!RecordingSid || !RecordingUrl) {
      return res.sendStatus(200);
    }

    const pool = require('../db/pool');

    // For inbound calls (Dial recording) — match by CallSid
    // For outbound calls (Conference recording) — match by ConferenceSid
    let updated = false;

    if (CallSid) {
      const { rowCount } = await pool.query(
        'UPDATE call_logs SET recording_sid = $1, recording_url = $2 WHERE call_sid = $3 AND recording_sid IS NULL',
        [RecordingSid, RecordingUrl, CallSid]
      );
      if (rowCount > 0) updated = true;
    }

    if (!updated && ConferenceSid) {
      await pool.query(
        'UPDATE call_logs SET recording_sid = $1, recording_url = $2 WHERE conference_sid = $3 AND recording_sid IS NULL',
        [RecordingSid, RecordingUrl, ConferenceSid]
      );
    }
  } catch (err) {
    logger.error(err, 'Error handling recording status');
  }

  res.sendStatus(200);
});

// Mobile app: accept an inbound call by bridging into a conference
router.post('/accept-mobile', async (req, res) => {
  const { callSid, fromNumber } = req.body;

  if (!callSid && !fromNumber) {
    return res.status(400).json({ error: 'callSid or fromNumber is required' });
  }

  try {
    const twilioClient = require('../services/twilioClient');
    const conferenceName = `conf_mobile_${callSid || Date.now()}`;

    // Try to redirect the original inbound call into the conference
    let redirected = false;
    if (callSid) {
      try {
        const confTwiml = new VoiceResponse();
        const dial = confTwiml.dial();
        dial.conference(
          {
            startConferenceOnEnter: true,
            endConferenceOnExit: true,
            record: 'record-from-start',
            recordingStatusCallback: `${config.serverBaseUrl}/api/twilio/recording-status`,
            recordingStatusCallbackEvent: 'completed',
            statusCallback: `${config.serverBaseUrl}/api/twilio/conference-status`,
            statusCallbackEvent: 'start end join leave',
            waitUrl: `${config.serverBaseUrl}/api/twilio/hold-music`,
          },
          conferenceName
        );
        await twilioClient.calls(callSid).update({ twiml: confTwiml.toString() });
        redirected = true;
        logger.info({ callSid, conferenceName }, 'Inbound call redirected to conference');
      } catch (redirectErr) {
        logger.warn({ callSid, err: redirectErr.message }, 'Could not redirect original call, will dial caller into conference');
      }
    }

    // If redirect failed (call ended/answered), dial the caller into the conference
    if (!redirected && fromNumber) {
      // Find an agent phone to use as caller ID
      const agents = await agentService.listAll();
      const agentPhone = agents.length > 0 ? agents[0].twilio_phone_number : config.twilio.phoneNumber;

      await twilioClient.conferences(conferenceName)
        .participants
        .create({
          to: fromNumber,
          from: agentPhone || config.twilio.phoneNumber,
          earlyMedia: true,
          endConferenceOnExit: true,
          statusCallback: `${config.serverBaseUrl}/api/twilio/conference-status`,
          statusCallbackEvent: ['initiated', 'ringing', 'answered', 'completed'],
        });
      logger.info({ fromNumber, conferenceName }, 'Dialed caller into conference as fallback');
    }

    res.json({ conferenceName });
  } catch (err) {
    logger.error(err, 'Error accepting inbound call for mobile');
    res.status(500).json({ error: 'Failed to accept call: ' + err.message });
  }
});

module.exports = router;
