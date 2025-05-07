const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  email: { type: String, unique: true, required: true },
  passwordHash: { type: String, required: true },
  devices: [{ type: String }]
});

module.exports = mongoose.model('User', userSchema);
