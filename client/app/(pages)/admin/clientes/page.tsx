'use client';

import { useEffect, useState, useCallback, useMemo } from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { Briefcase, Plus, ChevronLeft, Pencil, X, AlertTriangle, Users } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { withAuth } from '../../../components/withAuth';
import { Card } from '../../../components/Card';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { isEditorRole } from '../../../lib/client-config';
import { useToast } from '../../../providers';

interface Cliente {
  id: number;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

interface UserClienteLink {
  user_id: number;
  cliente_id: number;
  ativo: boolean;
}

function AdminClientesPage() {
  const router = useRouter();
  const toast = useToast();
  const [users, setUsers] = useState<any[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [links, setLinks] = useState<UserClienteLink[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Cliente | null>(null);
  const [form, setForm] = useState({ nome: '', descricao: '', ativo: true });
  const [error, setError] = useState<string | null>(null);
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
      if (!map[l.cliente_id]) map[l.cliente_id] = [];
      map[l.cliente_id].push(l.user_id);
    });
    return map;
  }, [links]);

  function openModal(c: Cliente | null) {
    setEditing(c);
    setForm({
      nome: c?.nome || '',
      descricao: c?.descricao || '',
      ativo: c ? c.ativo : true,
    });
    setError(null);
    setModalOpen(true);
  }

  async function saveCliente() {
    if (!form.nome.trim()) {
      setError('Digite um nome para o cliente.');
      return;
    }
    try {
      if (!editing) {
        await API.createCliente({ nome: form.nome.trim(), descricao: form.descricao.trim() || null, ativo: form.ativo });
        toast.show('Cliente criado!');
      } else {
        await API.updateCliente(editing.id, { nome: form.nome.trim(), descricao: form.descricao.trim() || null, ativo: form.ativo });
        toast.show('Cliente atualizado!');
      }
      setModalOpen(false);
      load();
    } catch (e: any) {
      setError(e.message);
    }
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

  async function quickAssignToCliente(userId: number, clienteId: number) {
    if (!clienteId) return;
    await toggleAlloc(clienteId, userId, true);
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
                <Card key={c.id} className={`!p-4 ${!c.ativo ? 'opacity-60' : ''}`}>
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
                      <Button icon={<Plus size={12} />} size="small" severity="secondary" outlined onClick={() => setAddToCliente(c)} tooltip="Alocar usuário" />
                      <Button icon={<Pencil size={12} />} size="small" severity="secondary" outlined onClick={() => openModal(c)} tooltip="Editar" />
                      <Button icon={<X size={12} />} size="small" severity="danger" outlined onClick={() => deleteCliente(c)} tooltip="Excluir" />
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
                          <Button icon={<X size={10} />} size="small" severity="danger" outlined onClick={() => toggleAlloc(c.id, u.id, false)} tooltip="Desalocar" />
                        </div>
                      ))}
                    </div>
                  )}
                </Card>
              );
            })}
          </div>
        )}

        <Dialog
          header={editing ? 'Editar Cliente' : 'Novo Cliente'}
          visible={modalOpen}
          onHide={() => setModalOpen(false)}
          style={{ width: 480 }}
          modal
        >
          <div className="space-y-3">
            <div>
              <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Nome do cliente</label>
              <InputText value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} placeholder="Ex.: Itaú" className="w-full" autoFocus />
            </div>
            <div>
              <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Descrição (opcional)</label>
              <InputTextarea value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} rows={2} className="w-full" />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="ativo"
                checked={form.ativo}
                onChange={(e) => setForm({ ...form, ativo: e.target.checked })}
                className="w-4 h-4"
              />
              <label htmlFor="ativo" className="text-xs">Cliente ativo</label>
            </div>
            {error && <div className="px-3 py-2 bg-red-500/10 border border-red-500/30 text-red-400 rounded text-sm">{error}</div>}
            <div className="flex justify-end gap-2 pt-2">
              <Button label="Cancelar" severity="secondary" outlined onClick={() => setModalOpen(false)} />
              <Button label="Salvar" onClick={saveCliente} />
            </div>
          </div>
        </Dialog>

        <Dialog
          header={`Alocar usuários a: ${addToCliente?.nome || ''}`}
          visible={!!addToCliente}
          onHide={() => setAddToCliente(null)}
          style={{ width: 560 }}
          modal
        >
          {addToCliente && (() => {
            const jaAlocados = usersByCliente[addToCliente.id] || [];
            const disponiveis = users.filter((u) => !jaAlocados.includes(u.id));
            if (!disponiveis.length) {
              return <div className="text-center text-[var(--text-muted)] py-6 text-sm">Todos os usuários já estão alocados a este cliente.</div>;
            }
            return (
              <div className="space-y-2 max-h-[60vh] overflow-y-auto">
                {disponiveis.map((u) => (
                  <div key={u.id} className="flex items-center gap-2 px-3 py-2 border-b border-[var(--border)]">
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-medium">{u.full_name}</div>
                      <div className="text-[10px] text-[var(--text-muted)]">
                        {u.email}{u.department ? ` · ${u.department}` : ''}
                      </div>
                    </div>
                    <Button
                      label="Alocar"
                      size="small"
                      onClick={async () => {
                        await quickAssignToCliente(u.id, addToCliente.id);
                      }}
                    />
                  </div>
                ))}
              </div>
            );
          })()}
        </Dialog>

        {users.length > 0 && clientes.length > 0 && (
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
                          <button onClick={() => toggleAlloc(c.id, u.id, false)} className="hover:text-red-400" title="Desalocar">
                            <X size={10} />
                          </button>
                        </span>
                      ))}
                    </div>
                    {disponiveis.length > 0 && (
                      <Dropdown
                        options={[{ label: '+ Alocar...', value: 0 }, ...disponiveis.map((c) => ({ label: c.nome, value: c.id }))]}
                        value={0}
                        onChange={(e) => e.value && quickAssignToCliente(u.id, e.value)}
                        className="text-xs"
                      />
                    )}
                  </div>
                );
              })}
            </div>
          </Card>
        )}
      </div>
    </div>
  );
}

export default withAuth(AdminClientesPage, isEditorRole);
