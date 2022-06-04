import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import router from './router';
import store from './store';

const app = createApp(App);
app.use(store).use(router).use(ElementPlus)
  .mount('#app');
// eslint-disable-next-line
for (const iconName in ElementPlusIconsVue) {
  if (Reflect.has(ElementPlusIconsVue, iconName)) {
    app.component(iconName, ElementPlusIconsVue[iconName]);
  }
}
