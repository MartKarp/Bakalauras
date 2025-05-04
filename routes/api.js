const express = require('express');
const router = express.Router();

let latestCoordinates = { lat: null, lon: null };

router.post('/location', (req, res) => {
  console.log('Raw body:', req.body); // Debug log

  // Try to parse the values
  const lat = parseFloat(req.body.lat);
  const lon = parseFloat(req.body.lon);

  if (!isNaN(lat) && !isNaN(lon)) {
    latestCoordinates = { lat, lon };
    console.log(`Received location: lat=${lat}, lon=${lon}`);
    res.status(200).json({ message: 'Location received' });
  } else {
    res.status(400).json({ error: 'Invalid or missing lat/lon' });
  }
});

// Optional: view the last received coordinates
router.get('/location', (req, res) => {
  res.json(latestCoordinates);
});

module.exports = router;
