const mongoose = require('mongoose');

const lockSchema = new mongoose.Schema({
  _id: { type: String, default: 'current' },
  state: { type: String, enum: ['locked', 'unlocked'], default: 'unlocked' },
  updatedAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Lock', lockSchema);
