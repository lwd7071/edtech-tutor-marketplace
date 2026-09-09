'use client';

import { useEffect } from 'react';
import { useAuthStore } from './sessionStore';

export function SessionHydrator({ children }: { children: React.ReactNode }) {
  const hydrate = useAuthStore((state) => state.hydrate);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  return children;
}
