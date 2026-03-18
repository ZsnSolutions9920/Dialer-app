// Simple in-memory rate limiter — no extra dependencies
const hits = new Map();

// Clean up old entries every 5 minutes
setInterval(() => {
  const now = Date.now();
  for (const [key, entry] of hits) {
    if (now - entry.windowStart > entry.windowMs * 2) hits.delete(key);
  }
}, 5 * 60 * 1000);

function rateLimit({ windowMs = 60000, max = 10, message = 'Too many requests, please try again later.' } = {}) {
  return (req, res, next) => {
    const ip = req.ip || req.connection.remoteAddress;
    const key = `${req.path}:${ip}`;
    const now = Date.now();

    let entry = hits.get(key);
    if (!entry || now - entry.windowStart > windowMs) {
      entry = { count: 0, windowStart: now, windowMs };
      hits.set(key, entry);
    }

    entry.count++;

    if (entry.count > max) {
      const retryAfter = Math.ceil((entry.windowStart + windowMs - now) / 1000);
      res.set('Retry-After', String(retryAfter));
      return res.status(429).json({ error: message });
    }

    next();
  };
}

module.exports = rateLimit;
