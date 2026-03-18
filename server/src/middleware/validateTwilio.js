const twilio = require('twilio');
const config = require('../config');
const logger = require('../utils/logger');

function validateTwilio(req, res, next) {
  // Skip validation in development if desired
  if (process.env.SKIP_TWILIO_VALIDATION === 'true') {
    return next();
  }

  const signature = req.headers['x-twilio-signature'];
  const url = config.serverBaseUrl + req.originalUrl;

  const isValid = twilio.validateRequest(
    config.twilio.authToken,
    signature,
    url,
    req.body
  );

  if (!isValid) {
    logger.warn({ url }, 'Invalid Twilio signature');
    return res.status(403).send('Invalid Twilio signature');
  }

  next();
}

module.exports = validateTwilio;
