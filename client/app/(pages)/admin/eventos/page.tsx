'use client';

import { useEffect, useState, useCallback } from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Calendar as PrimeCalendar } from 'primereact/calendar';
import { Dialog } from 'primereact/dialog';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { CalendarRange, Plus, ChevronLeft, Pencil, X, Briefcase } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { withAuth } from '../../../components/withAuth';
import { Card } from '../../../components/Card';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { isEditorRole, formatDate } from '../../../lib/client-config';
import { useToast } from '../../../providers';

interface Cliente {
  id: number;
  nome: string;
  ativo: boolean;
}

interface Evento {
  id: number;
  nome: string;
  descricao: string | null;
  data_inicio: string;
  data_fim: string;
  cliente_ids?: number[];
  clientes?: { id: number; nome: string }[];
}

interface FormState {
  nome: string;
  descricao: string;
  data_inicio: Date | null;
  data_fim: Date | null;
  cliente_ids: Set<number>;
}

const EMPTY_FORM: FormState = {
  nome: '',
  descricao: '',
  data_inicio: null,
  data_fim: null,
  cliente_ids: new Set(),
};

function toIsoDate(d: Date | null): string {
  if (!d) return '';
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function fromIsoDate(s: string | null): Date | null {
  if (!s) return null;
  const [y, m, d] = s.split('-').map(Number);
  if (!y) return null;
  return new Date(y, m - 1, d);
}

function AdminEventosPage() {
  const router = useRouter();
  const toast = useToast();
  const [eventos, setEventos] = useState<Evento[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Evento | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      API.clearCache();
      const [evRes, clRes] = await Promise.all([API.getEventos(), API.getClientes()]);
      setEventos(evRes.eventos);
      setClientes(clRes.clientes.filter((c: Cliente) => c.ativo));
    } catch (e: any) {
      toast.show(e.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { load(); }, [load]);

  function openModal(e: Evento | null) {
    setEditing(e);
    setForm(e ? {
      nome: e.nome,
      descricao: e.descricao || '',
      data_inicio: fromIsoDate(e.data_inicio),
      data_fim: fromIsoDate(e.data_fim),
      cliente_ids: new Set(e.cliente_ids || []),
    } : EMPTY_FORM);
    setError(null);
    setModalOpen(true);
  }

  function toggleCliente(id: number) {
    setForm((prev) => {
      const next = new Set(prev.cliente_ids);
      if (next.has(id)) next.delete(id); else next.add(id);
      return { ...prev, cliente_ids: next };
    });
  }

  async function save() {
    setError(null);
    if (!form.nome.trim()) { setError('Nome obrigatório.'); return; }
    if (!form.data_inicio || !form.data_fim) { setError('Datas obrigatórias.'); return; }
    if (form.data_fim < form.data_inicio) { setError('Data de fim deve ser ≥ data de início.'); return; }
    if (form.cliente_ids.size === 0) {
      setError('Selecione ao menos um cliente.');
      return;
    }
    const payload = {
      nome: form.nome.trim(),
      descricao: form.descricao.trim() || null,
      data_inicio: toIsoDate(form.data_inicio),
      data_fim: toIsoDate(form.data_fim),
      cliente_ids: Array.from(form.cliente_ids),
    };
    try {
      if (!editing) {
        await API.createEvento(payload);
        toast.show('Evento criado!');
      } else {
        await API.updateEvento(editing.id, payload);
        toast.show('Evento atualizado!');
      }
      setModalOpen(false);
      load();
    } catch (e: any) {
      setError(e.message);
    }
  }

  function deleteEvento(e: Evento) {
    confirmDialog({
      message: `Excluir o evento "${e.nome}"?`,
      header: 'Confirmar exclusão',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try { await API.deleteEvento(e.id); toast.show('Evento excluído.'); load(); }
        catch (err: any) { toast.show(err.message, 'error'); }
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
              <CalendarRange size={28} className="text-[var(--accent)]" /> Eventos
            </h1>
            <p className="text-[var(--text-muted)] text-sm mt-1">{eventos.length} evento(s) cadastrado(s) · {clientes.length} cliente(s) ativo(s)</p>
          </div>
          <div className="flex gap-2">
            <Button label="Novo Evento" icon={<Plus size={14} />} onClick={() => openModal(null)} size="small" disabled={clientes.length === 0} />
            <Button label="Voltar" icon={<ChevronLeft size={14} />} severity="secondary" outlined size="small" onClick={() => router.push('/unavailability')} />
          </div>
        </div>

        {clientes.length === 0 && (
          <Card className="!p-4 mb-4 border-yellow-500/30">
            <div className="flex items-center gap-2 text-yellow-400 text-sm">
              <Briefcase size={16} />
              <span>Cadastre ao menos um cliente em <a href="/admin/clientes" className="underline">/admin/clientes</a> antes de criar eventos.</span>
            </div>
          </Card>
        )}

        {eventos.length === 0 ? (
          <Card className="!p-8 text-center">
            <CalendarRange size={40} className="mx-auto text-[var(--text-muted)] mb-3" />
            <h3 className="text-base font-semibold mb-1">Nenhum evento cadastrado</h3>
            <p className="text-xs text-[var(--text-muted)] mb-4">Crie um evento para começar a detectar conflitos com indisponibilidades.</p>
            {clientes.length > 0 && (
              <Button label="Criar primeiro evento" icon={<Plus size={14} />} onClick={() => openModal(null)} size="small" />
            )}
          </Card>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {eventos.map((e) => (
              <Card key={e.id} className="!p-4">
                <div className="flex justify-between items-start mb-2 gap-2">
                  <div className="min-w-0 flex-1">
                    <h3 className="text-sm font-bold truncate">{e.nome}</h3>
                    <div className="text-xs text-[var(--text-muted)] mt-0.5">
                      {formatDate(e.data_inicio)} → {formatDate(e.data_fim)}
                    </div>
                  </div>
                  <div className="flex gap-1 flex-shrink-0">
                    <Button icon={<Pencil size={12} />} size="small" severity="secondary" outlined onClick={() => openModal(e)} tooltip="Editar" />
                    <Button icon={<X size={12} />} size="small" severity="danger" outlined onClick={() => deleteEvento(e)} tooltip="Excluir" />
                  </div>
                </div>

                {e.descricao && (
                  <div className="text-xs text-[var(--text-muted)] mb-2 line-clamp-3">{e.descricao}</div>
                )}

                <div className="flex flex-wrap gap-1 mt-2">
                  {!e.clientes || e.clientes.length === 0 ? (
                    <span className="text-[10px] text-yellow-400">Sem clientes vinculados</span>
                  ) : e.clientes.map((c) => (
                    <span key={c.id} className="text-[10px] px-2 py-0.5 rounded bg-[var(--accent)]/15 text-[var(--accent)] border border-[var(--accent)]/30 flex items-center gap-1">
                      <Briefcase size={9} /> {c.nome}
                    </span>
                  ))}
                </div>
              </Card>
            ))}
          </div>
        )}

        <Dialog
          header={editing ? 'Editar Evento' : 'Novo Evento'}
          visible={modalOpen}
          onHide={() => setModalOpen(false)}
          style={{ width: 560 }}
          modal
        >
          <div className="space-y-3">
            <div>
              <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Nome do evento</label>
              <InputText value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} placeholder="Ex.: Campanha Black Friday" className="w-full" autoFocus />
            </div>

            <div>
              <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Descrição (opcional)</label>
              <InputTextarea value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} rows={2} className="w-full" />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Data de início</label>
                <PrimeCalendar value={form.data_inicio} onChange={(e) => setForm({ ...form, data_inicio: e.value as Date })} dateFormat="dd/mm/yy" showIcon className="w-full" />
              </div>
              <div>
                <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Data de fim</label>
                <PrimeCalendar value={form.data_fim} onChange={(e) => setForm({ ...form, data_fim: e.value as Date })} dateFormat="dd/mm/yy" showIcon minDate={form.data_inicio || undefined} className="w-full" />
              </div>
            </div>

            <div>
              <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">
                Clientes envolvidos <span className="text-[10px] normal-case font-normal text-[var(--text-muted)]">({form.cliente_ids.size} selecionado(s))</span>
              </label>
              {clientes.length === 0 ? (
                <div className="px-3 py-2 bg-yellow-500/10 border border-yellow-500/30 text-yellow-400 rounded text-xs">
                  Nenhum cliente cadastrado. Vá em /admin/clientes para criar.
                </div>
              ) : (
                <div className="flex flex-col gap-1 max-h-56 overflow-y-auto px-3 py-2 bg-[var(--surface)] rounded-lg border border-[var(--border)]">
                  {clientes.map((c) => (
                    <label key={c.id} className="flex items-center gap-2 text-sm cursor-pointer py-1 hover:bg-[var(--surface2)] rounded px-1">
                      <input
                        type="checkbox"
                        checked={form.cliente_ids.has(c.id)}
                        onChange={() => toggleCliente(c.id)}
                        className="w-4 h-4"
                      />
                      <Briefcase size={12} className="text-[var(--text-muted)]" />
                      {c.nome}
                      <span className="text-[10px] text-[var(--text-muted)] ml-auto">({form.cliente_ids.has(c.id) ? '1' : '0'})</span>
                    </label>
                  ))}
                </div>
              )}
            </div>

            {error && <div className="px-3 py-2 bg-red-500/10 border border-red-500/30 text-red-400 rounded text-sm">{error}</div>}
            <div className="flex justify-end gap-2 pt-2">
              <Button label="Cancelar" severity="secondary" outlined onClick={() => setModalOpen(false)} />
              <Button label="Salvar" onClick={save} />
            </div>
          </div>
        </Dialog>
      </div>
    </div>
  );
}

export default withAuth(AdminEventosPage, isEditorRole);
