'use client';

import { useEffect, useState, useCallback } from 'react';
import { Button } from 'primereact/button';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { Users, UserPlus, ChevronLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { withAuth } from '../../../components/withAuth';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { ROLE_LABELS, isAdminRole, isMasterAdminRole, isEditorRole } from '../../../lib/client-config';
import { useAuth, useToast, useSetores } from '../../../providers';
import { PendingUsersCard } from './_components/PendingUsersCard';
import { UsersTable } from './_components/UsersTable';
import { CreateUserDialog } from './_components/CreateUserDialog';
import { AssignSetorDialog } from './_components/AssignSetorDialog';

function AdminUsersPage() {
  const router = useRouter();
  const toast = useToast();
  const { user } = useAuth();
  const { setores } = useSetores();
  const [users, setUsers] = useState<any[]>([]);
  const [pending, setPending] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignUserId, setAssignUserId] = useState<number | null>(null);

  const isMaster = isMasterAdminRole(user!.role);
  const canEdit = isEditorRole(user!.role);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      API.clearCache();
      const [u, p] = await Promise.all([API.getUsers(), API.getPending()]);
      setUsers(u);
      setPending(p);
    } catch (e: any) {
      toast.show(e.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { load(); }, [load]);

  async function approve(id: number) {
    try { await API.approveUser(id); toast.show('Usuário aprovado!'); load(); } catch (e: any) { toast.show(e.message, 'error'); }
  }
  async function reject(id: number) {
    try { await API.rejectUser(id); toast.show('Usuário rejeitado.'); load(); } catch (e: any) { toast.show(e.message, 'error'); }
  }
  function changeRole(id: number, role: string) {
    confirmDialog({
      message: `Alterar role para "${ROLE_LABELS[role]}"? Esta ação é imediata.`,
      header: 'Confirmar',
      accept: async () => {
        try { await API.changeUserRole(id, role); toast.show('Role atualizado!'); load(); }
        catch (e: any) { toast.show(e.message, 'error'); load(); }
      },
      reject: () => load(),
    });
  }
  function deleteUser(id: number) {
    confirmDialog({
      message: 'Remover este usuário permanentemente?',
      header: 'Confirmar remoção',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try { await API.deleteUser(id); toast.show('Usuário removido.'); load(); }
        catch (e: any) { toast.show(e.message, 'error'); }
      },
    });
  }

  if (loading) {
    return <div className="min-h-screen"><Navbar /><div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8"><Skeleton rows={4} /></div></div>;
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <ConfirmDialog />
      <div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8">
        <div className="flex justify-between items-start mb-7 flex-wrap gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
              <Users size={28} className="text-[var(--accent)]" /> Gerenciar Usuários
            </h1>
            <p className="text-[var(--text-muted)] text-sm mt-1">Aprovação e gerenciamento de contas</p>
          </div>
          <div className="flex gap-2">
            {isMaster && (
              <Button label="Criar Usuário" icon={<UserPlus size={14} />} onClick={() => setCreateOpen(true)} size="small" />
            )}
            <Button label="Voltar" icon={<ChevronLeft size={14} />} severity="secondary" outlined size="small" onClick={() => router.push('/unavailability')} />
          </div>
        </div>

        <PendingUsersCard pending={pending} canEdit={canEdit} onApprove={approve} onReject={reject} />

        <UsersTable
          users={users}
          currentUserId={user!.id}
          isMaster={isMaster}
          onChangeRole={changeRole}
          onAssignSetor={(id) => { setAssignUserId(id); setAssignOpen(true); }}
          onDelete={deleteUser}
        />

        <CreateUserDialog visible={createOpen} onHide={() => setCreateOpen(false)} onSaved={load} setores={setores} />
        <AssignSetorDialog visible={assignOpen} onHide={() => setAssignOpen(false)} userId={assignUserId} users={users} setores={setores} onSaved={load} />
      </div>
    </div>
  );
}

export default withAuth(AdminUsersPage, isAdminRole);
