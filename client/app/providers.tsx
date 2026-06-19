'use client';

import React, { createContext, useContext, useEffect, useState, useCallback, useMemo } from 'react';
import { API } from './lib/api-client';
import type { AppUser } from './lib/client-config';


interface AuthCtx {
  user: AppUser | null;
  loading: boolean;
  refresh: () => Promise<void>;
  setUser: (u: AppUser | null) => void;
}

const AuthContext = createContext<AuthCtx>({ user: null, loading: true, refresh: async () => {}, setUser: () => {} });

export function useAuth() {
  return useContext(AuthContext);
}


type Theme = 'dark' | 'light';
interface ThemeCtx {
  theme: Theme;
  toggle: () => void;
}
const ThemeContext = createContext<ThemeCtx>({ theme: 'dark', toggle: () => {} });
export function useTheme() {
  return useContext(ThemeContext);
}


interface ToastMsg { id: number; message: string; type: 'success' | 'error' }
interface ToastCtx {
  show: (msg: string, type?: 'success' | 'error') => void;
}
const ToastContext = createContext<ToastCtx>({ show: () => {} });
export function useToast() {
  return useContext(ToastContext);
}


interface SetoresCtx {
  setores: string[];
  refresh: () => Promise<void>;
}
const SetoresContext = createContext<SetoresCtx>({ setores: [], refresh: async () => {} });
export function useSetores() {
  return useContext(SetoresContext);
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AppUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [theme, setTheme] = useState<Theme>('dark');
  const [toasts, setToasts] = useState<ToastMsg[]>([]);
  const [setores, setSetores] = useState<string[]>([]);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const res = await API.me();
      setUser(res.user as AppUser);
    } catch {
      setUser(null);
    }
    setLoading(false);
  }, []);

  const refreshSetores = useCallback(async () => {
    try {
      const list = await API.getSetores();
      setSetores(list);
    } catch {}
  }, []);

  useEffect(() => {
    refresh();
    refreshSetores();
  }, [refresh, refreshSetores]);

  useEffect(() => {
    const saved = (typeof window !== 'undefined' ? localStorage.getItem('theme') : null) as Theme | null;
    const initial: Theme = saved === 'light' ? 'light' : 'dark';
    setTheme(initial);
    document.documentElement.setAttribute('data-theme', initial);
    document.documentElement.classList.toggle('dark', initial === 'dark');
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme((prev) => {
      const next = prev === 'light' ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', next);
      document.documentElement.classList.toggle('dark', next === 'dark');
      localStorage.setItem('theme', next);
      return next;
    });
  }, []);

  const show = useCallback((message: string, type: 'success' | 'error' = 'success') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  }, []);

  const authValue = useMemo(() => ({ user, loading, refresh, setUser }), [user, loading, refresh]);
  const themeValue = useMemo(() => ({ theme, toggle: toggleTheme }), [theme, toggleTheme]);
  const toastValue = useMemo(() => ({ show }), [show]);
  const setoresValue = useMemo(() => ({ setores, refresh: refreshSetores }), [setores, refreshSetores]);

  return (
    <AuthContext.Provider value={authValue}>
      <ThemeContext.Provider value={themeValue}>
        <ToastContext.Provider value={toastValue}>
          <SetoresContext.Provider value={setoresValue}>
            {children}
            <div className="fixed bottom-6 right-6 z-[9999] flex flex-col gap-2 pointer-events-none">
              {toasts.map((t) => (
                <div
                  key={t.id}
                  className={`px-5 py-3 rounded-lg shadow-2xl backdrop-blur-md font-medium text-sm animate-slide-up ${
                    t.type === 'success' ? 'bg-emerald-500/90 text-white' : 'bg-red-500/90 text-white'
                  }`}
                >
                  {t.message}
                </div>
              ))}
            </div>
          </SetoresContext.Provider>
        </ToastContext.Provider>
      </ThemeContext.Provider>
    </AuthContext.Provider>
  );
}
