const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { date: '今日', slots: [
      { time: '08:00-12:00', used: 2, max: 4, pct: 50 },
      { time: '12:00-16:00', used: 4, max: 4, pct: 100 },
      { time: '16:00-20:00', used: 1, max: 4, pct: 25 }
    ] },
  onLoad(options) {

  }
});
