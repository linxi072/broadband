// app.js
App({
  globalData: {
    // 后端基址（Spring Boot 端口 8082，与派单/SLA 模块一致）
    baseUrl: 'http://localhost:8082',
    // 客户端主题色（设计稿 v1.8 规范：蓝 #2563eb）
    primaryColor: '#2563eb'
  },
  onLaunch() {}
});
