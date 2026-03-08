import { createApp } from 'vue';
import ArcoVue from '@arco-design/web-vue';
import ArcoVueIcon from '@arco-design/web-vue/es/icon';
import VueDOMPurifyHTML from 'vue-dompurify-html';
import naive from 'naive-ui';
import router from './router';
import store from './store';
import i18n from './locale';
import App from './App.vue';

import '@/assets/style/global.less';
import '@/api/interceptor';
import '@arco-design/web-vue/dist/arco.css';

const app = createApp(App);

app.use(ArcoVue, {});
app.use(ArcoVueIcon);
app.use(naive);

app.use(router);
app.use(store);
app.use(i18n);
app.use(VueDOMPurifyHTML);

app.mount('#app');
