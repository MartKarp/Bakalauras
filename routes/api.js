const express = require('express');
const router = express.Router();
const Location = require('../models/Location'); // adjust path if needed
const Lock = require('../models/Lock'); // adjust path as needed
const Device = require('../models/Device');
const User = require('../models/User');
const jwt = require('jsonwebtoken');

require('dotenv').config();
const jwtSecret = process.env.JWT_SECRET;


function authenticateToken(req, res, next) {
  const token = req.headers['authorization'];
  if (!token) return res.sendStatus(401);

  jwt.verify(token, jwtSecret, (err, user) => {
    if (err) return res.sendStatus(403);
    req.user = user;
    next();
  });
}



router.post('/location', async (req, res) => {
  const { lat, lon } = req.body;

  if (typeof lat === 'number' && typeof lon === 'number') {
    try {
      const newLocation = new Location({ lat, lon });
      await newLocation.save();
      res.status(200).json({ message: 'Location saved', data: newLocation });
    } catch (err) {
      console.error(err);
      res.status(500).json({ error: 'Database error', details: err.message });
    }
  } else {
    res.status(400).json({ error: 'Invalid coordinates' });
  }
});

router.get('/location', async (req, res) => {
  try {
    const latest = await Location.findOne().sort({ timestamp: -1 });
    if (latest) {
      res.json(latest);
    } else {
      res.status(404).json({ error: 'No coordinates found' });
    }
  } catch (err) {
    res.status(500).json({ error: 'Database error', details: err.message });
  }
});

// GET current lock state
router.get('/lock', async (req, res) => {
  try {
    let doc = await Lock.findById('current');
    if (!doc) {
      doc = await Lock.create({ _id: 'current', state: 'unlocked' });
    }
    res.json({ status: doc.state });
  } catch (err) {
    res.status(500).json({ error: 'Database error', details: err.message });
  }
});

// POST to lock
router.post('/lock', async (req, res) => {
  await Lock.findByIdAndUpdate('current', { state: 'locked', updatedAt: new Date() }, { upsert: true });
  res.json({ status: 'locked' });
});

// POST to unlock
router.post('/unlock', async (req, res) => {
  await Lock.findByIdAndUpdate('current', { state: 'unlocked', updatedAt: new Date() }, { upsert: true });
  res.json({ status: 'unlocked' });
});

// Claim a device
router.post('/claim', authenticateToken, async (req, res) => {
  const { claimCode } = req.body;

  const device = await Device.findOne({ claimCode });
  if (!device) return res.status(404).json({ error: 'Invalid claim code' });
  if (device.claimed) return res.status(400).json({ error: 'Device already claimed' });

  device.claimed = true;
  device.owner = req.user.id;
  await device.save();

  await User.findByIdAndUpdate(req.user.id, { $push: { devices: device.deviceId } });

  res.json({ success: true, deviceId: device.deviceId });
});

module.exports = router;
