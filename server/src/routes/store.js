const express = require('express');
const twilioClient = require('../services/twilioClient');
const logger = require('../utils/logger');

const router = express.Router();

// Minutes packages — tiered pricing (more minutes = cheaper per minute)
const MINUTES_PACKAGES = [
  { id: 'min_50',   minutes: 50,   price: 2.00,  perMinute: 0.040, savings: 0  },
  { id: 'min_150',  minutes: 150,  price: 5.00,  perMinute: 0.033, savings: 17 },
  { id: 'min_500',  minutes: 500,  price: 12.00, perMinute: 0.024, savings: 40 },
  { id: 'min_1500', minutes: 1500, price: 30.00, perMinute: 0.020, savings: 50 },
  { id: 'min_5000', minutes: 5000, price: 80.00, perMinute: 0.016, savings: 60 },
];

const MARKUP = 1.00; // $1 profit per number

// Browse available Twilio phone numbers
router.get('/numbers', async (req, res) => {
  try {
    const { country = 'US', type = 'local', limit = 10 } = req.query;
    const countryCode = country.toUpperCase();
    const fetchLimit = Math.min(parseInt(limit) || 10, 20);

    let numbers = [];

    if (type === 'tollfree') {
      const available = await twilioClient
        .availablePhoneNumbers(countryCode)
        .tollFree.list({ limit: fetchLimit });

      numbers = available.map(n => ({
        phoneNumber: n.phoneNumber,
        friendlyName: n.friendlyName,
        region: n.region || null,
        capabilities: {
          voice: n.capabilities.voice,
          sms: n.capabilities.SMS,
          mms: n.capabilities.MMS,
        },
        type: 'tollfree',
        monthlyPrice: 2.15 + MARKUP, // Twilio toll-free ~$2.15/mo + markup
      }));
    } else {
      // Default: local numbers
      const available = await twilioClient
        .availablePhoneNumbers(countryCode)
        .local.list({ limit: fetchLimit });

      numbers = available.map(n => ({
        phoneNumber: n.phoneNumber,
        friendlyName: n.friendlyName,
        locality: n.locality || null,
        region: n.region || null,
        capabilities: {
          voice: n.capabilities.voice,
          sms: n.capabilities.SMS,
          mms: n.capabilities.MMS,
        },
        type: 'local',
        monthlyPrice: 1.15 + MARKUP, // Twilio local ~$1.15/mo + markup
      }));
    }

    res.json({ numbers, type, country: countryCode });
  } catch (err) {
    logger.error(err, 'Failed to fetch available numbers');
    res.status(500).json({ error: 'Failed to fetch available numbers' });
  }
});

// Get minutes packages
router.get('/minutes-packages', (req, res) => {
  res.json({ packages: MINUTES_PACKAGES });
});

module.exports = router;
