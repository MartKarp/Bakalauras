const express = require('express');
const router = express.Router();
const Location = require('../models/Location'); // adjust path if needed

let lockState = 'unlocked';

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
    let doc = await LockState.findById('current');
    if (!doc) {
      doc = await LockState.create({ _id: 'current', state: 'unlocked' });
    }
    res.json({ status: doc.state });
  } catch (err) {
    res.status(500).json({ error: 'Database error', details: err.message });
  }
});

// POST to lock
router.post('/lock', async (req, res) => {
  await LockState.findByIdAndUpdate('current', { state: 'locked', updatedAt: new Date() }, { upsert: true });
  res.json({ status: 'locked' });
});

// POST to unlock
router.post('/unlock', async (req, res) => {
  await LockState.findByIdAndUpdate('current', { state: 'unlocked', updatedAt: new Date() }, { upsert: true });
  res.json({ status: 'unlocked' });
});

module.exports = router;
