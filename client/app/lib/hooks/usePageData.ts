'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useToast } from '../../providers';


export function usePageData<T>(fetcher: () => Promise<T>, initial: T) {
  const toast = useToast();
  const toastRef = useRef(toast);
  toastRef.current = toast;

  const [data, setData] = useState<T>(initial);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await fetcher());
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Erro ao carregar dados.';
      setError(msg);
      toastRef.current.show(msg, 'error');
    } finally {
      setLoading(false);
    }
  }, [fetcher]);

  useEffect(() => { reload(); }, [reload]);

  return { data, setData, loading, error, reload };
}
