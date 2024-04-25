import { createApp } from 'vue';
import ArcoVue from '@arco-design/web-vue';
import ArcoVueIcon from '@arco-design/web-vue/es/icon';
import VueDOMPurifyHTML from 'vue-dompurify-html';
import naive from 'naive-ui';
import router from './router';
import store from './store';
import i18n from './locale';
import App from './App.vue';
// Styles are imported via arco-plugin. See config/plugin/arcoStyleImport.ts in the directory for details
// 样式通过 arco-plugin 插件导入。详见目录文件 config/plugin/arcoStyleImport.ts
// https://arco.design/docs/designlab/use-theme-package
import '@/assets/style/global.less';
import '@/api/interceptor';

const app = createApp(App);

app.use(ArcoVue, {});
app.use(ArcoVueIcon);
app.use(naive);

app.use(router);
app.use(store);
app.use(i18n);
app.use(VueDOMPurifyHTML);

app.mount('#app');
