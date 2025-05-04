const express = require('express');
const router = express.Router();

router.post('/location', (req, res) => {
  const { lat, lon } = req.body;
  console.log(`Received location: Lat=${lat}, Lon=${lon}`);
  res.status(200).json({ message: 'Location received' });
});

module.exports = router;
