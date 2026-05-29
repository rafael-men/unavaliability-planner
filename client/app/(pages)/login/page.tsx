'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Mail, KeyRound, Eye, EyeOff, Loader2, CalendarCheck } from 'lucide-react';
import { API } from '../../lib/api-client';
import { useAuth, useToast } from '../../providers';

export default function LoginPage() {
  const router = useRouter();
  const { user, refresh } = useAuth();
  const toast = useToast();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (user) router.replace('/unavailability');
  }, [user, router]);

  async function doLogin(e?: React.FormEvent) {
    e?.preventDefault();
    setError(null);
    if (!email || !password) {
      setError('Preencha email e senha.');
      return;
    }
    setLoading(true);
    try {
      await API.login(email.trim(), password);
      await refresh();
      toast.show('Bem-vindo!');
      router.push('/unavailability');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-5">
      <div className="w-full max-w-md bg-[var(--card)] border border-[var(--border)] rounded-2xl p-8 shadow-2xl animate-fade-up">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center mb-4 w-16 h-16 rounded-2xl bg-[var(--accent)]/10 border border-[var(--accent)]/30">
            <CalendarCheck size={32} className="text-[var(--accent)]" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-br from-foreground to-[var(--accent)] bg-clip-text text-transparent">
            Indisponibilidade
          </h1>
          <p className="text-[var(--text-muted)] text-sm mt-2">Faça login para acessar o sistema</p>
        </div>

        <form onSubmit={doLogin} className="space-y-4">
          {error && (
            <div className="px-4 py-2.5 bg-red-500/10 text-red-400 border border-red-500/20 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 flex items-center gap-1.5">
              <Mail size={12} /> Email
            </label>
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              placeholder="seu@.com.br"
              disabled={loading}
              className="w-full px-4 py-2.5 rounded-lg border border-[var(--border)] bg-[var(--surface)] text-foreground placeholder:text-[var(--text-dim)] outline-none focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--accent)]/20 disabled:opacity-60 transition"
            />
          </div>

          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 flex items-center gap-1.5">
              <KeyRound size={12} /> Senha
            </label>
            <div className="relative">
              <input
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                type={showPwd ? 'text' : 'password'}
                placeholder="Sua senha"
                disabled={loading}
                className="w-full px-4 py-2.5 pr-11 rounded-lg border border-[var(--border)] bg-[var(--surface)] text-foreground placeholder:text-[var(--text-dim)] outline-none focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--accent)]/20 disabled:opacity-60 transition"
              />
              <button
                type="button"
                onClick={() => setShowPwd((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-foreground transition"
                tabIndex={-1}
              >
                {showPwd ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 px-5 py-2.5 rounded-lg bg-[var(--accent)] hover:brightness-110 active:brightness-95 text-white font-semibold transition disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {loading && <Loader2 size={16} className="animate-spin" />}
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <div className="text-center mt-6">
          <button
            onClick={() => router.push('/register')}
            className="text-sm text-[var(--text-muted)] hover:text-foreground transition-colors"
          >
            Não tem conta? Cadastre-se
          </button>
        </div>
      </div>
    </div>
  );
}
