'use client';

import { useEffect, useState } from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Dropdown } from 'primereact/dropdown';
import { Dialog } from 'primereact/dialog';
import { API } from '../../../../lib/api-client';
import { ROLE_LABELS, CREATABLE_ROLES } from '../../../../lib/client-config';
import { useToast } from '../../../../providers';

interface Props {
  visible: boolean;
  onHide: () => void;
  onSaved: () => void;
  setores: string[];
}


export function CreateUserDialog({ visible, onHide, onSaved, setores }: Props) {
  const toast = useToast();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [dept, setDept] = useState<string | null>(null);
  const [role, setRole] = useState('colaborador');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (visible) {
      setEmail(''); setName(''); setPassword(''); setDept(null); setRole('colaborador'); setError(null);
    }
  }, [visible]);

  async function save() {
    setError(null);
    if (!email || !name || !password || !dept || !role) {
      setError('Preencha todos os campos.');
      return;
    }
    setSaving(true);
    try {
      await API.createUserDirect({ email, full_name: name, password, department: dept, role });
      toast.show('Usuário criado e aprovado!');
      onHide();
      onSaved();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog header="Criar Usuário" visible={visible} onHide={onHide} style={{ width: 480 }} modal>
      <div className="space-y-3">
        <div>
          <label className="text-xs text-[var(--text-muted)] uppercase tracking-wider mb-1 block">Email *</label>
          <InputText value={email} onChange={(e) => setEmail(e.target.value)} type="email" placeholder="email@macfor.com.br" className="w-full" />
        </div>
        <div>
          <label className="text-xs text-[var(--text-muted)] uppercase tracking-wider mb-1 block">Nome completo *</label>
          <InputText value={name} onChange={(e) => setName(e.target.value)} className="w-full" />
        </div>
        <div>
          <label className="text-xs text-[var(--text-muted)] uppercase tracking-wider mb-1 block">Senha inicial *</label>
          <Password value={password} onChange={(e) => setPassword(e.target.value)} feedback={false} toggleMask inputClassName="w-full" className="w-full" />
        </div>
        <div>
          <label className="text-xs text-[var(--text-muted)] uppercase tracking-wider mb-1 block">Departamento *</label>
          <Dropdown value={dept} options={setores.map((s) => ({ label: s, value: s }))} onChange={(e) => setDept(e.value)} className="w-full" />
        </div>
        <div>
          <label className="text-xs text-[var(--text-muted)] uppercase tracking-wider mb-1 block">Role *</label>
          <Dropdown
            value={role}
            options={CREATABLE_ROLES.map((r) => ({ label: ROLE_LABELS[r] || r, value: r }))}
            onChange={(e) => setRole(e.value)}
            className="w-full"
          />
        </div>
        {error && <div className="px-3 py-2 bg-red-500/10 border border-red-500/30 text-red-400 rounded text-sm">{error}</div>}
        <div className="flex justify-end gap-2 pt-2">
          <Button label="Cancelar" severity="secondary" outlined onClick={onHide} />
          <Button label="Criar e Aprovar" loading={saving} onClick={save} />
        </div>
      </div>
    </Dialog>
  );
}
