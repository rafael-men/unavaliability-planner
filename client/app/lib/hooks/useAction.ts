'use client';

import { useCallback, useState } from 'react';


export function useAction() {
  const [busyKey, setBusyKey] = useState<string | number | null>(null);

  const run = useCallback(async (key: string | number, fn: () => Promise<unknown>) => {
    if (busyKey !== null) return; 
    setBusyKey(key);
    try {
      await fn();
    } finally {
      setBusyKey(null);
    }
  }, [busyKey]);

  return { run, busyKey, isBusy: busyKey !== null };
}
