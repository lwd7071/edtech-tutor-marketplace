'use client';

import { useCallback, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { useAuthStore } from './sessionStore';

const LOGOUT_TIMEOUT_MS = 2_000;

export function useLogout() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const running = useRef(false);
  const refreshToken = useAuthStore((state) => state.refreshToken);
  const clear = useAuthStore((state) => state.clear);

  return useCallback(async () => {
    if (running.current) return;
    running.current = true;
    try {
      if (refreshToken) {
        await new Promise<void>((resolve) => {
          const timeout = setTimeout(resolve, LOGOUT_TIMEOUT_MS);
          void authApi.logout(refreshToken).catch(() => undefined).finally(() => {
            clearTimeout(timeout);
            resolve();
          });
        });
      }
    } finally {
      clear();
      queryClient.clear();
      router.replace('/auth/login');
      running.current = false;
    }
  }, [clear, queryClient, refreshToken, router]);
}
