// app.js —— 安装师傅端小程序（主题色 橙 #ea580c）
App({
  globalData: {
    baseUrl: 'http://localhost:8082',
    primaryColor: '#ea580c',
    token: '',
    role: null,          // 'client' | 'worker'
    worker: null
  },
  onLaunch() {
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.globalData.role = wx.getStorageSync('role');
      this.globalData.worker = wx.getStorageSync('worker') || null;
    }
  },
  setWorkerLogin(token, worker) {
    this.globalData.token = token; this.globalData.role = 'worker'; this.globalData.worker = worker;
    wx.setStorageSync('token', token); wx.setStorageSync('role', 'worker'); wx.setStorageSync('worker', worker);
  },
  logout() {
    this.globalData.token = ''; this.globalData.role = null; this.globalData.worker = null;
    wx.removeStorageSync('token'); wx.removeStorageSync('role'); wx.removeStorageSync('worker');
  }
});
