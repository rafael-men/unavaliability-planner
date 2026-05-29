'use client';

import { CalendarCheck } from 'lucide-react';

export function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-[var(--border)] bg-background/60 backdrop-blur-sm mt-auto">
      <div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-6 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <CalendarCheck size={14} className="text-[var(--text-muted)]" />
          <span className="text-xs text-[var(--text-muted)]">
            © {year} · Sistema de Indisponibilidade
          </span>
        </div>
      </div>
    </footer>
  );
}
