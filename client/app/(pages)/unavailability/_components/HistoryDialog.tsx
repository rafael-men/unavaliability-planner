'use client';

import { useEffect, useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { CheckCircle2, XCircle, Pencil, Plus, CalendarClock, Ban } from 'lucide-react';
import { API } from '../../../lib/api-client';
import { useToast } from '../../../providers';

interface AuditEntry {
  id: number;
  action: string;
  actorName?: string | null;
  detail?: string | null;
  createdAt?: string | null;
}

interface Props {
  unavailabilityId: number | null;
  onHide: () => void;
}

const ACTION_META: Record<string, { label: string; icon: typeof Plus; color: string }> = {
  created: { label: 'Criada', icon: Plus, color: 'text-[var(--accent)]' },
  updated: { label: 'Editada', icon: Pencil, color: 'text-blue-400' },
  approved: { label: 'Aprovada', icon: CheckCircle2, color: 'text-emerald-400' },
  rejected: { label: 'Rejeitada', icon: XCircle, color: 'text-red-400' },
  canceled: { label: 'Cancelada', icon: Ban, color: 'text-red-400' },
  shortened: { label: 'Retorno antecipado', icon: CalendarClock, color: 'text-yellow-400' },
};


export function HistoryDialog({ unavailabilityId, onHide }: Props) {
  const toast = useToast();
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (unavailabilityId == null) return;
    setLoading(true);
    API.getUnavailabilityHistory(unavailabilityId)
      .then((data) => setEntries(data as AuditEntry[]))
      .catch((e: any) => toast.show(e.message, 'error'))
      .finally(() => setLoading(false));
  }, [unavailabilityId, toast]);

  return (
    <Dialog
      header="Histórico da solicitação"
      visible={unavailabilityId != null}
      onHide={onHide}
      style={{ width: 'min(92vw, 520px)' }}
      modal
    >
      {loading ? (
        <div className="text-sm text-[var(--text-muted)] py-6 text-center">Carregando…</div>
      ) : entries.length === 0 ? (
        <div className="text-sm text-[var(--text-muted)] py-6 text-center">Sem eventos registrados.</div>
      ) : (
        <ol className="relative border-l border-[var(--border)] ml-2">
          {entries.map((e) => {
            const meta = ACTION_META[e.action] || { label: e.action, icon: Plus, color: 'text-[var(--text-muted)]' };
            const Icon = meta.icon;
            return (
              <li key={e.id} className="mb-5 ml-5">
                <span className={`absolute -left-[9px] flex items-center justify-center w-4 h-4 rounded-full bg-[var(--surface2)] ${meta.color}`}>
                  <Icon size={11} />
                </span>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={`text-sm font-semibold ${meta.color}`}>{meta.label}</span>
                  {e.actorName && <span className="text-xs text-[var(--text-muted)]">por {e.actorName}</span>}
                  {e.createdAt && <span className="text-[10px] text-[var(--text-dim)]">{fmt(e.createdAt)}</span>}
                </div>
                {e.detail && <p className="text-xs text-[var(--text-muted)] mt-0.5">{e.detail}</p>}
              </li>
            );
          })}
        </ol>
      )}
    </Dialog>
  );
}

function fmt(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
