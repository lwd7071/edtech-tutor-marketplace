'use client';

import { useEffect } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from './sessionStore';
import { readSessionChange } from './sessionPersistence';
import { roleHome } from '@/shared/lib/navigation';

function workspaceRole(pathname: string) {
  if (pathname === '/student' || pathname.startsWith('/student/')) return 'STUDENT';
  if (pathname === '/teacher' || pathname.startsWith('/teacher/')) return 'TEACHER';
  if (pathname === '/admin' || pathname.startsWith('/admin/')) return 'ADMIN';
  return null;
}

export function SessionHydrator({ children }: { children: React.ReactNode }) {
  const hydrate = useAuthStore((state) => state.hydrate);
  const router = useRouter();
  const pathname = usePathname();
  const queryClient = useQueryClient();

  useEffect(() => {
    hydrate();
    let lastHandledRevision = useAuthStore.getState().revision;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const reconcile = () => {
      const before = useAuthStore.getState();
      const change = readSessionChange();
      if (!change || change.revision === lastHandledRevision) return;
      hydrate();
      const after = useAuthStore.getState();
      lastHandledRevision = after.revision ?? change.revision;
      const identityChanged = before.sessionId !== after.sessionId || before.user?.id !== after.user?.id || before.isAuthenticated !== after.isAuthenticated;
      if (!identityChanged) return;

      queryClient.clear();
      if (!after.isAuthenticated || !after.user) {
        router.replace('/auth/login');
        return;
      }
      const currentRole = workspaceRole(pathname);
      if (currentRole && currentRole !== after.user.role) router.replace(roleHome(after.user.role));
    };

    const scheduleReconcile = () => {
      if (timer) return;
      timer = setTimeout(() => {
        timer = null;
        reconcile();
      }, 0);
    };
    const onStorage = (event: StorageEvent) => {
      if (event.key === 'tutor-match.session.revision') scheduleReconcile();
    };
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') scheduleReconcile();
    };
    window.addEventListener('storage', onStorage);
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      if (timer) clearTimeout(timer);
      window.removeEventListener('storage', onStorage);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [hydrate, pathname, queryClient, router]);

  return children;
}
