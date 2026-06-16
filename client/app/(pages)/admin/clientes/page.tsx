'use client';

import { useCallback, useMemo, useState } from 'react';
import { Button } from 'primereact/button';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { Briefcase, Plus, ChevronLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { PageHeader } from '../../../components/PageHeader';
import { EmptyState } from '../../../components/EmptyState';
import { withAuth } from '../../../components/withAuth';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { isEditorRole } from '../../../lib/client-config';
import { usePageData } from '../../../lib/hooks/usePageData';
import { useToast } from '../../../providers';
import type { Cliente, User, UserClienteLink } from '../../../lib/types';
import { ClienteCard } from './_components/ClienteCard';
import { ClienteFormDialog } from './_components/ClienteFormDialog';
import { AllocateUsersDialog } from './_components/AllocateUsersDialog';
import { UserAllocationPanel } from './_components/UserAllocationPanel';

interface ClientesData { users: User[]; clientes: Cliente[]; links: UserClienteLink[]; }

const fetchClientes = async (): Promise<ClientesData> => {
  API.clearCache();
  const [users, c] = await Promise.all([API.getUsers(), API.getClientes()]);
  return { users, clientes: c.clientes, links: c.links };
};

function AdminClientesPage() {
  const router = useRouter();
  const toast = useToast();
  const { data, loading, reload } = usePageData<ClientesData>(fetchClientes, { users: [], clientes: [], links: [] });
  const { users, clientes, links } = data;
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Cliente | null>(null);
  const [addToCliente, setAddToCliente] = useState<Cliente | null>(null);

  const usersByCliente = useMemo(() => {
    const map: Record<number, number[]> = {};
    links.filter((l) => l.ativo).forEach((l) => {
      (map[l.cliente_id] ??= []).push(l.user_id);
    });
    return map;
  }, [links]);

  function openModal(c: Cliente | null) {
    setEditing(c);
    setModalOpen(true);
  }

  function deleteCliente(c: Cliente) {
    confirmDialog({
      message: `Excluir o cliente "${c.nome}"? Todas as alocações de usuários a este cliente serão removidas.`,
      header: 'Confirmar exclusão',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try { await API.deleteCliente(c.id); toast.show('Cliente excluído.'); reload(); }
        catch (e: any) { toast.show(e.message, 'error'); }
      },
    });
  }

  const toggleAlloc = useCallback(async (clienteId: number, userId: number, ativo: boolean) => {
    try {
      await API.assignUserToCliente(clienteId, userId, ativo);
      toast.show(ativo ? 'Usuário alocado!' : 'Usuário desalocado.');
      reload();
    } catch (e: any) { toast.show(e.message, 'error'); }
  }, [toast, reload]);

  if (loading) {
    return <div className="min-h-screen"><Navbar /><div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8"><Skeleton rows={4} /></div></div>;
  }

  const totalAtivos = clientes.filter((c) => c.ativo).length;
  const totalAlocacoes = links.filter((l) => l.ativo).length;

  return (
    <div className="min-h-screen">
      <Navbar />
      <ConfirmDialog />
      <div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8">
        <PageHeader
          title="Gerenciar Clientes"
          icon={Briefcase}
          description={`${clientes.length} cliente(s) · ${totalAtivos} ativo(s) · ${totalAlocacoes} alocação(ões)`}
          actions={
            <>
              <Button label="Novo Cliente" icon={<Plus size={14} />} onClick={() => openModal(null)} size="small" />
              <Button label="Voltar" icon={<ChevronLeft size={14} />} severity="secondary" outlined size="small" onClick={() => router.push('/unavailability')} />
            </>
          }
        />

        {clientes.length === 0 ? (
          <EmptyState
            icon={Briefcase}
            title="Nenhum cliente cadastrado"
            description="Comece criando um cliente para alocar usuários."
            action={<Button label="Criar primeiro cliente" icon={<Plus size={14} />} onClick={() => openModal(null)} size="small" />}
          />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {clientes.map((c) => {
              const alocadosIds = usersByCliente[c.id] || [];
              const alocados = users.filter((u) => alocadosIds.includes(u.id));
              return (
                <ClienteCard
                  key={c.id}
                  cliente={c}
                  alocados={alocados}
                  onAdd={setAddToCliente}
                  onEdit={openModal}
                  onDelete={deleteCliente}
                  onUnassign={(clienteId, userId) => toggleAlloc(clienteId, userId, false)}
                />
              );
            })}
          </div>
        )}

        <ClienteFormDialog visible={modalOpen} editing={editing} onHide={() => setModalOpen(false)} onSaved={reload} />

        <AllocateUsersDialog
          cliente={addToCliente}
          users={users}
          jaAlocadosIds={addToCliente ? (usersByCliente[addToCliente.id] || []) : []}
          onHide={() => setAddToCliente(null)}
          onAllocate={async (userId, clienteId) => { await toggleAlloc(clienteId, userId, true); }}
        />

        <UserAllocationPanel
          users={users}
          clientes={clientes}
          links={links}
          onUnassign={(clienteId, userId) => toggleAlloc(clienteId, userId, false)}
          onAllocate={(userId, clienteId) => toggleAlloc(clienteId, userId, true)}
        />
      </div>
    </div>
  );
}

export default withAuth(AdminClientesPage, isEditorRole);
