const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const http = require('http');
const path = require('path');
const config = require('./config');
const logger = require('./utils/logger');
const { setupSocket } = require('./socket');

// Routes
const authMiddleware = require('./middleware/auth');
const authRoutes = require('./routes/auth');
const agentRoutes = require('./routes/agents');
const tokenRoutes = require('./routes/token');
const callRoutes = require('./routes/calls');
const contactRoutes = require('./routes/contacts');
const phoneListRoutes = require('./routes/phoneLists');
const twilioRoutes = require('./routes/twilio');
const attendanceRoutes = require('./routes/attendance');
const emailRoutes = require('./routes/email');
const emailTrackingRoutes = require('./routes/emailTracking');
const customerAuthRoutes = require('./routes/customerAuth');
const adminRoutes = require('./routes/admin');
const storeRoutes = require('./routes/store');
const purchaseRoutes = require('./routes/purchase');

const app = express();
const server = http.createServer(app);

// Middleware — mobile backend allows all origins
app.use(cors());
app.use(helmet({ contentSecurityPolicy: false }));
app.use(express.urlencoded({ extended: false, limit: '50mb' }));
app.use(express.json({ limit: '50mb' }));

// Serve static files (APK downloads + admin panel)
app.use(express.static(path.join(__dirname, '../public')));

// Socket.IO
const io = setupSocket(server);
app.set('io', io);

// Public routes (no auth — tracking pixels and click redirects)
app.use('/api/email', emailTrackingRoutes);

// Customer auth (signup/login for mobile app users)
app.use('/api/customer', customerAuthRoutes);

// Admin panel API
app.use('/api/admin', adminRoutes);

// Store (browse numbers, minutes packages)
app.use('/api/store', storeRoutes);

// Purchases (checkout, mock, history)
app.use('/api/purchase', purchaseRoutes);

// API Routes (existing agent-based routes)
app.use('/api/auth', authRoutes);
app.use('/api/agents', agentRoutes);
app.use('/api/token', tokenRoutes);
app.use('/api/calls', callRoutes);
app.use('/api/contacts', authMiddleware, contactRoutes);
app.use('/api/phone-lists', authMiddleware, phoneListRoutes);
app.use('/api/twilio', twilioRoutes);
app.use('/api/attendance', attendanceRoutes);
app.use('/api/email', authMiddleware, emailRoutes);

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

server.listen(config.port, () => {
  logger.info(`Mobile backend listening on port ${config.port}`);
});
