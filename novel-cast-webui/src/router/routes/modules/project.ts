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
      path: 'model', // The midline path complies with SEO specifications
      name: 'Model',
      component: () => import('@/views/model/index.vue'),
      meta: {
        locale: 'menu.novelCast.model',
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
