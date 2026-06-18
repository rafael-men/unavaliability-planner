'use client';

import { LucideIcon } from 'lucide-react';
import { Card } from './Card';

interface Props {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: React.ReactNode;
}

export function EmptyState({ icon: Icon, title, description, action }: Props) {
  return (
    <Card className="!p-8 text-center">
      <Icon size={40} className="mx-auto text-[var(--text-muted)] mb-3" aria-hidden />
      <h3 className="text-base font-semibold mb-1">{title}</h3>
      {description && <p className="text-xs text-[var(--text-muted)] mb-4">{description}</p>}
      {action}
    </Card>
  );
}
