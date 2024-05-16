import type { Router } from 'vue-router';
import { setRouteEmitter } from '@/utils/route-listener';
import { useAppStore } from '@/store';

function setupPageGuard(router: Router) {
  router.beforeEach(async (to) => {
    // emit route change
    setRouteEmitter(to);
    if (to.name === 'Chapter') {
      useAppStore().updateSettings({ menuCollapse: true });
    } else {
      useAppStore().updateSettings({ menuCollapse: false });
    }
  });
}

export default function createRouteGuard(router: Router) {
  setupPageGuard(router);
}
