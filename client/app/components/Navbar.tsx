'use client';

import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '../providers';
import { API } from '../lib/api-client';
import { ROLE_LABELS, isAdminRole, isMasterAdminRole } from '../lib/client-config';
import { Calendar, Users, Building2, ClipboardList, LogOut, User, Briefcase, CalendarRange, CalendarCheck } from 'lucide-react';
import { NotificationBell } from './NotificationBell';

export function Navbar() {
  const { user, setUser } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  if (!user) return null;

  const roleLabel = ROLE_LABELS[user.role] || user.role;
  const showAdminBtns = isAdminRole(user.role);
  const isMaster = isMasterAdminRole(user.role);
  const isEditor = user.role === 'admin_master' || user.role === 'admin_editor';

  async function doLogout() {
    try { await API.logout(); } catch {}
    setUser(null);
    router.push('/login');
  }

  function navBtn(active: boolean) {
    return `flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
      active
        ? 'bg-[var(--accent)]/15 text-[var(--accent)] border border-[var(--accent)]/30'
        : 'bg-[var(--surface)] text-foreground border border-[var(--border)] hover:bg-[var(--surface2)]'
    }`;
  }

  return (
    <nav className="sticky top-0 z-50 backdrop-blur-md bg-background/80 border-b border-[var(--border)]">
      <div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-4">
          <button
            onClick={() => router.push('/unavailability')}
            className="flex items-center gap-2 text-base font-extrabold tracking-tight"
            aria-label="Ir para Indisponibilidade"
          >
            <CalendarCheck size={22} className="text-[var(--accent)]" />
            <span className="bg-gradient-to-br from-[var(--accent)] to-blue-300 bg-clip-text text-transparent">
              Indisponibilidade
            </span>
          </button>
          <span className="text-[10px] px-2 py-0.5 rounded-full font-semibold uppercase tracking-wider bg-[var(--accent)]/10 text-[var(--accent)] border border-[var(--accent)]/25">
            {roleLabel}
          </span>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-[var(--text-muted)] flex items-center gap-1.5">
            <User size={14} /> {user.full_name}
          </span>
          {showAdminBtns && (
            <button onClick={() => router.push('/admin/users')} className={navBtn(pathname.startsWith('/admin/users'))}>
              <Users size={14} /> Usuários
            </button>
          )}
          {isMaster && (
            <button onClick={() => router.push('/admin/setores')} className={navBtn(pathname.startsWith('/admin/setores'))}>
              <Building2 size={14} /> Setores
            </button>
          )}
          {isMaster && (
            <button onClick={() => router.push('/admin/members')} className={navBtn(pathname.startsWith('/admin/members'))}>
              <ClipboardList size={14} /> Membros
            </button>
          )}
          {isEditor && (
            <button onClick={() => router.push('/admin/clientes')} className={navBtn(pathname.startsWith('/admin/clientes'))}>
              <Briefcase size={14} /> Clientes
            </button>
          )}
          {isEditor && (
            <button onClick={() => router.push('/admin/eventos')} className={navBtn(pathname.startsWith('/admin/eventos'))}>
              <CalendarRange size={14} /> Eventos
            </button>
          )}
          <button onClick={() => router.push('/unavailability')} className={navBtn(pathname.startsWith('/unavailability'))}>
            <Calendar size={14} /> Indisponibilidade
          </button>
          <NotificationBell />
          <button onClick={doLogout} className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs text-[var(--text-muted)] hover:text-red-400 transition-colors">
            <LogOut size={14} /> Sair
          </button>
        </div>
      </div>
    </nav>
  );
}
