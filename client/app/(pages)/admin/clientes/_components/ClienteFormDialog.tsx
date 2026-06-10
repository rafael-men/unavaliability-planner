'use client';

import { useEffect, useState } from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Dialog } from 'primereact/dialog';
import { API } from '../../../../lib/api-client';
import { useToast } from '../../../../providers';
import type { Cliente } from './types';

interface Props {
  visible: boolean;
  editing: Cliente | null;
  onHide: () => void;
  onSaved: () => void;
}


export function ClienteFormDialog({ visible, editing, onHide, onSaved }: Props) {
  const toast = useToast();
  const [form, setForm] = useState({ nome: '', descricao: '', ativo: true });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (visible) {
      setForm({
        nome: editing?.nome || '',
        descricao: editing?.descricao || '',
        ativo: editing ? editing.ativo : true,
      });
      setError(null);
    }
  }, [visible, editing]);

  async function save() {
    if (!form.nome.trim()) {
      setError('Digite um nome para o cliente.');
      return;
    }
    const payload = { nome: form.nome.trim(), descricao: form.descricao.trim() || null, ativo: form.ativo };
    try {
      if (!editing) {
        await API.createCliente(payload);
        toast.show('Cliente criado!');
      } else {
        await API.updateCliente(editing.id, payload);
        toast.show('Cliente atualizado!');
      }
      onHide();
      onSaved();
    } catch (e: any) {
      setError(e.message);
    }
  }

  return (
    <Dialog header={editing ? 'Editar Cliente' : 'Novo Cliente'} visible={visible} onHide={onHide} style={{ width: 480 }} modal>
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
          <input type="checkbox" id="ativo" checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} className="w-4 h-4" />
          <label htmlFor="ativo" className="text-xs">Cliente ativo</label>
        </div>
        {error && <div className="px-3 py-2 bg-red-500/10 border border-red-500/30 text-red-400 rounded text-sm">{error}</div>}
        <div className="flex justify-end gap-2 pt-2">
          <Button label="Cancelar" severity="secondary" outlined onClick={onHide} />
          <Button label="Salvar" onClick={save} />
        </div>
      </div>
    </Dialog>
  );
}
