'use client';

import { useEffect, useState } from 'react';
import { CircleCheck } from 'lucide-react';
import { Card } from '../../../components/Card';
import { UnavailList } from '../../../components/UnavailList';
import { UnavailForm } from '../../../components/UnavailForm';
import { Skeleton } from '../../../components/Skeleton';
import { API } from '../../../lib/api-client';
import type { UnavailabilityRecord, Evento } from '../../../lib/types';
import { useAuth, useToast } from '../../../providers';
import { OverviewContent } from './OverviewContent';
import { PendingContent } from './PendingContent';
import { AllHistoryContent } from './AllHistoryContent';

type TabData =
  | { data: UnavailabilityRecord[]; truncated: boolean; eventos?: Evento[] }
  | UnavailabilityRecord[]
  | null;

interface Props {
  tabKey: string;
  reloadKey: number;
  onReload: () => void;
  onEdit: (item: UnavailabilityRecord) => void;
  onDelete: (id: number) => void;
}

export function TabContent({ tabKey, reloadKey, onReload, onEdit, onDelete }: Props) {
  const { user } = useAuth();
  const toast = useToast();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<TabData>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        let res: TabData = null;
        if (tabKey === 'overview') {
          const [unav, ev] = await Promise.all([API.getUnavailability(), API.getEventosPublic().catch(() => ({ eventos: [] as Evento[] }))]);
          res = { ...unav, eventos: ev.eventos };
        }
        else if (tabKey === 'all') res = await API.getUnavailability();
        else if (tabKey === 'pending') res = await API.getPendingUnavailability();
        else if (tabKey === 'active') res = await API.getActiveUnavailability();
        else if (tabKey === 'mine') res = await API.getMyUnavailability();
        if (!cancelled) setData(res);
      } catch (e: unknown) {
        if (!cancelled) toast.show(e instanceof Error ? e.message : 'Erro', 'error');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    if (tabKey !== 'form') load();
    return () => { cancelled = true; };
  }, [tabKey, reloadKey, toast]);

  async function approve(id: number) {
    try { await API.approveUnavailability(id); toast.show('Solicitação aprovada!'); onReload(); }
    catch (e: unknown) { toast.show(e instanceof Error ? e.message : 'Erro', 'error'); }
  }
  async function reject(id: number) {
    try { await API.rejectUnavailability(id); toast.show('Solicitação rejeitada.'); onReload(); }
    catch (e: unknown) { toast.show(e instanceof Error ? e.message : 'Erro', 'error'); }
  }

  if (tabKey === 'form') return <UnavailForm user={user!} onSubmitted={onReload} />;
  if (loading) return <Skeleton rows={4} />;

  if (tabKey === 'overview') {
    const obj = data && !Array.isArray(data) ? data : { data: [], truncated: false };
    return <OverviewContent all={obj.data} eventos={obj.eventos || []} onApprove={approve} onReject={reject} />;
  }

  if (tabKey === 'pending') {
    const list = Array.isArray(data) ? data : [];
    return (
      <PendingContent
        items={list}
        onApprove={approve}
        onReject={reject}
        onEdit={onEdit}
        onDelete={onDelete}
        onReload={onReload}
      />
    );
  }

  if (tabKey === 'active') {
    const list = Array.isArray(data) ? data : [];
    if (!list.length) return <Card className="text-center text-[var(--text-muted)] py-10">Nenhuma indisponibilidade ativa.</Card>;
    return (
      <>
        <div className="mb-4">
          <h3 className="text-base font-semibold flex items-center gap-2">
            <CircleCheck size={16} className="text-emerald-400" /> Pessoas Indisponíveis Agora
          </h3>
          <p className="text-xs text-[var(--text-muted)]">Períodos aprovados que incluem a data de hoje</p>
        </div>
        <UnavailList items={list} showUser currentUser={user!} onEdit={onEdit} onDelete={onDelete} onChanged={onReload} />
      </>
    );
  }

  if (tabKey === 'mine') {
    const list = Array.isArray(data) ? data : [];
    if (!list.length) return <Card className="text-center text-[var(--text-muted)] py-10">Você não tem solicitações.</Card>;
    return <UnavailList items={list} currentUser={user!} onEdit={onEdit} onDelete={onDelete} onChanged={onReload} />;
  }

  if (tabKey === 'all') {
    const obj = data && !Array.isArray(data) ? data : { data: [], truncated: false };
    return <AllHistoryContent all={obj.data} truncated={obj.truncated} onEdit={onEdit} onDelete={onDelete} onChanged={onReload} />;
  }

  return null;
}
