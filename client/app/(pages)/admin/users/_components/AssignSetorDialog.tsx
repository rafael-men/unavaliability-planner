'use client';

import { useEffect, useState } from 'react';
import { Button } from 'primereact/button';
import { Dropdown } from 'primereact/dropdown';
import { Dialog } from 'primereact/dialog';
import { Checkbox } from 'primereact/checkbox';
import { API } from '../../../../lib/api-client';
import { useToast } from '../../../../providers';

interface Props {
  visible: boolean;
  onHide: () => void;
  userId: number | null;
  users: any[];
  setores: string[];
  onSaved: () => void;
}

/** Diálogo para atribuir setor (e papel de líder) a um usuário. */
export function AssignSetorDialog({ visible, onHide, userId, users, setores, onSaved }: Props) {
  const toast = useToast();
  const target = users.find((u) => u.id === userId);
  const [setor, setSetor] = useState<string | null>(target?.department || null);
  const [isLider, setIsLider] = useState(target?.role === 'lider');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (visible && target) {
      setSetor(target.department || null);
      setIsLider(target.role === 'lider');
    }
  }, [visible, target]);

  async function save() {
    if (!userId) return;
    setSaving(true);
    try {
      await API.assignUserSetor(userId, { setor: setor || null, is_lider: isLider });
      toast.show('Setor atualizado!');
      onHide();
      onSaved();
    } catch (e: any) {
      toast.show(e.message, 'error');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog header="Atribuir Setor" visible={visible} onHide={onHide} style={{ width: 420 }} modal>
      <div className="space-y-3">
        {target && (
          <p className="text-sm text-[var(--text-muted)]">{target.full_name} ({target.email})</p>
        )}
        <div>
          <label className="text-xs text-[var(--text-muted)] uppercase tracking-wider mb-1 block">Setor</label>
          <Dropdown value={setor} options={[{ label: 'Sem setor', value: null }, ...setores.map((s) => ({ label: s, value: s }))]} onChange={(e) => setSetor(e.value)} className="w-full" />
        </div>
        <div className="flex items-center gap-2">
          <Checkbox inputId="isLider" checked={isLider} onChange={(e) => setIsLider(e.checked || false)} />
          <label htmlFor="isLider" className="text-sm cursor-pointer">É líder deste setor?</label>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Button label="Cancelar" severity="secondary" outlined onClick={onHide} />
          <Button label="Salvar" loading={saving} onClick={save} />
        </div>
      </div>
    </Dialog>
  );
}
