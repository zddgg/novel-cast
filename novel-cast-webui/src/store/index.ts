import { createPinia } from 'pinia';
import useAppStore from './modules/app';
import useTabBarStore from './modules/tab-bar';

const pinia = createPinia();

export { useAppStore, useTabBarStore };
export default pinia;
