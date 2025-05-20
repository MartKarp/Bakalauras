const express = require('express');
const router = express.Router();
const Location = require('../models/Location'); 
const Lock = require('../models/Lock'); 
const Device = require('../models/Device');
const User = require('../models/User');
const Alert = require('../models/Alert');

const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const moment = require('moment-timezone');


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

router.post('/register', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password)
    return res.status(400).json({ error: 'Email and password required' });

  try {
    const existing = await User.findOne({ email });
    if (existing)
      return res.status(400).json({ error: 'User already exists' });

    const passwordHash = await bcrypt.hash(password, 10);
    const newUser = await User.create({ email, passwordHash, devices: [] });

    res.status(201).json({ message: 'Registered successfully' });
  } catch (err) {
    res.status(500).json({ error: 'Registration failed', details: err.message });
  }
});

router.get('/test', (req, res) => {
  res.json({ message: 'API test OK!' });
});


router.post('/login', async (req, res) => {
  const { email, password } = req.body;

  try {
    const user = await User.findOne({ email });
    if (!user)
      return res.status(401).json({ error: 'Invalid credentials' });

    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid)
      return res.status(401).json({ error: 'Invalid credentials' });

    const token = jwt.sign({ id: user._id, email: user.email }, jwtSecret, { expiresIn: '7d' });
    res.json({ token });
  } catch (err) {
    res.status(500).json({ error: 'Login failed', details: err.message });
  }
});

router.get('/me', authenticateToken, async (req, res) => {
  try {
    const user = await User.findById(req.user.id).populate('devices');
    if (!user) return res.status(404).json({ error: 'User not found' });

    res.json({
      email: user.email,
      devices: user.devices,
    });
  } catch (err) {
    res.status(500).json({ error: 'Database error', details: err.message });
  }
});

router.post('/unclaim', authenticateToken, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user || user.devices.length === 0) {
      return res.status(400).json({ error: 'No device to unclaim' });
    }

    const deviceId = user.devices[0]; // assume 1 device for now
    const device = await Device.findOne({ deviceId });

    if (!device) {
      return res.status(404).json({ error: 'Device not found' });
    }

    device.claimed = false;
    device.owner = null;
    await device.save();

    user.devices = [];
    await user.save();

    res.json({ success: true, message: 'Device unclaimed' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Unclaim failed', details: err.message });
  }
});

router.post('/device_register', async (req, res) => {
  const { deviceId, claimCode, deviceSecret } = req.body;

  if (!deviceId || !claimCode || !deviceSecret) {
    return res.status(400).json({ error: "Missing required fields" });
  }

  // Optional: Check if device already exists
  const existing = await Device.findOne({ deviceId });
  if (existing) {
    return res.status(409).json({ error: "Device already exists" });
  }

  try {
    const newDevice = new Device({
      deviceId,
      claimCode,
      deviceSecret,
      claimed: false,
      owner: null
    });

    await newDevice.save();
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: "Database error", details: err.message });
  }
});

// POST /api/motion-alert
router.post('/motion-alert', async (req, res) => {
  const { deviceId, message } = req.body;

  if (!deviceId || !message) {
    return res.status(400).json({ error: "Missing deviceId or message" });
  }

  try {
// POST /api/motion-alert
router.post('/motion-alert', async (req, res) => {
  const { deviceId, message } = req.body;

  if (!deviceId || !message) {
    return res.status(400).json({ error: "Missing deviceId or message" });
  }

  try {
const device = await Device.findOne({ deviceId });
if (device) req.user.deviceId = device.deviceId;

    if (!device || !device.claimed || !device.owner) {
      return res.status(404).json({ error: "Device not found or unclaimed" });
    }

    console.log(`Alert from device ${deviceId}: ${message}`);

    await Alert.create({
      deviceId,
      owner: device.owner,
      message,
      timestamp: new Date()
    });

    res.status(200).json({ success: true, received: true });
  } catch (err) {
    console.error("Error handling motion alert:", err);
    res.status(500).json({ error: "Server error", details: err.message });
  }
});if (device) req.user.deviceId = device.deviceId;

    if (!device || !device.claimed || !device.owner) {
      return res.status(404).json({ error: "Device not found or unclaimed" });
    }

    console.log(`Alert from device ${deviceId}: ${message}`);

    await Alert.create({
      deviceId,
      owner: device.owner,
      message,
      timestamp: new Date()
    });

    res.status(200).json({ success: true, received: true });
  } catch (err) {
    console.error("Error handling motion alert:", err);
    res.status(500).json({ error: "Server error", details: err.message });
  }
});

router.get('/alerts', authenticateToken, async (req, res) => {
  const userId = req.user.id;

  const device = await Device.findOne({ owner: userId });
  if (!device) return res.json([]);

  const alerts = await Alert.find({ deviceId: device.deviceId })
    .sort({ timestamp: -1 })
    .limit(10);

  const response = alerts.map(alert => ({
    _id: alert._id,
    message: alert.message,
    timestamp: moment(alert.timestamp).tz('Europe/Vilnius').format('YYYY-MM-DD HH:mm:ss')
  }));

  res.json(response);
});


router.delete('/alerts/:id', authenticateToken, async (req, res) => {
  try {
const result = await Alert.findOneAndDelete({ _id: req.params.id, owner: req.user.id });
    if (!result) return res.status(404).json({ error: 'Alert not found' });
    res.json({ message: 'Alert deleted' });
  } catch (err) {
    res.status(500).json({ error: 'Server error' });
  }
});







module.exports = router;
