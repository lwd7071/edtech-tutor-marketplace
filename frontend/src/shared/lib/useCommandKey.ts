import { useRef } from 'react';

/** Keep an idempotency key for the same form payload until the server confirms success. */
export function useCommandKey<T extends object>() {
  const pending = useRef<{ payload: string; key: string } | null>(null);
  return {
    forPayload(value: T) {
      const payload = JSON.stringify(value);
      if (pending.current?.payload !== payload) pending.current = { payload, key: globalThis.crypto.randomUUID() };
      return pending.current.key;
    },
    clear(value: T) {
      if (pending.current?.payload === JSON.stringify(value)) pending.current = null;
    },
  };
}
