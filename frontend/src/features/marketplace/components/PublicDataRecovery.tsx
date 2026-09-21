'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

const INITIAL_RETRY_DELAY_MS = 5_000;
const RETRY_INTERVAL_MS = 15_000;

export default function PublicDataRecovery({ hasError }: { hasError: boolean }) {
  const router = useRouter();

  useEffect(() => {
    if (!hasError) return;

    const initialRetry = window.setTimeout(() => router.refresh(), INITIAL_RETRY_DELAY_MS);
    const retryInterval = window.setInterval(() => router.refresh(), RETRY_INTERVAL_MS);

    return () => {
      window.clearTimeout(initialRetry);
      window.clearInterval(retryInterval);
    };
  }, [hasError, router]);

  return null;
}
