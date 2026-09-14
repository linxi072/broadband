// utils/auth.js —— 登录态校验
function app() { return getApp(); }
function isLogin() { return !!app().globalData.token; }
function currentRole() { return app().globalData.role; }
function requireLogin(redirectUrl) { if (isLogin()) return true; wx.redirectTo({ url: redirectUrl }); return false; }
module.exports = { isLogin: isLogin, currentRole: currentRole, requireLogin: requireLogin };
