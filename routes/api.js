const express = require('express');
const router = express.Router();

let latestCoordinates = { lat: null, lon: null };

// Handle POST to store coordinates
router.post('/location', (req, res) => {
  const { lat, lon } = req.body;

  if (typeof lat === 'number' && typeof lon === 'number') {
    latestCoordinates = { lat, lon };
    console.log(`Received location: Lat=${lat}, Lon=${lon}`);
    res.status(200).json({ message: 'Location received' });
  } else {
    res.status(400).json({ error: 'Invalid coordinates' });
  }
});

// Handle GET to display the last known coordinates
router.get('/location', (req, res) => {
  res.json(latestCoordinates);
});

module.exports = router;
