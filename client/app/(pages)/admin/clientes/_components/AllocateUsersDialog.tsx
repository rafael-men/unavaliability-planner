'use client';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import type { Cliente } from './types';

interface Props {
  cliente: Cliente | null;
  users: any[];
  /** ids de usuários já alocados ao cliente. */
  jaAlocadosIds: number[];
  onHide: () => void;
  onAllocate: (userId: number, clienteId: number) => void;
}

/** Diálogo para alocar usuários disponíveis a um cliente. */
export function AllocateUsersDialog({ cliente, users, jaAlocadosIds, onHide, onAllocate }: Props) {
  const disponiveis = cliente ? users.filter((u) => !jaAlocadosIds.includes(u.id)) : [];

  return (
    <Dialog
      header={`Alocar usuários a: ${cliente?.nome || ''}`}
      visible={!!cliente}
      onHide={onHide}
      style={{ width: 560 }}
      modal
    >
      {cliente && (
        disponiveis.length === 0 ? (
          <div className="text-center text-[var(--text-muted)] py-6 text-sm">
            Todos os usuários já estão alocados a este cliente.
          </div>
        ) : (
          <div className="space-y-2 max-h-[60vh] overflow-y-auto">
            {disponiveis.map((u) => (
              <div key={u.id} className="flex items-center gap-2 px-3 py-2 border-b border-[var(--border)]">
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-medium">{u.full_name}</div>
                  <div className="text-[10px] text-[var(--text-muted)]">
                    {u.email}{u.department ? ` · ${u.department}` : ''}
                  </div>
                </div>
                <Button label="Alocar" size="small" onClick={() => onAllocate(u.id, cliente.id)} />
              </div>
            ))}
          </div>
        )
      )}
    </Dialog>
  );
}
