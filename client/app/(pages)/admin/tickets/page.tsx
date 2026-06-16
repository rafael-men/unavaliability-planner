'use client';

import { useState } from 'react';
import { Button } from 'primereact/button';
import { Password } from 'primereact/password';
import { Dialog } from 'primereact/dialog';
import { ConfirmDialog } from 'primereact/confirmdialog';
import { Ticket as TicketIcon, ChevronLeft, KeyRound, Clock, CheckCircle2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Navbar } from '../../../components/Navbar';
import { PageHeader } from '../../../components/PageHeader';
import { EmptyState } from '../../../components/EmptyState';
import { Card } from '../../../components/Card';
import { withAuth } from '../../../components/withAuth';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import { isEditorRole } from '../../../lib/client-config';
import { usePageData } from '../../../lib/hooks/usePageData';
import { useToast } from '../../../providers';

interface Ticket {
  id: number;
  email: string;
  status: 'open' | 'resolved';
  resolvedBy?: number | null;
  resolvedAt?: string | null;
  createdAt?: string | null;
}

const fetchTickets = () => API.getTickets(false) as Promise<Ticket[]>;

function AdminTicketsPage() {
  const router = useRouter();
  const toast = useToast();
  const { data: tickets, loading, reload } = usePageData<Ticket[]>(fetchTickets, []);
  const [target, setTarget] = useState<Ticket | null>(null);
  const [password, setPassword] = useState('');
  const [saving, setSaving] = useState(false);

  function openResolve(t: Ticket) {
    setTarget(t);
    setPassword('');
  }

  async function resolve() {
    if (!target) return;
    if (password.length < 6) {
      toast.show('A senha deve ter ao menos 6 caracteres.', 'error');
      return;
    }
    setSaving(true);
    try {
      await API.resolveTicket(target.id, password);
      toast.show('Senha redefinida e enviada por e-mail ao usuário.');
      setTarget(null);
      reload();
    } catch (e: any) {
      toast.show(e.message, 'error');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="min-h-screen"><Navbar /><div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8"><Skeleton rows={4} /></div></div>;
  }

  const open = tickets.filter((t) => t.status === 'open');
  const resolved = tickets.filter((t) => t.status === 'resolved');

  return (
    <div className="min-h-screen">
      <Navbar />
      <ConfirmDialog />
      <div className="max-w-[1440px] mx-auto px-4 sm:px-9 py-8">
        <PageHeader
          title="Tickets de Senha"
          icon={TicketIcon}
          description={`${open.length} aberto(s) · ${resolved.length} resolvido(s)`}
          actions={<Button label="Voltar" icon={<ChevronLeft size={14} />} severity="secondary" outlined size="small" onClick={() => router.push('/unavailability')} />}
        />

        {tickets.length === 0 ? (
          <EmptyState icon={TicketIcon} title="Nenhum ticket" description="Solicitações de redefinição de senha aparecerão aqui." />
        ) : (
          <div className="flex flex-col gap-3">
            {open.map((t) => (
              <Card key={t.id} className="flex items-center justify-between gap-3 flex-wrap border-yellow-500/30">
                <div>
                  <div className="text-sm font-semibold flex items-center gap-2">
                    <Clock size={15} className="text-yellow-400" /> {t.email}
                  </div>
                  <div className="text-xs text-[var(--text-muted)] mt-0.5">Aberto {fmt(t.createdAt)}</div>
                </div>
                <Button label="Definir nova senha" icon={<KeyRound size={14} />} size="small" onClick={() => openResolve(t)} />
              </Card>
            ))}

            {resolved.length > 0 && (
              <>
                <div className="text-[11px] uppercase tracking-wider text-[var(--text-muted)] font-semibold mt-3">Resolvidos</div>
                {resolved.map((t) => (
                  <Card key={t.id} className="flex items-center justify-between gap-3 flex-wrap opacity-70">
                    <div className="text-sm flex items-center gap-2">
                      <CheckCircle2 size={15} className="text-emerald-400" /> {t.email}
                    </div>
                    <div className="text-xs text-[var(--text-muted)]">Resolvido {fmt(t.resolvedAt)}</div>
                  </Card>
                ))}
              </>
            )}
          </div>
        )}

        <Dialog
          header="Definir nova senha"
          visible={!!target}
          onHide={() => setTarget(null)}
          style={{ width: 'min(92vw, 440px)' }}
          modal
        >
          {target && (
            <div className="space-y-3">
              <p className="text-sm text-[var(--text-muted)]">
                Usuário: <strong className="text-foreground">{target.email}</strong>
              </p>
              <div>
                <label className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-1.5 block">Nova senha</label>
                <Password value={password} onChange={(e) => setPassword(e.target.value)} feedback={false} toggleMask inputClassName="w-full" className="w-full" />
                <p className="text-[11px] text-[var(--text-muted)] mt-1">Mínimo 6 caracteres, com letra e número. Será enviada por e-mail ao usuário.</p>
              </div>
              <div className="flex justify-end gap-2 pt-1">
                <Button label="Cancelar" severity="secondary" outlined onClick={() => setTarget(null)} />
                <Button label="Redefinir e enviar" loading={saving} onClick={resolve} />
              </div>
            </div>
          )}
        </Dialog>
      </div>
    </div>
  );
}

function fmt(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  return isNaN(d.getTime()) ? '' : d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export default withAuth(AdminTicketsPage, isEditorRole);
