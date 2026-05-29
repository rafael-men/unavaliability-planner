'use client';

import { useEffect, useState, useCallback } from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { Building2, Plus, ChevronLeft, Pencil, X, Star, ChevronDown, AlertTriangle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { withAuth } from '../../../components/withAuth';
import { Card } from '../../../components/Card';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { DEPT_COLORS, isMasterAdminRole } from '../../../lib/client-config';
import { useToast, useSetores } from '../../../providers';

function AdminSetoresPage() {
  const router = useRouter();
  const toast = useToast();
  const { setores, refresh: refreshSetores } = useSetores();
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [setorOpen, setSetorOpen] = useState(false);
  const [editIdx, setEditIdx] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [setorError, setSetorError] = useState<string | null>(null);
  const [addToSetor, setAddToSetor] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      API.clearCache();
      const [u] = await Promise.all([API.getUsers(), refreshSetores()]);
      setUsers(u);
    } catch (e: any) {
      toast.show(e.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [toast, refreshSetores]);

  useEffect(() => { load(); }, [load]);

  function openModal(idx: number | null) {
    setEditIdx(idx);
    setEditName(idx !== null ? (setores[idx] || '') : '');
    setSetorError(null);
    setSetorOpen(true);
  }

  async function saveSetor() {
    if (!editName.trim()) {
      setSetorError('Digite um nome para o setor.');
      return;
    }
    try {
      if (editIdx === null) {
        await API.createSetor(editName.trim());
        toast.show('Setor criado!');
      } else {
        await API.updateSetor(editIdx, editName.trim());
        toast.show('Setor renomeado!');
      }
      setSetorOpen(false);
      load();
    } catch (e: any) {
      setSetorError(e.message);
    }
  }

  function deleteSetor(idx: number, name: string) {
    confirmDialog({
      message: `Excluir o setor "${name}"? Os usuários deste setor ficarão sem setor atribuído.`,
      header: 'Confirmar exclusão',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try { await API.deleteSetor(idx); toast.show('Setor excluído.'); load(); }
        catch (e: any) { toast.show(e.message, 'error'); }
      },
    });
  }

  async function toggleLider(userId: number, makeLider: boolean, setor: string) {
    try {
      await API.assignUserSetor(userId, { setor, is_lider: makeLider });
      toast.show(makeLider ? 'Usuário promovido a líder!' : 'Líder rebaixado a membro.');
      load();
    } catch (e: any) { toast.show(e.message, 'error'); }
  }

  function removeFromSetor(userId: number) {
    confirmDialog({
      message: 'Remover este usuário do setor?',
      header: 'Confirmar',
      accept: async () => {
        try {
          await API.assignUserSetor(userId, { setor: null, is_lider: false });
          toast.show('Usuário removido do setor.');
          load();
        } catch (e: any) { toast.show(e.message, 'error'); }
      },
    });
  }

  async function quickAssign(userId: number, setor: string) {
    if (!setor) return;
    try {
      await API.assignUserSetor(userId, { setor, is_lider: false });
      toast.show('Usuário alocado ao setor!');
      load();
    } catch (e: any) { toast.show(e.message, 'error'); }
  }

  async function addToSelectedSetor(userId: number, isLider: boolean) {
    if (!addToSetor) return;
    try {
      await API.assignUserSetor(userId, { setor: addToSetor, is_lider: isLider });
      toast.show(isLider ? 'Líder adicionado ao setor!' : 'Membro adicionado ao setor!');
      setAddToSetor(null);
      load();
    } catch (e: any) { toast.show(e.message, 'error'); }
  }

  if (loading) {
    return <div className="min-h-screen"><Navbar /><div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8"><Skeleton rows={4} /></div></div>;
  }

  const semSetor = users.filter((u) => u.role !== 'admin_master' && (!u.department || !setores.includes(u.department)));
  const eligibleForAddToSetor = addToSetor ? users.filter((u) => u.role !== 'admin_master' && u.department !== addToSetor) : [];

  return (
    <div className="min-h-screen">
      <Navbar />
      <ConfirmDialog />
      <div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8">
        <div className="flex justify-between items-start mb-7 flex-wrap gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
              <Building2 size={28} className="text-[var(--accent)]" /> Gerenciar Setores
            </h1>
            <p className="text-[var(--text-muted)] text-sm mt-1">{setores.length} setores · {users.length} usuários</p>
          </div>
          <div className="flex gap-2">
            <Button label="Novo Setor" icon={<Plus size={14} />} onClick={() => openModal(null)} size="small" />
            <Button label="Voltar" icon={<ChevronLeft size={14} />} severity="secondary" outlined size="small" onClick={() => router.push('/unavailability')} />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {setores.map((setor, idx) => {
            const members = users.filter((u) => u.department === setor);
            const lideres = members.filter((u) => u.role === 'lider');
            const outros = members.filter((u) => u.role !== 'lider');
            const color = DEPT_COLORS[setor] || 'var(--accent)';

            return (
              <Card key={setor} className="!p-4" borderColor={color}>
                <div className="flex justify-between items-start mb-3 gap-2">
                  <div>
                    <h3 className="text-sm font-bold">{setor}</h3>
                    <div className="text-xs text-[var(--text-muted)]">{lideres.length} líder(es) · {outros.length} membro(s)</div>
                  </div>
                  <div className="flex gap-1 flex-shrink-0">
                    <Button icon={<Plus size={12} />} size="small" severity="secondary" outlined onClick={() => setAddToSetor(setor)} tooltip="Adicionar membro" />
                    <Button icon={<Pencil size={12} />} size="small" severity="secondary" outlined onClick={() => openModal(idx)} tooltip="Renomear" />
                    <Button icon={<X size={12} />} size="small" severity="danger" outlined onClick={() => deleteSetor(idx, setor)} tooltip="Excluir" />
                  </div>
                </div>

                <div className="mb-3">
                  <div className="text-[10px] text-orange-400 uppercase tracking-wider font-semibold mb-1.5 flex items-center gap-1">
                    <Star size={10} /> Líderes (aprovam solicitações)
                  </div>
                  {!lideres.length ? (
                    <div className="text-xs text-yellow-400 px-2 py-1.5 bg-[var(--surface)] rounded flex items-center gap-1.5">
                      <AlertTriangle size={12} /> Nenhum líder — solicitações deste setor não terão aprovador
                    </div>
                  ) : lideres.map((u) => (
                    <div key={u.id} className="flex items-center gap-2 px-2 py-1.5 bg-[var(--surface)] rounded mb-1">
                      <div className="flex-1 min-w-0">
                        <div className="text-xs font-semibold truncate">{u.full_name}</div>
                        <div className="text-[10px] text-[var(--text-muted)] truncate">{u.email}</div>
                      </div>
                      <Button label="↓ Rebaixar" size="small" severity="warning" outlined onClick={() => toggleLider(u.id, false, setor)} />
                      <Button icon={<X size={10} />} size="small" severity="danger" outlined onClick={() => removeFromSetor(u.id)} />
                    </div>
                  ))}
                </div>

                {outros.length > 0 && (
                  <div>
                    <div className="text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1.5">Membros ({outros.length})</div>
                    <div className="max-h-44 overflow-y-auto flex flex-col gap-1">
                      {outros.map((u) => (
                        <div key={u.id} className="flex items-center gap-2 px-2 py-1 rounded">
                          <div className="flex-1 min-w-0 text-xs truncate">{u.full_name}</div>
                          <Button label="★ Líder" size="small" severity="secondary" outlined onClick={() => toggleLider(u.id, true, setor)} />
                          <Button icon={<X size={10} />} size="small" severity="danger" outlined onClick={() => removeFromSetor(u.id)} />
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </Card>
            );
          })}

          {semSetor.length > 0 && (
            <Card className="!p-4">
              <h3 className="text-sm font-bold text-[var(--text-muted)] mb-1">Sem Setor Atribuído</h3>
              <div className="text-xs text-[var(--text-muted)] mb-3">{semSetor.length} usuário(s) aguardando alocação</div>
              <div className="max-h-56 overflow-y-auto flex flex-col gap-1.5">
                {semSetor.map((u) => (
                  <div key={u.id} className="flex items-center gap-2 px-2 py-1.5 rounded">
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-medium truncate">{u.full_name}</div>
                      <div className="text-[10px] text-[var(--text-muted)] truncate">{u.email}</div>
                    </div>
                    <Dropdown
                      options={[{ label: 'Alocar...', value: '' }, ...setores.map((s) => ({ label: s, value: s }))]}
                      onChange={(e) => quickAssign(u.id, e.value)}
                      placeholder="Alocar..."
                      className="text-xs"
                    />
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>

        <Dialog header={editIdx === null ? 'Novo Setor' : 'Renomear Setor'} visible={setorOpen} onHide={() => setSetorOpen(false)} style={{ width: 400 }} modal>
          <div className="space-y-3">
            <InputText value={editName} onChange={(e) => setEditName(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && saveSetor()} placeholder="Nome do setor" className="w-full" autoFocus />
            {setorError && <div className="px-3 py-2 bg-red-500/10 border border-red-500/30 text-red-400 rounded text-sm">{setorError}</div>}
            <div className="flex justify-end gap-2 pt-2">
              <Button label="Cancelar" severity="secondary" outlined onClick={() => setSetorOpen(false)} />
              <Button label="Salvar" onClick={saveSetor} />
            </div>
          </div>
        </Dialog>

        <Dialog header={`Adicionar ao Setor: ${addToSetor || ''}`} visible={!!addToSetor} onHide={() => setAddToSetor(null)} style={{ width: 480 }} modal>
          <div className="space-y-2 max-h-[60vh] overflow-y-auto">
            {!eligibleForAddToSetor.length ? (
              <div className="text-center text-[var(--text-muted)] py-6 text-sm">Todos os usuários já estão neste setor.</div>
            ) : eligibleForAddToSetor.map((u) => (
              <div key={u.id} className="flex items-center gap-2 px-3 py-2 border-b border-[var(--border)]">
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-medium">{u.full_name}</div>
                  <div className="text-[10px] text-[var(--text-muted)]">
                    {u.email} · {u.department ? `Setor atual: ${u.department}` : 'Sem setor'}
                  </div>
                </div>
                <Button label="Membro" size="small" severity="secondary" outlined onClick={() => addToSelectedSetor(u.id, false)} />
                <Button label="★ Líder" size="small" severity="warning" onClick={() => addToSelectedSetor(u.id, true)} />
              </div>
            ))}
          </div>
        </Dialog>
      </div>
    </div>
  );
}

export default withAuth(AdminSetoresPage, isMasterAdminRole);
