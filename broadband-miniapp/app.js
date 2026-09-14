// app.js —— 客户端小程序（主题色 蓝 #2563eb）
App({
  globalData: {
    baseUrl: 'http://localhost:8082',
    primaryColor: '#2563eb',
    token: '',
    role: null,          // 'client' | 'worker'
    customer: null
  },
  onLaunch() {
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.globalData.role = wx.getStorageSync('role');
      this.globalData.customer = wx.getStorageSync('customer') || null;
    }
  },
  setClientLogin(token, customer) {
    this.globalData.token = token; this.globalData.role = 'client'; this.globalData.customer = customer;
    wx.setStorageSync('token', token); wx.setStorageSync('role', 'client'); wx.setStorageSync('customer', customer);
  },
  logout() {
    this.globalData.token = ''; this.globalData.role = null; this.globalData.customer = null;
    wx.removeStorageSync('token'); wx.removeStorageSync('role'); wx.removeStorageSync('customer');
  }
});
