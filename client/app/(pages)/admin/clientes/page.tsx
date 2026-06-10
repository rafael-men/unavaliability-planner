'use client';

import { useEffect, useState, useCallback, useMemo } from 'react';
import { Button } from 'primereact/button';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { Briefcase, Plus, ChevronLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { withAuth } from '../../../components/withAuth';
import { Card } from '../../../components/Card';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { isEditorRole } from '../../../lib/client-config';
import { useToast } from '../../../providers';
import type { Cliente, UserClienteLink } from './_components/types';
import { ClienteCard } from './_components/ClienteCard';
import { ClienteFormDialog } from './_components/ClienteFormDialog';
import { AllocateUsersDialog } from './_components/AllocateUsersDialog';
import { UserAllocationPanel } from './_components/UserAllocationPanel';

function AdminClientesPage() {
  const router = useRouter();
  const toast = useToast();
  const [users, setUsers] = useState<any[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [links, setLinks] = useState<UserClienteLink[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Cliente | null>(null);
  const [addToCliente, setAddToCliente] = useState<Cliente | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      API.clearCache();
      const [u, c] = await Promise.all([API.getUsers(), API.getClientes()]);
      setUsers(u);
      setClientes(c.clientes);
      setLinks(c.links);
    } catch (e: any) {
      toast.show(e.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { load(); }, [load]);

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
        try { await API.deleteCliente(c.id); toast.show('Cliente excluído.'); load(); }
        catch (e: any) { toast.show(e.message, 'error'); }
      },
    });
  }

  async function toggleAlloc(clienteId: number, userId: number, ativo: boolean) {
    try {
      await API.assignUserToCliente(clienteId, userId, ativo);
      toast.show(ativo ? 'Usuário alocado!' : 'Usuário desalocado.');
      load();
    } catch (e: any) { toast.show(e.message, 'error'); }
  }

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
        <div className="flex justify-between items-start mb-7 flex-wrap gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
              <Briefcase size={28} className="text-[var(--accent)]" /> Gerenciar Clientes
            </h1>
            <p className="text-[var(--text-muted)] text-sm mt-1">
              {clientes.length} cliente(s) · {totalAtivos} ativo(s) · {totalAlocacoes} alocação(ões)
            </p>
          </div>
          <div className="flex gap-2">
            <Button label="Novo Cliente" icon={<Plus size={14} />} onClick={() => openModal(null)} size="small" />
            <Button label="Voltar" icon={<ChevronLeft size={14} />} severity="secondary" outlined size="small" onClick={() => router.push('/unavailability')} />
          </div>
        </div>

        {clientes.length === 0 ? (
          <Card className="!p-8 text-center">
            <Briefcase size={40} className="mx-auto text-[var(--text-muted)] mb-3" />
            <h3 className="text-base font-semibold mb-1">Nenhum cliente cadastrado</h3>
            <p className="text-xs text-[var(--text-muted)] mb-4">Comece criando um cliente para alocar usuários.</p>
            <Button label="Criar primeiro cliente" icon={<Plus size={14} />} onClick={() => openModal(null)} size="small" />
          </Card>
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

        <ClienteFormDialog visible={modalOpen} editing={editing} onHide={() => setModalOpen(false)} onSaved={load} />

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
