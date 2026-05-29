'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, FilePlus2, Eye, EyeOff, Loader2, ChevronDown } from 'lucide-react';
import { API } from '../../lib/api-client';
import { useAuth, useSetores } from '../../providers';

export default function RegisterPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { setores } = useSetores();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [department, setDepartment] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) router.replace('/unavailability');
  }, [user, router]);

  async function doRegister(e?: React.FormEvent) {
    e?.preventDefault();
    setError(null);
    setSuccess(null);
    if (!name || !email || !password || !department) {
      setError('Preencha todos os campos.');
      return;
    }
    setLoading(true);
    try {
      const res: any = await API.register({ email, password, full_name: name, department });
      setSuccess(res.message);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  const inputCls =
    'w-full px-4 py-2.5 rounded-lg border border-[var(--border)] bg-[var(--surface)] text-foreground placeholder:text-[var(--text-dim)] outline-none focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--accent)]/20 disabled:opacity-60 transition';

  return (
    <div className="min-h-screen flex items-center justify-center p-5">
      <div className="w-full max-w-lg bg-[var(--card)] border border-[var(--border)] rounded-2xl p-8 shadow-2xl animate-fade-up">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-[var(--accent)]/10 rounded-2xl mb-4">
            <FilePlus2 size={32} className="text-[var(--accent)]" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-br from-foreground to-[var(--accent)] bg-clip-text text-transparent">
            Cadastro
          </h1>
          <p className="text-[var(--text-muted)] text-sm mt-2">
            Crie sua conta. Um admin precisará aprovar seu acesso.
          </p>
        </div>

        <form onSubmit={doRegister} className="space-y-4">
          {error && (
            <div className="px-4 py-2.5 bg-red-500/10 text-red-400 border border-red-500/20 rounded-lg text-sm">{error}</div>
          )}
          {success && (
            <div className="px-4 py-2.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-lg text-sm">{success}</div>
          )}

          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 block">Nome Completo</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Seu nome completo"
              disabled={loading}
              className={inputCls}
            />
          </div>

          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 block">Email</label>
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              placeholder="seu@.com.br"
              disabled={loading}
              className={inputCls}
            />
          </div>

          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 block">Senha (mín. 6 caracteres)</label>
            <div className="relative">
              <input
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                type={showPwd ? 'text' : 'password'}
                placeholder="Crie uma senha"
                disabled={loading}
                className={`${inputCls} pr-11`}
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

          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2 block">Setor</label>
            <div className="relative">
              <select
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                disabled={loading}
                className={`${inputCls} appearance-none pr-10 cursor-pointer`}
              >
                <option value="">Selecione...</option>
                {setores.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] pointer-events-none" />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 px-5 py-2.5 rounded-lg bg-[var(--accent)] hover:brightness-110 active:brightness-95 text-white font-semibold transition disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {loading && <Loader2 size={16} className="animate-spin" />}
            {loading ? 'Enviando...' : 'Solicitar Cadastro'}
          </button>
        </form>

        <div className="text-center mt-6">
          <button
            onClick={() => router.push('/login')}
            className="text-sm text-[var(--text-muted)] hover:text-foreground transition-colors flex items-center gap-1.5 mx-auto"
          >
            <ArrowLeft size={14} /> Voltar ao Login
          </button>
        </div>
      </div>
    </div>
  );
}
