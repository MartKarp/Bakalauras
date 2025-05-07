const mongoose = require('mongoose');

const deviceSchema = new mongoose.Schema({
  deviceId: { type: String, unique: true },
  claimCode: { type: String, unique: true },
  claimed: { type: Boolean, default: false },
  secret: { type: String },
  owner: { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null },
});

module.exports = mongoose.model('Device', deviceSchema);
