'use client';

import { LucideIcon } from 'lucide-react';

interface Props {
  title: string;
  icon: LucideIcon;
  description?: string;
  actions?: React.ReactNode;
}

export function PageHeader({ title, icon: Icon, description, actions }: Props) {
  return (
    <div className="flex justify-between items-start mb-7 flex-wrap gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
          <Icon size={28} className="text-[var(--accent)]" aria-hidden /> {title}
        </h1>
        {description && <p className="text-[var(--text-muted)] text-sm mt-1">{description}</p>}
      </div>
      {actions && <div className="flex gap-2">{actions}</div>}
    </div>
  );
}
