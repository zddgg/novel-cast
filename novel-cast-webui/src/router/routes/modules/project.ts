import { DEFAULT_LAYOUT } from '../base';
import { AppRouteRecordRaw } from '../types';

const LIST: AppRouteRecordRaw = {
  path: '/',
  name: 'NovelCast',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.novelCast',
    requiresAuth: true,
    icon: 'icon-list',
    order: -1,
  },
  children: [
    {
      path: 'project', // The midline path complies with SEO specifications
      name: 'Project',
      component: () => import('@/views/project/index.vue'),
      meta: {
        locale: 'menu.novelCast.project',
        requiresAuth: true,
        roles: ['*'],
      },
    },
    {
      path: 'projectCreate', // The midline path complies with SEO specifications
      name: 'ProjectCreate',
      component: () => import('@/views/project/components/ProjectCreate.vue'),
      meta: {
        requiresAuth: true,
        roles: ['*'],
        hideInMenu: true,
      },
    },
    {
      path: 'projectConfig', // The midline path complies with SEO specifications
      name: 'ProjectConfig',
      component: () => import('@/views/project/components/ProjectConfig.vue'),
      meta: {
        requiresAuth: true,
        roles: ['*'],
        hideInMenu: true,
      },
    },
    {
      path: 'gsvModel', // The midline path complies with SEO specifications
      name: 'gsvModel',
      component: () => import('@/views/gsvModel/index.vue'),
      meta: {
        locale: 'menu.novelCast.gsvModel',
        requiresAuth: true,
        roles: ['*'],
      },
    },
    {
      path: 'gsvAudio', // The midline path complies with SEO specifications
      name: 'gsvAudio',
      component: () => import('@/views/gsvAudio/index.vue'),
      meta: {
        locale: 'menu.novelCast.gsvAudio',
        requiresAuth: true,
        roles: ['*'],
      },
    },
    {
      path: 'chapter', // The midline path complies with SEO specifications
      name: 'Chapter',
      component: () => import('@/views/chapter/index.vue'),
      meta: {
        requiresAuth: true,
        roles: ['*'],
        hideInMenu: true,
      },
    },
  ],
};

export default LIST;
