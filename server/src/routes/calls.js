const express = require('express');
const authMiddleware = require('../middleware/auth');
const callService = require('../services/callService');
const statsService = require('../services/statsService');
const agentService = require('../services/agentService');
const twilioClient = require('../services/twilioClient');
const config = require('../config');
const logger = require('../utils/logger');

const router = express.Router();

// Active calls list for monitoring
router.get('/active', authMiddleware, async (req, res) => {
  try {
    const activeCalls = await callService.getAllActiveCalls();
    res.json(activeCalls);
  } catch (err) {
    logger.error(err, 'Error fetching active calls');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Join conference as coach (listen/whisper)
router.post('/monitor', authMiddleware, async (req, res) => {
  try {
    const { conferenceName } = req.body;
    if (!conferenceName) {
      return res.status(400).json({ error: 'conferenceName is required' });
    }

    // Look up the active call to get the agent's call_sid for coaching
    const activeCall = await callService.getActiveCallByConference(conferenceName);
    if (!activeCall) {
      return res.status(404).json({ error: 'Active call not found for this conference' });
    }

    // Add admin as coach participant (muted = listen-only)
    // Use conference_sid if available, otherwise use friendly name directly (same as transfer)
    const conferenceId = activeCall.conference_sid || conferenceName;
    const monitorIdentity = `monitor_${req.agent.id}`;
    const participant = await twilioClient.conferences(conferenceId)
      .participants
      .create({
        to: `client:${monitorIdentity}`,
        from: config.twilio.phoneNumber,
        coach: activeCall.call_sid,
        muted: true,
        endConferenceOnExit: false,
      });

    const conferenceSid = participant.conferenceSid || activeCall.conference_sid;
    logger.info({ conferenceName, conferenceSid, monitorIdentity }, 'Monitor joined conference');
    res.json({ ok: true, participantCallSid: participant.callSid, conferenceSid });
  } catch (err) {
    logger.error(err, 'Error starting monitor');
    res.status(500).json({ error: 'Failed to start monitoring' });
  }
});

// Toggle monitor mode (muted=true → listen, muted=false → whisper)
router.post('/monitor/mode', authMiddleware, async (req, res) => {
  try {
    const { conferenceSid, participantCallSid, muted } = req.body;
    if (!conferenceSid || !participantCallSid || typeof muted !== 'boolean') {
      return res.status(400).json({ error: 'conferenceSid, participantCallSid, and muted (boolean) are required' });
    }

    await twilioClient.conferences(conferenceSid)
      .participants(participantCallSid)
      .update({ muted });

    logger.info({ conferenceSid, participantCallSid, muted }, 'Monitor mode updated');
    res.json({ ok: true, muted });
  } catch (err) {
    logger.error(err, 'Error updating monitor mode');
    res.status(500).json({ error: 'Failed to update monitor mode' });
  }
});

// Remove admin from conference (stop monitoring)
router.post('/monitor/stop', authMiddleware, async (req, res) => {
  try {
    const { conferenceSid, participantCallSid } = req.body;
    if (!conferenceSid || !participantCallSid) {
      return res.status(400).json({ error: 'conferenceSid and participantCallSid are required' });
    }

    await twilioClient.conferences(conferenceSid)
      .participants(participantCallSid)
      .remove();

    logger.info({ conferenceSid, participantCallSid }, 'Monitor removed from conference');
    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Error stopping monitor');
    res.status(500).json({ error: 'Failed to stop monitoring' });
  }
});

// Dashboard stats
router.get('/stats', authMiddleware, async (req, res) => {
  try {
    const days = parseInt(req.query.days, 10) || 7;
    const agentId = req.query.agentId ? parseInt(req.query.agentId, 10) : null;
    const stats = await statsService.getDashboardStats(days, agentId);
    res.json(stats);
  } catch (err) {
    logger.error(err, 'Error fetching dashboard stats');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Call volume chart data
router.get('/stats/volume', authMiddleware, async (req, res) => {
  try {
    const days = parseInt(req.query.days, 10) || 7;
    const agentId = req.query.agentId ? parseInt(req.query.agentId, 10) : null;
    const volume = await statsService.getCallVolume(days, agentId);
    res.json(volume);
  } catch (err) {
    logger.error(err, 'Error fetching call volume');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Status breakdown
router.get('/stats/status-breakdown', authMiddleware, async (req, res) => {
  try {
    const days = parseInt(req.query.days, 10) || 7;
    const agentId = req.query.agentId ? parseInt(req.query.agentId, 10) : null;
    const breakdown = await statsService.getStatusBreakdown(days, agentId);
    res.json(breakdown);
  } catch (err) {
    logger.error(err, 'Error fetching status breakdown');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Today's call count for agent
router.get('/stats/today-count', authMiddleware, async (req, res) => {
  try {
    const count = await statsService.getTodayCallCount(req.agent.id);
    res.json({ count });
  } catch (err) {
    logger.error(err, 'Error fetching today call count');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Agent leaderboard
router.get('/stats/agent-leaderboard', authMiddleware, async (req, res) => {
  try {
    const days = parseInt(req.query.days, 10) || 7;
    const leaderboard = await statsService.getAgentLeaderboard(days);
    res.json(leaderboard);
  } catch (err) {
    logger.error(err, 'Error fetching agent leaderboard');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Export call logs as CSV
router.get('/export', authMiddleware, async (req, res) => {
  try {
    const { search, direction, status, disposition, dateFrom, dateTo, agentId } = req.query;
    const rows = await callService.getCallLogsForExport({ search, direction, status, disposition, dateFrom, dateTo, agentId });

    const header = 'Direction,From,To,Agent,Email,Trademark,Status,Duration (s),Date,Notes,Disposition\n';
    const csvRows = rows.map((r) => {
      const date = r.started_at ? new Date(r.started_at).toISOString() : '';
      const meta = r.lead_metadata || {};
      const trademark = Object.entries(meta).find(([k]) => {
        const lk = k.toLowerCase();
        return lk.includes('word mark') || lk.includes('mark') || lk.includes('trademark');
      });
      return [
        r.direction,
        r.from_number,
        r.to_number,
        r.agent_name || '',
        r.lead_email || '',
        trademark ? String(trademark[1]).replace(/"/g, '""') : '',
        r.status,
        r.duration_seconds || 0,
        date,
        (r.notes || '').replace(/"/g, '""'),
        r.disposition || '',
      ].map((v) => `"${v}"`).join(',');
    });

    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', 'attachment; filename="call-logs.csv"');
    res.send(header + csvRows.join('\n'));
  } catch (err) {
    logger.error(err, 'Error exporting call logs');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Purge old call history (keep most recent 10%)
const ADMIN_USERNAME = 'admin';
const ADMIN_PASSWORD = 'admin123';

router.delete('/purge-old', authMiddleware, async (req, res) => {
  try {
    const { username, password } = req.body;
    if (username !== ADMIN_USERNAME || password !== ADMIN_PASSWORD) {
      return res.status(401).json({ error: 'Invalid admin credentials' });
    }

    const result = await callService.purgeOldCallLogs();
    logger.info(result, 'Purged old call logs');
    res.json({ ok: true, ...result });
  } catch (err) {
    logger.error(err, 'Error purging old call logs');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Delete a call log entry
router.delete('/:id', authMiddleware, async (req, res) => {
  try {
    const deleted = await callService.deleteCallLog(req.params.id);
    if (!deleted) return res.status(404).json({ error: 'Call not found' });
    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Error deleting call log');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Update call notes and disposition
router.patch('/:id/notes', authMiddleware, async (req, res) => {
  try {
    const { notes, disposition } = req.body;
    const call = await callService.updateCallNotes(req.params.id, { notes, disposition });
    if (!call) return res.status(404).json({ error: 'Call not found' });
    res.json(call);
  } catch (err) {
    logger.error(err, 'Error updating call notes');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get call logs with pagination and filters
router.get('/', authMiddleware, async (req, res) => {
  try {
    const page = parseInt(req.query.page, 10) || 1;
    const limit = parseInt(req.query.limit, 10) || 20;
    const { search, direction, status, disposition, dateFrom, dateTo, agentId } = req.query;
    const result = await callService.getCallLogs({ page, limit, search, direction, status, disposition, dateFrom, dateTo, agentId });

    // Backfill missing recordings from Twilio API
    const pool = require('../db/pool');
    for (const call of result.calls) {
      if (call.status === 'completed' && !call.recording_url && (call.conference_sid || call.call_sid)) {
        try {
          let recordings = [];
          if (call.conference_sid) {
            recordings = await twilioClient.recordings.list({ conferenceSid: call.conference_sid, limit: 1 });
          }
          if (recordings.length === 0 && call.call_sid) {
            recordings = await twilioClient.recordings.list({ callSid: call.call_sid, limit: 1 });
          }
          if (recordings.length > 0) {
            const rec = recordings[0];
            const recordingUrl = `https://api.twilio.com/2010-04-01/Accounts/${config.twilio.accountSid}/Recordings/${rec.sid}`;
            await pool.query(
              'UPDATE call_logs SET recording_sid = $1, recording_url = $2 WHERE id = $3',
              [rec.sid, recordingUrl, call.id]
            );
            call.recording_sid = rec.sid;
            call.recording_url = recordingUrl;
          }
        } catch (err) {
          logger.error(err, 'Failed to backfill recording for call ' + call.id);
        }
      }
    }

    res.json(result);
  } catch (err) {
    logger.error(err, 'Error fetching call logs');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Download call recording (proxied through server to avoid exposing Twilio credentials)
router.get('/:id/recording', async (req, res) => {
  try {
    const pool = require('../db/pool');
    const { rows } = await pool.query(
      'SELECT call_sid, conference_sid, recording_sid, recording_url FROM call_logs WHERE id = $1',
      [req.params.id]
    );

    if (!rows[0]) {
      return res.status(404).json({ error: 'Call not found' });
    }

    let { recording_sid, recording_url } = rows[0];

    // If not cached in DB, fetch from Twilio API
    if (!recording_url) {
      let recordings = [];
      if (rows[0].conference_sid) {
        recordings = await twilioClient.recordings.list({ conferenceSid: rows[0].conference_sid, limit: 1 });
      }
      if (recordings.length === 0 && rows[0].call_sid) {
        recordings = await twilioClient.recordings.list({ callSid: rows[0].call_sid, limit: 1 });
      }

      if (recordings.length === 0) {
        return res.status(404).json({ error: 'Recording not found' });
      }

      recording_sid = recordings[0].sid;
      recording_url = `https://api.twilio.com/2010-04-01/Accounts/${config.twilio.accountSid}/Recordings/${recording_sid}`;

      // Cache it in the DB for next time
      await pool.query(
        'UPDATE call_logs SET recording_sid = $1, recording_url = $2 WHERE id = $3',
        [recording_sid, recording_url, req.params.id]
      );
    }

    const response = await fetch(`${recording_url}.mp3`, {
      headers: {
        Authorization: 'Basic ' + Buffer.from(
          `${config.twilio.accountSid}:${config.twilio.authToken}`
        ).toString('base64'),
      },
    });

    if (!response.ok) {
      return res.status(502).json({ error: 'Failed to fetch recording from Twilio' });
    }

    res.set({
      'Content-Type': 'audio/mpeg',
      'Content-Disposition': `attachment; filename="recording-${recording_sid}.mp3"`,
    });

    const { Readable } = require('stream');
    Readable.fromWeb(response.body).pipe(res);
  } catch (err) {
    logger.error(err, 'Error downloading recording');
    res.status(500).json({ error: 'Failed to download recording' });
  }
});

// Toggle hold
router.post('/hold', authMiddleware, async (req, res) => {
  try {
    const { conferenceSid, participantCallSid, hold } = req.body;
    if (!conferenceSid || !participantCallSid || typeof hold !== 'boolean') {
      return res.status(400).json({ error: 'conferenceSid, participantCallSid, and hold (boolean) are required' });
    }

    // Verify the requesting agent owns this call
    const activeCall = await callService.getActiveCallByAgent(req.agent.id);
    if (!activeCall || activeCall.conference_sid !== conferenceSid) {
      return res.status(403).json({ error: 'Not authorized to control this call' });
    }

    const result = await callService.holdParticipant(conferenceSid, participantCallSid, hold);

    const io = req.app.get('io');
    if (io) {
      io.to(`agent:${req.agent.id}`).emit('call:hold', { conferenceSid, participantCallSid, hold });
    }

    res.json(result);
  } catch (err) {
    logger.error(err, 'Error toggling hold');
    res.status(500).json({ error: 'Failed to toggle hold' });
  }
});

// Initiate transfer (warm or cold)
router.post('/transfer', authMiddleware, async (req, res) => {
  try {
    const { conferenceName, targetAgentId, type } = req.body;
    if (!conferenceName || !targetAgentId || !['warm', 'cold'].includes(type)) {
      return res.status(400).json({ error: 'conferenceName, targetAgentId, and type (warm/cold) are required' });
    }

    // Verify the requesting agent owns this call
    const agentCall = await callService.getActiveCallByAgent(req.agent.id);
    if (!agentCall || agentCall.conference_name !== conferenceName) {
      return res.status(403).json({ error: 'Not authorized to control this call' });
    }

    const targetAgent = await agentService.findById(targetAgentId);
    if (!targetAgent) {
      return res.status(404).json({ error: 'Target agent not found' });
    }

    if (targetAgent.status !== 'available') {
      return res.status(409).json({ error: `Target agent is ${targetAgent.status.replace('_', ' ')}` });
    }

    const targetActiveCall = await callService.getActiveCallByAgent(targetAgentId);
    if (targetActiveCall) {
      return res.status(409).json({ error: 'Target agent is already on a call' });
    }

    if (!req.agent.twilioPhoneNumber) {
      return res.status(400).json({ error: 'No Twilio phone number assigned to your account' });
    }

    // Add target agent to conference by calling their Twilio client
    const participant = await twilioClient.conferences(conferenceName)
      .participants
      .create({
        to: `client:${targetAgent.twilio_identity}`,
        from: req.agent.twilioPhoneNumber,
        endConferenceOnExit: false,
      });

    logger.info({ conferenceName, targetAgent: targetAgent.twilio_identity, type }, 'Transfer initiated');

    // For cold transfer, remove the original agent immediately
    if (type === 'cold') {
      const activeCall = await callService.getActiveCallByConference(conferenceName);
      if (activeCall) {
        // Find and remove the original agent from conference
        const conferences = await twilioClient.conferences.list({
          friendlyName: conferenceName,
          status: 'in-progress',
        });
        if (conferences.length > 0) {
          const participants = await twilioClient.conferences(conferences[0].sid).participants.list();
          for (const p of participants) {
            // Remove the originating agent's leg (not the new agent, not the external caller)
            if (p.callSid === activeCall.call_sid) {
              await twilioClient.conferences(conferences[0].sid).participants(p.callSid).remove();
              break;
            }
          }
        }

        // Update tracking
        await agentService.updateStatus(req.agent.id, 'available');
        await callService.removeActiveCall(activeCall.call_sid);

        // Create new active call for target agent
        await callService.createActiveCall({
          callSid: participant.callSid,
          conferenceName,
          agentId: targetAgent.id,
          direction: 'transfer',
          from: activeCall.from_number,
          to: activeCall.to_number,
          status: 'in-progress',
        });

        await agentService.updateStatus(targetAgent.id, 'on_call');

        // Update call log
        await callService.updateCallLog(activeCall.call_sid, {
          transferredTo: targetAgent.id,
          transferredFrom: req.agent.id,
        });
      }

      const io = req.app.get('io');
      if (io) {
        io.emit('agent:status', { id: req.agent.id, status: 'available' });
        io.emit('agent:status', { id: targetAgent.id, status: 'on_call' });
      }
    }

    res.json({ ok: true, participantCallSid: participant.callSid, type });
  } catch (err) {
    logger.error(err, 'Error initiating transfer');
    res.status(500).json({ error: 'Failed to initiate transfer' });
  }
});

// Complete warm transfer (original agent leaves)
router.post('/transfer/complete', authMiddleware, async (req, res) => {
  try {
    const { conferenceName, targetAgentId } = req.body;
    if (!conferenceName) {
      return res.status(400).json({ error: 'conferenceName is required' });
    }

    const activeCall = await callService.getActiveCallByConference(conferenceName);
    if (!activeCall) {
      return res.status(404).json({ error: 'Active call not found' });
    }

    // Verify the requesting agent owns this call
    if (activeCall.agent_id !== req.agent.id) {
      return res.status(403).json({ error: 'Not authorized to control this call' });
    }

    // Find and remove the original agent from conference
    const conferences = await twilioClient.conferences.list({
      friendlyName: conferenceName,
      status: 'in-progress',
    });

    if (conferences.length > 0) {
      const participants = await twilioClient.conferences(conferences[0].sid).participants.list();
      for (const p of participants) {
        if (p.callSid === activeCall.call_sid) {
          await twilioClient.conferences(conferences[0].sid).participants(p.callSid).remove();
          break;
        }
      }
    }

    await agentService.updateStatus(req.agent.id, 'available');
    await callService.removeActiveCall(activeCall.call_sid);

    if (targetAgentId) {
      const targetAgent = await agentService.findById(targetAgentId);
      if (targetAgent) {
        await agentService.updateStatus(targetAgent.id, 'on_call');
        await callService.updateCallLog(activeCall.call_sid, {
          transferredTo: targetAgent.id,
          transferredFrom: req.agent.id,
        });
      }
    }

    const io = req.app.get('io');
    if (io) {
      io.emit('agent:status', { id: req.agent.id, status: 'available' });
      if (targetAgentId) {
        io.emit('agent:status', { id: parseInt(targetAgentId, 10), status: 'on_call' });
      }
    }

    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Error completing transfer');
    res.status(500).json({ error: 'Failed to complete transfer' });
  }
});

// Hangup call / end conference
router.post('/hangup', authMiddleware, async (req, res) => {
  try {
    const { conferenceName } = req.body;
    if (!conferenceName) {
      return res.status(400).json({ error: 'conferenceName is required' });
    }

    // Verify the requesting agent owns this call
    const agentCall = await callService.getActiveCallByAgent(req.agent.id);
    if (!agentCall || agentCall.conference_name !== conferenceName) {
      return res.status(403).json({ error: 'Not authorized to control this call' });
    }

    await callService.hangupConference(conferenceName);
    await callService.removeActiveCallsByConference(conferenceName);
    await agentService.updateStatus(req.agent.id, 'available');

    const io = req.app.get('io');
    if (io) {
      io.emit('agent:status', { id: req.agent.id, status: 'available' });
      io.to(`agent:${req.agent.id}`).emit('call:ended', { conferenceName });
    }

    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Error hanging up');
    res.status(500).json({ error: 'Failed to hang up' });
  }
});

module.exports = router;
