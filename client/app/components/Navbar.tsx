'use client';

import { useState, useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '../providers';
import { API } from '../lib/api-client';
import { ROLE_LABELS, isAdminRole, isMasterAdminRole } from '../lib/client-config';
import { Calendar, Users, Building2, ClipboardList, LogOut, User, Briefcase, CalendarRange, CalendarCheck, Menu, X, Ticket, LucideIcon } from 'lucide-react';
import { NotificationBell } from './NotificationBell';

interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  show: boolean;
}

export function Navbar() {
  const { user, setUser } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  useEffect(() => { setMenuOpen(false); }, [pathname]);

  if (!user) return null;

  const roleLabel = ROLE_LABELS[user.role] || user.role;
  const showAdminBtns = isAdminRole(user.role);
  const isMaster = isMasterAdminRole(user.role);
  const isEditor = user.role === 'admin_master' || user.role === 'admin_editor';

  const items: NavItem[] = [
    { label: 'Usuários', href: '/admin/users', icon: Users, show: showAdminBtns },
    { label: 'Setores', href: '/admin/setores', icon: Building2, show: isMaster },
    { label: 'Membros', href: '/admin/members', icon: ClipboardList, show: isMaster },
    { label: 'Clientes', href: '/admin/clientes', icon: Briefcase, show: isEditor },
    { label: 'Eventos', href: '/admin/eventos', icon: CalendarRange, show: isEditor },
    { label: 'Tickets', href: '/admin/tickets', icon: Ticket, show: isEditor },
    { label: 'Indisponibilidade', href: '/unavailability', icon: Calendar, show: true },
  ].filter((i) => i.show);

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
      <div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-3 flex items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <button
            onClick={() => router.push('/unavailability')}
            className="flex items-center gap-2 text-base font-extrabold tracking-tight min-w-0"
            aria-label="Ir para Indisponibilidade"
          >
            <CalendarCheck size={22} className="text-[var(--accent)] shrink-0" />
            <span className="bg-gradient-to-br from-[var(--accent)] to-blue-300 bg-clip-text text-transparent truncate">
              Indisponibilidade
            </span>
          </button>
          <span className="hidden sm:inline text-[10px] px-2 py-0.5 rounded-full font-semibold uppercase tracking-wider bg-[var(--accent)]/10 text-[var(--accent)] border border-[var(--accent)]/25">
            {roleLabel}
          </span>
        </div>

        <div className="hidden md:flex items-center gap-2 flex-wrap justify-end">
          <span className="text-xs text-[var(--text-muted)] flex items-center gap-1.5">
            <User size={14} /> {user.full_name}
          </span>
          {items.map((it) => (
            <button key={it.href} onClick={() => router.push(it.href)} className={navBtn(pathname.startsWith(it.href))}>
              <it.icon size={14} /> {it.label}
            </button>
          ))}
          <NotificationBell />
          <button onClick={doLogout} className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs text-[var(--text-muted)] hover:text-red-400 transition-colors">
            <LogOut size={14} /> Sair
          </button>
        </div>
        <div className="flex md:hidden items-center gap-1">
          <NotificationBell />
          <button
            onClick={() => setMenuOpen((v) => !v)}
            aria-label={menuOpen ? 'Fechar menu' : 'Abrir menu'}
            aria-expanded={menuOpen}
            className="p-2 rounded-lg text-foreground hover:bg-[var(--surface2)] transition-colors"
          >
            {menuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>
      {menuOpen && (
        <div className="md:hidden border-t border-[var(--border)] bg-background/95 backdrop-blur-md">
          <div className="max-w-[1440px] mx-auto px-4 py-3 flex flex-col gap-1.5">
            <div className="flex items-center gap-2 px-1 py-2 text-xs text-[var(--text-muted)]">
              <User size={14} /> {user.full_name}
              <span className="ml-auto text-[10px] px-2 py-0.5 rounded-full font-semibold uppercase tracking-wider bg-[var(--accent)]/10 text-[var(--accent)] border border-[var(--accent)]/25">
                {roleLabel}
              </span>
            </div>
            {items.map((it) => {
              const active = pathname.startsWith(it.href);
              return (
                <button
                  key={it.href}
                  onClick={() => router.push(it.href)}
                  className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                    active
                      ? 'bg-[var(--accent)]/15 text-[var(--accent)] border border-[var(--accent)]/30'
                      : 'text-foreground hover:bg-[var(--surface2)] border border-transparent'
                  }`}
                >
                  <it.icon size={16} /> {it.label}
                </button>
              );
            })}
            <button onClick={doLogout} className="flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm text-[var(--text-muted)] hover:text-red-400 hover:bg-[var(--surface2)] transition-colors mt-1">
              <LogOut size={16} /> Sair
            </button>
          </div>
        </div>
      )}
    </nav>
  );
}
