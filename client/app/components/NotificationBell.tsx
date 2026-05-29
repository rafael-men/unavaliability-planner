'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Bell, UserPlus, Calendar } from 'lucide-react';
import { API } from '../lib/api-client';

interface NotificationItem {
  id: string;
  type: 'pending_user' | 'pending_unavailability';
  title: string;
  subtitle: string;
  created_at: string | null;
  href: string;
}

const POLL_MS = 30_000;
const SEEN_KEY = 'notif:seen-ids';

function loadSeenIds(): Set<string> {
  if (typeof window === 'undefined') return new Set();
  try {
    const raw = localStorage.getItem(SEEN_KEY);
    if (!raw) return new Set();
    const arr = JSON.parse(raw);
    return new Set(Array.isArray(arr) ? arr : []);
  } catch {
    return new Set();
  }
}

function saveSeenIds(ids: Set<string>) {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(SEEN_KEY, JSON.stringify([...ids]));
  } catch {}
}

export function NotificationBell() {
  const router = useRouter();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [open, setOpen] = useState(false);
  const [seenIds, setSeenIds] = useState<Set<string>>(() => loadSeenIds());
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchItems = useCallback(async () => {
    try {
      const res = await API.getNotifications();
      setItems(res.items || []);
    } catch {}
  }, []);

  useEffect(() => {
    fetchItems();
    const id = setInterval(fetchItems, POLL_MS);
    return () => clearInterval(id);
  }, [fetchItems]);

  useEffect(() => {
    if (!open) return;
    function onClick(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [open]);

  const unread = items.filter((i) => !seenIds.has(i.id));
  const unreadCount = unread.length;

  function markAllSeen() {
    const next = new Set(seenIds);
    items.forEach((i) => next.add(i.id));
    setSeenIds(next);
    saveSeenIds(next);
  }

  function handleOpen() {
    const next = !open;
    setOpen(next);
    if (next && unreadCount > 0) {
      markAllSeen();
    }
  }

  function handleClickItem(item: NotificationItem) {
    setOpen(false);
    router.push(item.href);
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={handleOpen}
        className="relative flex items-center justify-center w-8 h-8 rounded-lg bg-[var(--surface)] border border-[var(--border)] hover:bg-[var(--surface2)] transition-all"
        aria-label="Notificações"
        title="Notificações"
      >
        <Bell size={16} className={unreadCount > 0 ? 'text-[var(--accent)]' : 'text-[var(--text-muted)]'} />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 min-w-[16px] h-[16px] px-1 flex items-center justify-center rounded-full bg-red-500 text-white text-[9px] font-bold leading-none border border-background">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 max-h-96 overflow-y-auto bg-[var(--card)] border border-[var(--border)] rounded-lg shadow-2xl z-50">
          <div className="px-4 py-3 border-b border-[var(--border)] flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">Notificações</span>
            <span className="text-[10px] text-[var(--text-muted)]">{items.length} total</span>
          </div>

          {items.length === 0 ? (
            <div className="px-4 py-8 text-center text-xs text-[var(--text-muted)]">
              Nenhuma notificação pendente.
            </div>
          ) : (
            <div className="flex flex-col">
              {items.map((item) => {
                const Icon = item.type === 'pending_user' ? UserPlus : Calendar;
                const iconColor = item.type === 'pending_user' ? 'text-emerald-400' : 'text-yellow-400';
                return (
                  <button
                    key={item.id}
                    onClick={() => handleClickItem(item)}
                    className="flex items-start gap-3 px-4 py-3 hover:bg-[var(--surface)] border-b border-[var(--border)] last:border-0 text-left transition-colors"
                  >
                    <Icon size={16} className={`${iconColor} flex-shrink-0 mt-0.5`} />
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-semibold truncate">{item.title}</div>
                      <div className="text-[11px] text-[var(--text-muted)] truncate">{item.subtitle}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
