'use client';

import { Button } from 'primereact/button';
import { Clock } from 'lucide-react';
import { Card } from '../../../../components/Card';
import type { User } from '../../../../lib/types';

interface Props {
  pending: User[];
  canEdit: boolean;
  busyKey: string | number | null;
  disabled: boolean;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
}


export function PendingUsersCard({ pending, canEdit, busyKey, disabled, onApprove, onReject }: Props) {
  if (!pending.length) return null;

  return (
    <Card className="mb-5 border-yellow-500/30">
      <h4 className="text-yellow-400 mb-3 font-semibold flex items-center gap-2">
        <Clock size={16} aria-hidden /> {pending.length} Aguardando Aprovação
      </h4>
      {pending.map((u) => (
        <div key={u.id} className="flex justify-between items-center py-2.5 border-b border-[var(--border)] last:border-0">
          <div>
            <div className="text-sm font-medium">{u.full_name}</div>
            <div className="text-xs text-[var(--text-muted)]">{u.email} · {u.role}</div>
          </div>
          {canEdit ? (
            <div className="flex gap-1.5">
              <Button label="Aprovar" size="small" severity="success" loading={busyKey === `approve-${u.id}`} disabled={disabled} onClick={() => onApprove(u.id)} />
              <Button label="Rejeitar" size="small" severity="danger" outlined loading={busyKey === `reject-${u.id}`} disabled={disabled} onClick={() => onReject(u.id)} />
            </div>
          ) : <span className="text-xs text-[var(--text-muted)]">Somente leitura</span>}
        </div>
      ))}
    </Card>
  );
}
