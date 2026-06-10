'use client';

import { Button } from 'primereact/button';
import { Plus, Pencil, X, AlertTriangle, Users } from 'lucide-react';
import { Card } from '../../../../components/Card';
import type { Cliente } from './types';

interface Props {
  cliente: Cliente;
  alocados: any[];
  onAdd: (c: Cliente) => void;
  onEdit: (c: Cliente) => void;
  onDelete: (c: Cliente) => void;
  onUnassign: (clienteId: number, userId: number) => void;
}


export function ClienteCard({ cliente: c, alocados, onAdd, onEdit, onDelete, onUnassign }: Props) {
  return (
    <Card className={`!p-4 ${!c.ativo ? 'opacity-60' : ''}`}>
      <div className="flex justify-between items-start mb-3 gap-2">
        <div className="min-w-0 flex-1">
          <h3 className="text-sm font-bold flex items-center gap-2">
            {c.nome}
            {!c.ativo && (
              <span className="text-[9px] px-1.5 py-0.5 rounded bg-red-500/15 text-red-400 uppercase tracking-wider">Inativo</span>
            )}
          </h3>
          {c.descricao && (
            <div className="text-[11px] text-[var(--text-muted)] mt-0.5 line-clamp-2">{c.descricao}</div>
          )}
          <div className="text-xs text-[var(--text-muted)] mt-1 flex items-center gap-1">
            <Users size={11} /> {alocados.length} usuário(s) alocado(s)
          </div>
        </div>
        <div className="flex gap-1 flex-shrink-0">
          <Button icon={<Plus size={12} />} size="small" severity="secondary" outlined onClick={() => onAdd(c)} tooltip="Alocar usuário" />
          <Button icon={<Pencil size={12} />} size="small" severity="secondary" outlined onClick={() => onEdit(c)} tooltip="Editar" />
          <Button icon={<X size={12} />} size="small" severity="danger" outlined onClick={() => onDelete(c)} tooltip="Excluir" />
        </div>
      </div>

      {alocados.length === 0 ? (
        <div className="text-xs text-yellow-400 px-2 py-1.5 bg-[var(--surface)] rounded flex items-center gap-1.5">
          <AlertTriangle size={12} /> Nenhum usuário alocado
        </div>
      ) : (
        <div className="max-h-44 overflow-y-auto flex flex-col gap-1">
          {alocados.map((u) => (
            <div key={u.id} className="flex items-center gap-2 px-2 py-1.5 bg-[var(--surface)] rounded">
              <div className="flex-1 min-w-0">
                <div className="text-xs font-medium truncate">{u.full_name}</div>
                <div className="text-[10px] text-[var(--text-muted)] truncate">{u.email}</div>
              </div>
              <Button icon={<X size={10} />} size="small" severity="danger" outlined onClick={() => onUnassign(c.id, u.id)} tooltip="Desalocar" />
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
