const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { shifts: [
      { day: '周一', am: '08:00-12:00', pm: '13:00-18:00', rest: false },
      { day: '周二', am: '休息', pm: '休息', rest: true },
      { day: '周三', am: '08:00-12:00', pm: '13:00-18:00', rest: false },
      { day: '周四', am: '08:00-12:00', pm: '13:00-18:00', rest: false },
      { day: '周五', am: '08:00-12:00', pm: '13:00-18:00', rest: false },
      { day: '周六', am: '09:00-12:00', pm: '休息', rest: false },
      { day: '周日', am: '休息', pm: '休息', rest: true }
    ] },
  onLoad(options) {

  }
});
