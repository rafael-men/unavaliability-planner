import { NextResponse } from 'next/server';
import { queries } from '../../lib/database';
import { requireAuth, isAdmin, isAdminEditor, isLider } from '../../lib/auth';
import { reportToMatchesLider } from '../../lib/unavailability-helpers';

interface NotificationItem {
  id: string;
  type: 'pending_user' | 'pending_unavailability';
  title: string;
  subtitle: string;
  created_at: string | null;
  href: string;
}

export async function GET() {
  const { user, response } = await requireAuth();
  if (response) return response;

  const items: NotificationItem[] = [];

  if (isAdminEditor(user!.role)) {
    const pendingUsers = await queries.getPendingUsers('pending');
    pendingUsers.forEach((u: any) => {
      items.push({
        id: `user-${u.id}`,
        type: 'pending_user',
        title: u.full_name || u.nome || u.email,
        subtitle: `Novo cadastro · ${u.email}`,
        created_at: u.created_at || null,
        href: '/admin/users',
      });
    });
  }

  if (isAdmin(user!.role) || isLider(user!.role) || user!.role === 'socio') {
    const pendingUnavail = await queries.getPendingUnavailability();
    let visible = pendingUnavail;

    if (isLider(user!.role) && !isAdmin(user!.role)) {
      const userIds = [...new Set(pendingUnavail.map((r: any) => r.user_id))] as number[];
      const batchUsers = userIds.length ? await queries.getUsersByIds(userIds) : [];
      const userById: Record<number, any> = Object.fromEntries(batchUsers.map((u: any) => [u.id, u]));

      const memberIds = batchUsers.filter((u: any) => u.member_id).map((u: any) => u.member_id);
      const emails = batchUsers.filter((u: any) => !u.member_id && u.email).map((u: any) => u.email);
      const [membersByIds, membersByEmails] = await Promise.all([
        memberIds.length ? queries.getMembersByIds(memberIds) : Promise.resolve([]),
        emails.length ? queries.getMembersByEmails(emails) : Promise.resolve([]),
      ]);
      const memberByMemberId: Record<number, any> = Object.fromEntries(membersByIds.map((m: any) => [m.id, m]));
      const memberByEmail: Record<string, any> = Object.fromEntries(
        [...membersByIds, ...membersByEmails].filter((m: any) => m.email).map((m: any) => [m.email.toLowerCase(), m])
      );

      const liderEmail = user!.email || '';
      const liderMember: any = liderEmail ? await queries.getMemberByEmail(liderEmail.toLowerCase()) : null;
      const liderName = liderMember?.name || null;

      visible = pendingUnavail.filter((r: any) => {
        if (r.user_id === user!.id) return false;
        const u = userById[r.user_id];
        if (!u) return false;
        if (user!.department && u.department === user!.department) return true;
        const member = (u.member_id ? memberByMemberId[u.member_id] : null)
          ?? (u.email ? memberByEmail[u.email.toLowerCase()] : null);
        return member ? reportToMatchesLider(member.report_to, liderEmail, liderName) : false;
      });
    }

    visible.forEach((r: any) => {
      items.push({
        id: `unavail-${r.id}`,
        type: 'pending_unavailability',
        title: r.user_name || r.full_name || 'Solicitação',
        subtitle: `Indisponibilidade · ${r.start_date} a ${r.end_date}`,
        created_at: r.created_at || null,
        href: '/unavailability',
      });
    });
  }

  items.sort((a, b) => {
    if (!a.created_at) return 1;
    if (!b.created_at) return -1;
    return b.created_at.localeCompare(a.created_at);
  });

  return NextResponse.json({ items, count: items.length });
}
