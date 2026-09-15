import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { setupPermissionDirective } from './directives/permission'
import EmptyState from './components/EmptyState.vue'
import StatusTag from './components/StatusTag.vue'
import './styles/index.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
setupPermissionDirective(app)
// 全局设计系统组件：空态 / 状态标签（三端统一语义）
app.component('EmptyState', EmptyState)
app.component('StatusTag', StatusTag)
app.mount('#app')
