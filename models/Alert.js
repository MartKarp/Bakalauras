const mongoose = require('mongoose');

const alertSchema = new mongoose.Schema({
  deviceId: String,
  owner: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  message: String,
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Alert', alertSchema);
