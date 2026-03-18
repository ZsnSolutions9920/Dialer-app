const jwt = require('jsonwebtoken');
const config = require('../config');
const agentService = require('../services/agentService');
const logger = require('../utils/logger');

// Map of socket.id -> agentId
const connectedAgents = new Map();

function setupAgentPresence(io) {
  io.on('connection', async (socket) => {
    const agentId = socket.agent?.id;
    if (!agentId) {
      socket.disconnect();
      return;
    }

    connectedAgents.set(socket.id, agentId);
    socket.join(`agent:${agentId}`);
    logger.info({ agentId, socketId: socket.id }, 'Agent connected via WebSocket');

    // Auto-set agent to available when they connect (if offline)
    try {
      const currentAgent = await agentService.findById(agentId);
      if (currentAgent && currentAgent.status === 'offline') {
        await agentService.updateStatus(agentId, 'available');
        io.emit('agent:status', { id: agentId, status: 'available' });
      }
    } catch (err) {
      logger.error(err, 'Error auto-setting agent available on connect');
    }

    // Broadcast agent list
    const agents = await agentService.listAll();
    io.emit('agents:list', agents);

    // Handle status change
    socket.on('agent:setStatus', async (status) => {
      try {
        const updated = await agentService.updateStatus(agentId, status);
        if (updated) {
          io.emit('agent:status', updated);
        }
      } catch (err) {
        logger.error(err, 'Error updating agent status via socket');
      }
    });

    // Handle disconnect
    socket.on('disconnect', async () => {
      connectedAgents.delete(socket.id);
      logger.info({ agentId, socketId: socket.id }, 'Agent disconnected from WebSocket');

      // Check if agent has no other connections
      const hasOtherSockets = [...connectedAgents.values()].includes(agentId);
      if (!hasOtherSockets) {
        await agentService.updateStatus(agentId, 'offline');
        io.emit('agent:status', { id: agentId, status: 'offline' });
      }
    });
  });
}

module.exports = { setupAgentPresence, connectedAgents };
