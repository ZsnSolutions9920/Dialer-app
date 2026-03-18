const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const config = require('../config');
const { setupAgentPresence } = require('./agentPresence');
const logger = require('../utils/logger');

function setupSocket(httpServer) {
  const io = new Server(httpServer, {
    cors: {
      origin: '*',
      methods: ['GET', 'POST'],
    },
  });

  // Auth middleware for socket connections
  io.use((socket, next) => {
    const token = socket.handshake.auth?.token;
    if (!token) {
      return next(new Error('Authentication required'));
    }

    try {
      const payload = jwt.verify(token, config.jwtSecret);
      socket.agent = payload;
      next();
    } catch (err) {
      logger.warn('Socket auth failed: invalid token');
      next(new Error('Invalid token'));
    }
  });

  setupAgentPresence(io);

  logger.info('Socket.IO initialized');
  return io;
}

module.exports = { setupSocket };
