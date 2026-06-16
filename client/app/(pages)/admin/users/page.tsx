'use client';

import { useCallback, useState } from 'react';
import { Button } from 'primereact/button';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { Users, UserPlus, ChevronLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { PageHeader } from '../../../components/PageHeader';
import { withAuth } from '../../../components/withAuth';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { ROLE_LABELS, isAdminRole, isMasterAdminRole, isEditorRole } from '../../../lib/client-config';
import { usePageData } from '../../../lib/hooks/usePageData';
import { useAction } from '../../../lib/hooks/useAction';
import type { User } from '../../../lib/types';
import { useAuth, useToast, useSetores } from '../../../providers';
import { PendingUsersCard } from './_components/PendingUsersCard';
import { UsersTable } from './_components/UsersTable';
import { CreateUserDialog } from './_components/CreateUserDialog';
import { AssignSetorDialog } from './_components/AssignSetorDialog';

interface UsersData { users: User[]; pending: User[]; }

const fetchUsers = async (): Promise<UsersData> => {
  API.clearCache();
  const [users, pending] = await Promise.all([API.getUsers(), API.getPending()]);
  return { users, pending };
};

function AdminUsersPage() {
  const router = useRouter();
  const toast = useToast();
  const { user } = useAuth();
  const { setores } = useSetores();
  const { data, loading, reload } = usePageData<UsersData>(fetchUsers, { users: [], pending: [] });
  const { run, busyKey, isBusy } = useAction();
  const [createOpen, setCreateOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignUserId, setAssignUserId] = useState<number | null>(null);

  const isMaster = isMasterAdminRole(user!.role);
  const canEdit = isEditorRole(user!.role);

  const approve = useCallback((id: number) => run(`approve-${id}`, async () => {
    try { await API.approveUser(id); toast.show('Usuário aprovado!'); reload(); } catch (e: any) { toast.show(e.message, 'error'); }
  }), [run, toast, reload]);

  const reject = useCallback((id: number) => run(`reject-${id}`, async () => {
    try { await API.rejectUser(id); toast.show('Usuário rejeitado.'); reload(); } catch (e: any) { toast.show(e.message, 'error'); }
  }), [run, toast, reload]);

  function changeRole(id: number, role: string) {
    confirmDialog({
      message: `Alterar role para "${ROLE_LABELS[role]}"? Esta ação é imediata.`,
      header: 'Confirmar',
      accept: async () => {
        try { await API.changeUserRole(id, role); toast.show('Role atualizado!'); reload(); }
        catch (e: any) { toast.show(e.message, 'error'); reload(); }
      },
      reject: () => reload(),
    });
  }

  function deleteUser(id: number) {
    confirmDialog({
      message: 'Remover este usuário permanentemente?',
      header: 'Confirmar remoção',
      icon: 'pi pi-exclamation-triangle',
      accept: () => run(`delete-${id}`, async () => {
        try { await API.deleteUser(id); toast.show('Usuário removido.'); reload(); }
        catch (e: any) { toast.show(e.message, 'error'); }
      }),
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
        <PageHeader
          title="Gerenciar Usuários"
          icon={Users}
          description="Aprovação e gerenciamento de contas"
          actions={
            <>
              {isMaster && (
                <Button label="Criar Usuário" icon={<UserPlus size={14} />} onClick={() => setCreateOpen(true)} size="small" />
              )}
              <Button label="Voltar" icon={<ChevronLeft size={14} />} severity="secondary" outlined size="small" onClick={() => router.push('/unavailability')} />
            </>
          }
        />

        <PendingUsersCard
          pending={data.pending}
          canEdit={canEdit}
          busyKey={busyKey}
          disabled={isBusy}
          onApprove={approve}
          onReject={reject}
        />

        <UsersTable
          users={data.users}
          currentUserId={user!.id}
          isMaster={isMaster}
          busyKey={busyKey}
          disabled={isBusy}
          onChangeRole={changeRole}
          onAssignSetor={(id) => { setAssignUserId(id); setAssignOpen(true); }}
          onDelete={deleteUser}
        />

        <CreateUserDialog visible={createOpen} onHide={() => setCreateOpen(false)} onSaved={reload} setores={setores} />
        <AssignSetorDialog visible={assignOpen} onHide={() => setAssignOpen(false)} userId={assignUserId} users={data.users} setores={setores} onSaved={reload} />
      </div>
    </div>
  );
}

export default withAuth(AdminUsersPage, isAdminRole);
