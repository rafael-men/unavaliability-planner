'use client';

import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { API } from '../../lib/api-client';
import { useToast } from '../../providers';

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="min-h-screen" />}>
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const toast = useToast();
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const token = searchParams.get('token') || '';

    if (!token) {
      toast.show('Link de redefinição inválido.', 'error');
      return;
    }

    if (password.length < 6 || !/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
      toast.show('A senha deve ter pelo menos 6 caracteres, uma letra e um número.', 'error');
      return;
    }

    if (password !== confirmation) {
      toast.show('As senhas não coincidem.', 'error');
      return;
    }

    setLoading(true);
    try {
      await API.resetPassword(token, password);
      toast.show('Senha redefinida com sucesso.');
      router.push('/login');
    } catch (error: unknown) {
      toast.show(error instanceof Error ? error.message : 'Link inválido ou expirado.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-5">
      <form onSubmit={submit} className="w-full max-w-md bg-[var(--card)] border border-[var(--border)] rounded-2xl p-8 shadow-2xl">
        <h1 className="text-2xl font-bold mb-2">Redefinir senha</h1>
        <p className="text-sm text-[var(--text-muted)] mb-6">
          Escolha uma nova senha para sua conta.
        </p>

        <div className="space-y-4">
          <div>
            <label htmlFor="reset-password" className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 block">
              Nova senha
            </label>
            <input
              id="reset-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="new-password"
              disabled={loading}
              className="w-full px-4 py-2.5 rounded-xl border border-[var(--border)] bg-[var(--surface)]"
            />
          </div>

          <div>
            <label htmlFor="reset-confirmation" className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 block">
              Confirmar senha
            </label>
            <input
              id="reset-confirmation"
              type="password"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              autoComplete="new-password"
              disabled={loading}
              className="w-full px-4 py-2.5 rounded-xl border border-[var(--border)] bg-[var(--surface)]"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-[var(--accent)] text-white font-semibold disabled:opacity-60"
          >
            {loading && <Loader2 size={16} className="animate-spin" />}
            {loading ? 'Salvando...' : 'Salvar nova senha'}
          </button>
        </div>
      </form>
    </div>
  );
}
