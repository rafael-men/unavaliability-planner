'use client';

import { Button } from 'primereact/button';
import { Dropdown } from 'primereact/dropdown';
import { Trash2, Building2 } from 'lucide-react';
import { Card } from '../../../../components/Card';
import { ROLE_LABELS, ROLES, ASSIGNABLE_ROLES } from '../../../../lib/client-config';
import { statusBadgeClasses } from '../../../../lib/ui-utils';
import type { User } from '../../../../lib/types';

interface Props {
  users: User[];
  currentUserId: number;
  isMaster: boolean;
  busyKey: string | number | null;
  disabled: boolean;
  onChangeRole: (id: number, role: string) => void;
  onAssignSetor: (id: number) => void;
  onDelete: (id: number) => void;
}


export function UsersTable({ users, currentUserId, isMaster, busyKey, disabled, onChangeRole, onAssignSetor, onDelete }: Props) {
  return (
    <Card>
      <h4 className="mb-3 font-semibold">Todos os Usuários ({users.length})</h4>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-[var(--surface)] border-b border-[var(--border)]">
              <th className="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-[var(--text-muted)]">Nome</th>
              <th className="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-[var(--text-muted)]">Email</th>
              <th className="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-[var(--text-muted)]">Setor</th>
              <th className="px-4 py-3 text-center text-[11px] uppercase tracking-wider text-[var(--text-muted)]">Role</th>
              <th className="px-4 py-3 text-center text-[11px] uppercase tracking-wider text-[var(--text-muted)]">Status</th>
              <th className="px-4 py-3 text-center text-[11px] uppercase tracking-wider text-[var(--text-muted)]">Ações</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const isLider = u.role === ROLES.LIDER;
              return (
                <tr key={u.id} className="border-b border-[var(--border)] last:border-0">
                  <td className="px-4 py-2.5 font-medium">
                    {u.full_name}
                    {isLider && <span className="ml-1.5 text-[9px] bg-orange-500 text-white px-1.5 py-0.5 rounded">LÍDER</span>}
                  </td>
                  <td className="px-4 py-2.5 text-[var(--text-muted)] text-xs">{u.email}</td>
                  <td className="px-4 py-2.5 text-xs">{u.department || '-'}</td>
                  <td className="px-4 py-2.5 text-center">
                    {u.role === ROLES.ADMIN_MASTER ? (
                      <span className="text-[11px] bg-purple-500 text-white px-2 py-0.5 rounded">Admin Master</span>
                    ) : (
                      <Dropdown
                        value={u.role}
                        options={ASSIGNABLE_ROLES.map((r) => ({ label: ROLE_LABELS[r] || r, value: r }))}
                        onChange={(e) => onChangeRole(u.id, e.value)}
                        disabled={u.id === currentUserId}
                        className="text-xs"
                      />
                    )}
                  </td>
                  <td className="px-4 py-2.5 text-center">
                    <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-full border ${statusBadgeClasses(u.status)}`}>{u.status}</span>
                  </td>
                  <td className="px-4 py-2.5 text-center">
                    <div className="flex gap-1 justify-center">
                      {isMaster && u.role !== ROLES.ADMIN_MASTER && (
                        <Button icon={<Building2 size={12} />} size="small" severity="secondary" outlined label="Setor" disabled={disabled} onClick={() => onAssignSetor(u.id)} />
                      )}
                      {u.id !== currentUserId && (
                        <Button icon={<Trash2 size={12} />} size="small" severity="danger" outlined label="Remover" loading={busyKey === `delete-${u.id}`} disabled={disabled} onClick={() => onDelete(u.id)} />
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
