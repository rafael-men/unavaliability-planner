'use client';

import { useEffect, useState } from 'react';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Calendar as PCalendar } from 'primereact/calendar';
import { API } from '../../../lib/api-client';
import { formatDate } from '../../../lib/client-config';
import { useToast } from '../../../providers';
import type { UnavailabilityRecord } from '../../../lib/types';

interface Props {
  record: UnavailabilityRecord | null;
  onHide: () => void;
  onDone: () => void;
}


export function CancelUnavailDialog({ record, onHide, onDone }: Props) {
  const toast = useToast();
  const [mode, setMode] = useState<'cancel' | 'shorten'>('cancel');
  const [newEnd, setNewEnd] = useState<Date | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (record) { setMode('cancel'); setNewEnd(null); }
  }, [record]);

  async function confirm() {
    if (!record) return;
    if (mode === 'shorten' && !newEnd) {
      toast.show('Escolha a nova data de retorno.', 'error');
      return;
    }
    setSaving(true);
    try {
      const iso = mode === 'shorten' && newEnd ? toISO(newEnd) : undefined;
      await API.cancelUnavailability(record.id, iso);
      toast.show(mode === 'cancel'
        ? 'Período cancelado. O colaborador foi notificado por e-mail.'
        : 'Retorno antecipado. O colaborador foi notificado por e-mail.');
      onHide();
      onDone();
    } catch (e: any) {
      toast.show(e.message, 'error');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog
      header="Cancelar ou antecipar retorno"
      visible={!!record}
      onHide={onHide}
      style={{ width: 'min(92vw, 460px)' }}
      modal
    >
      {record && (
        <div className="space-y-4">
          <div className="text-sm text-[var(--text-muted)]">
            <strong className="text-foreground">{record.user_name || record.full_name}</strong><br />
            Período atual: {formatDate(record.start_date)} → {formatDate(record.end_date)} ({record.total_days} dias úteis)
          </div>

          <div className="flex gap-2">
            <Button label="Cancelar tudo" size="small" severity={mode === 'cancel' ? 'warning' : 'secondary'} outlined={mode !== 'cancel'} onClick={() => setMode('cancel')} />
            <Button label="Antecipar retorno" size="small" severity={mode === 'shorten' ? 'warning' : 'secondary'} outlined={mode !== 'shorten'} onClick={() => setMode('shorten')} />
          </div>

          {mode === 'shorten' && (
            <div>
              <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Nova data de retorno</label>
              <PCalendar
                value={newEnd}
                onChange={(e) => setNewEnd((e.value as Date) ?? null)}
                dateFormat="dd/mm/yy"
                minDate={parseISO(record.start_date)}
                maxDate={addDays(parseISO(record.end_date), -1)}
                showIcon
                className="w-full"
              />
              <p className="text-[11px] text-[var(--text-muted)] mt-1">Deve ser anterior ao fim atual. Os dias excedentes voltam para a cota.</p>
            </div>
          )}

          <div className="px-3 py-2 bg-yellow-500/10 border border-yellow-500/30 text-yellow-300 rounded text-xs">
            O colaborador será notificado por e-mail sobre esta alteração.
          </div>

          <div className="flex justify-end gap-2 pt-1">
            <Button label="Voltar" severity="secondary" outlined onClick={onHide} />
            <Button label="Confirmar" severity="warning" loading={saving} onClick={confirm} />
          </div>
        </div>
      )}
    </Dialog>
  );
}

function toISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
function parseISO(s: string): Date {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}
function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}
