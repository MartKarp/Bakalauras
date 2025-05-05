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

router.post('/lock', (req, res) => {
  lockState = 'locked';
  console.log("Bike locked.");
  res.json({ status: 'locked' });
});

router.post('/unlock', (req, res) => {
  lockState = 'unlocked';
  console.log("Bike unlocked.");
  res.json({ status: 'unlocked' });
});

router.get('/lock', (req, res) => {
  res.json({ status: lockState });
});

module.exports = router;
