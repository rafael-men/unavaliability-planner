'use client';

import { Dropdown } from 'primereact/dropdown';
import { X, Users } from 'lucide-react';
import { Card } from '../../../../components/Card';
import type { Cliente, UserClienteLink } from './types';

interface Props {
  users: any[];
  clientes: Cliente[];
  links: UserClienteLink[];
  onUnassign: (clienteId: number, userId: number) => void;
  onAllocate: (userId: number, clienteId: number) => void;
}

export function UserAllocationPanel({ users, clientes, links, onUnassign, onAllocate }: Props) {
  if (!users.length || !clientes.length) return null;

  return (
    <Card className="!p-4 mt-6">
      <h3 className="text-sm font-bold mb-3 flex items-center gap-2">
        <Users size={16} /> Visão por Usuário
      </h3>
      <div className="text-xs text-[var(--text-muted)] mb-3">Alocação rápida: escolha um cliente para adicionar.</div>
      <div className="max-h-96 overflow-y-auto flex flex-col gap-1.5">
        {users.map((u) => {
          const userClientesIds = links.filter((l) => l.user_id === u.id && l.ativo).map((l) => l.cliente_id);
          const userClientes = clientes.filter((c) => userClientesIds.includes(c.id));
          const disponiveis = clientes.filter((c) => c.ativo && !userClientesIds.includes(c.id));
          return (
            <div key={u.id} className="flex items-center gap-2 px-2 py-1.5 rounded border border-[var(--border)] flex-wrap">
              <div className="min-w-0 flex-1">
                <div className="text-xs font-medium truncate">{u.full_name}</div>
                <div className="text-[10px] text-[var(--text-muted)] truncate">{u.email}</div>
              </div>
              <div className="flex flex-wrap gap-1">
                {userClientes.map((c) => (
                  <span key={c.id} className="text-[10px] px-2 py-0.5 rounded bg-[var(--accent)]/15 text-[var(--accent)] border border-[var(--accent)]/30 flex items-center gap-1">
                    {c.nome}
                    <button onClick={() => onUnassign(c.id, u.id)} className="hover:text-red-400" title="Desalocar">
                      <X size={10} />
                    </button>
                  </span>
                ))}
              </div>
              {disponiveis.length > 0 && (
                <Dropdown
                  options={[{ label: '+ Alocar...', value: 0 }, ...disponiveis.map((c) => ({ label: c.nome, value: c.id }))]}
                  value={0}
                  onChange={(e) => e.value && onAllocate(u.id, e.value)}
                  className="text-xs"
                />
              )}
            </div>
          );
        })}
      </div>
    </Card>
  );
}
