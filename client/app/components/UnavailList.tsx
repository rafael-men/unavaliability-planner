'use client';

import { useState } from 'react';
import { Button } from 'primereact/button';
import { Checkbox } from 'primereact/checkbox';
import { User, Calendar, CalendarRange, CalendarCheck, Pencil, X, CheckCircle2, XCircle, AlertTriangle, CalendarClock, History } from 'lucide-react';
import { DEPT_COLORS, STATUS_MAP, formatDate, isEditorRole, isLiderRole, AppUser } from '../lib/client-config';
import type { UnavailabilityRecord } from '../lib/types';
import { Card } from './Card';
import { CancelUnavailDialog } from '../(pages)/unavailability/_components/CancelUnavailDialog';
import { HistoryDialog } from '../(pages)/unavailability/_components/HistoryDialog';

type Item = UnavailabilityRecord;

interface Props {
  items: Item[];
  showActions?: boolean;
  showUser?: boolean;
  showCheckbox?: boolean;
  selectedIds?: Set<number>;
  onToggleSelect?: (id: number) => void;
  onApprove?: (id: number) => void;
  onReject?: (id: number) => void;
  onEdit?: (item: Item) => void;
  onDelete?: (id: number) => void;
  /** Recarrega a lista após cancelar/antecipar. */
  onChanged?: () => void;
  currentUser: AppUser;
}

export function UnavailList({
  items,
  showActions,
  showUser,
  showCheckbox,
  selectedIds,
  onToggleSelect,
  onApprove,
  onReject,
  onEdit,
  onDelete,
  onChanged,
  currentUser,
}: Props) {
  const today = new Date().toISOString().split('T')[0];
  const canManage = isEditorRole(currentUser.role) || isLiderRole(currentUser.role);
  const [cancelTarget, setCancelTarget] = useState<Item | null>(null);
  const [historyId, setHistoryId] = useState<number | null>(null);

  return (
    <div className="flex flex-col gap-3">
      {items.map((item) => {
        const st = STATUS_MAP[item.status] || STATUS_MAP.pending;
        const isActive = item.status === 'approved' && item.start_date <= today && item.end_date >= today;
        const deptColor = (item.department && DEPT_COLORS[item.department]) || 'var(--accent)';
        const canEdit = item.status === 'pending' && (item.user_id === currentUser.id || isEditorRole(currentUser.role));

        return (
          <Card
            key={item.id}
            borderColor={deptColor}
            className={isActive ? 'bg-gradient-to-br from-[var(--card)] to-emerald-500/5' : ''}
          >
            <div className="flex justify-between items-start gap-4 flex-wrap">
              {showCheckbox && (
                <div className="pt-1">
                  <Checkbox
                    checked={selectedIds?.has(item.id) || false}
                    onChange={() => onToggleSelect?.(item.id)}
                  />
                </div>
              )}
              <div className="flex-1 min-w-[200px]">
                {showUser && (
                  <div className="text-base font-semibold flex items-center gap-2 mb-1">
                    <User size={16} />
                    {item.user_name || item.full_name}
                  </div>
                )}
                <div className="text-xs text-[var(--text-muted)] mb-2">
                  {showUser && item.user_email ? `${item.user_email} · ` : ''}
                  {item.department}
                </div>

                <div className="flex gap-2 flex-wrap mb-2">
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full inline-flex items-center gap-1.5 ${st.bg} ${st.color} border ${st.border}`}>
                    {st.label}
                  </span>
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-full inline-flex items-center gap-1.5 bg-[var(--surface)] border border-[var(--border)] text-[var(--text-muted)]">
                    {item.unavailability_type === 'prolongado' ? <><CalendarRange size={12} /> Prolongado</> : <><Calendar size={12} /> Pontual</>}
                  </span>
                  {isActive && (
                    <span className="text-xs font-semibold px-2.5 py-1 rounded-full inline-flex items-center gap-1.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      <CalendarCheck size={12} /> Ativo agora
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-3 gap-4 text-sm mt-2">
                  <div>
                    <span className="text-[var(--text-muted)]">Início: </span>
                    <strong>{formatDate(item.start_date)}</strong>
                  </div>
                  <div>
                    <span className="text-[var(--text-muted)]">Fim: </span>
                    <strong>{formatDate(item.end_date)}</strong>
                  </div>
                  <div>
                    <span className="text-[var(--text-muted)]">Dias: </span>
                    <strong className="font-mono">{item.total_days}</strong>
                  </div>
                </div>

                {item.event_conflicts && item.event_conflicts.length > 0 && (
                  <div className="mt-3 px-3 py-2.5 bg-red-500/10 border border-red-500/30 rounded-lg">
                    <div className="flex items-center gap-1.5 text-red-400 text-xs font-semibold uppercase tracking-wider mb-1.5">
                      <AlertTriangle size={13} />
                      Conflito com evento{item.event_conflicts.length > 1 ? 's' : ''} de cliente
                    </div>
                    <div className="flex flex-col gap-1.5">
                      {item.event_conflicts.map((ev) => (
                        <div key={ev.id} className="text-xs">
                          <div className="font-semibold text-red-300">{ev.nome}</div>
                          <div className="text-[var(--text-muted)]">
                            {formatDate(ev.data_inicio)} → {formatDate(ev.data_fim)}
                            {ev.clientes.length > 0 && (
                              <> · <span className="text-red-300">{ev.clientes.join(', ')}</span></>
                            )}
                          </div>
                          {ev.descricao && (
                            <div className="text-[var(--text-muted)] text-[11px] mt-0.5 line-clamp-2">{ev.descricao}</div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              <div className="flex flex-col gap-1.5 items-end">
                {showActions && (
                  <>
                    <Button size="small" severity="success" outlined onClick={() => onApprove?.(item.id)} icon={<CheckCircle2 size={12} />} label="Aprovar" />
                    <Button size="small" severity="danger" outlined onClick={() => onReject?.(item.id)} icon={<XCircle size={12} />} label="Rejeitar" />
                  </>
                )}
                {canEdit && (
                  <>
                    <Button size="small" severity="secondary" outlined onClick={() => onEdit?.(item)} icon={<Pencil size={12} />} label="Editar" />
                    <Button size="small" severity="danger" outlined onClick={() => onDelete?.(item.id)} icon={<X size={12} />} label="Cancelar" />
                  </>
                )}
                {item.status === 'approved' && canManage && (
                  <Button size="small" severity="warning" outlined onClick={() => setCancelTarget(item)} icon={<CalendarClock size={12} />} label="Encerrar/Antecipar" />
                )}
                <Button size="small" severity="secondary" text onClick={() => setHistoryId(item.id)} icon={<History size={12} />} label="Histórico" />
              </div>
            </div>
          </Card>
        );
      })}

      <CancelUnavailDialog
        record={cancelTarget}
        onHide={() => setCancelTarget(null)}
        onDone={() => { setCancelTarget(null); onChanged?.(); }}
      />
      <HistoryDialog unavailabilityId={historyId} onHide={() => setHistoryId(null)} />
    </div>
  );
}
