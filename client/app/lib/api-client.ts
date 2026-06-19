import type { UnavailabilityRecord, Member, User, Evento, Cliente, NotificationItem } from './types';

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';


const cache: Record<string, unknown> = {};
const cacheTs: Record<string, number> = {};
const CACHE_TTL = 15_000;

function invalidateCache(url: string) {
  const keys = Object.keys(cache);
  const isUnavail = url.includes('unavailability');
  for (const k of keys) {
    if (isUnavail ? (k.includes('unavailability') || k.includes('dashboard')) : true) {
      delete cache[k];
      delete cacheTs[k];
    }
  }
}

export function clearCache() {
  for (const k of Object.keys(cache)) { delete cache[k]; delete cacheTs[k]; }
}

async function request<T>(method: Method, url: string, body: unknown = null, retries = 2): Promise<T> {
  if (method === 'GET' && cache[url] && (Date.now() - (cacheTs[url] ?? 0)) < CACHE_TTL) {
    return JSON.parse(JSON.stringify(cache[url])) as T;
  }

  const opts: RequestInit = {
    method,
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  };
  if (body) opts.body = JSON.stringify(body);

  try {
    const res = await fetch(url, opts);
    const ct = res.headers.get('content-type') ?? '';
    if (!ct.includes('application/json')) {
      throw new Error(res.ok ? 'Resposta inválida do servidor' : `Erro ${res.status} do servidor`);
    }
    const data = await res.json();
    if (res.status === 401) {
      clearCache();
      if (
        typeof window !== 'undefined' &&
        !window.location.pathname.startsWith('/login') &&
        !window.location.pathname.startsWith('/register')
      ) {
        window.location.href = '/login';
      }
      throw new Error(data.error ?? 'Sessão expirada. Faça login novamente.');
    }
    if (!res.ok) throw new Error(data.error ?? 'Erro desconhecido');
    if (method === 'GET') {
      cache[url] = JSON.parse(JSON.stringify(data));
      cacheTs[url] = Date.now();
    } else {
      invalidateCache(url);
    }
    return data as T;
  } catch (e: unknown) {
    if (retries > 0 && e instanceof TypeError) {
      const delay = (3 - retries) * 800 + 300;
      await new Promise((r) => setTimeout(r, delay));
      return request<T>(method, url, body, retries - 1);
    }
    if (e instanceof TypeError) {
      throw new Error('Sem conexão com o servidor. Verifique sua internet e tente novamente.');
    }
    throw e;
  }
}

export const API = {
  clearCache,

  login: async (email: string, password: string) => {
    clearCache();
    return request<{ user: User }>('POST', '/api/auth/login', { email, password });
  },
  register: (data: { email: string; full_name: string; password: string; department?: string }) =>
    request<void>('POST', '/api/auth/register', data),
  logout: async () => {
    clearCache();
    try { await request<void>('POST', '/api/auth/logout'); } catch {}
  },
  me: () => request<{ user: User }>('GET', '/api/auth/me'),
  forgotPassword: (email: string) => request<void>('POST', '/api/auth/forgot-password', { email }),

  getTickets: (onlyOpen = false) => request<{ id: number; email: string; status: 'open' | 'resolved'; resolvedBy?: number | null; resolvedAt?: string | null; createdAt?: string | null }[]>('GET', `/api/admin/tickets?onlyOpen=${onlyOpen}`),
  resolveTicket: (id: number, password: string) => request<void>('POST', `/api/admin/tickets/${id}/resolve`, { password }),

  getSetores: () => request<string[]>('GET', '/api/setores'),
  createSetor: (name: string) => request<void>('POST', '/api/admin/setores', { name }),
  updateSetor: (index: number, name: string) => request<void>('PUT', `/api/admin/setores/${index}`, { name }),
  deleteSetor: (index: number) => request<void>('DELETE', `/api/admin/setores/${index}`),
  assignUserSetor: (id: number, data: { setor: string | null; is_lider?: boolean }) => request<void>('POST', `/api/admin/users/${id}/assign-setor`, data),

  getUsers: () => request<User[]>('GET', '/api/admin/users'),
  getPending: () => request<User[]>('GET', '/api/admin/pending'),
  approveUser: (id: number) => request<void>('POST', `/api/admin/approve/${id}`),
  rejectUser: (id: number) => request<void>('POST', `/api/admin/reject/${id}`),
  deleteUser: (id: number) => request<void>('DELETE', `/api/admin/users/${id}`),
  changeUserRole: (id: number, role: string) => request<void>('POST', `/api/admin/change-role/${id}`, { role }),
  createUserDirect: (data: Partial<User> & { password: string }) => request<User>('POST', '/api/admin/users/create', data),

  getDashboard: () => request<{ kpis: Record<string, number> }>('GET', '/api/dashboard'),

  getEventosPublic: () => request<{ eventos: Evento[] }>('GET', '/api/eventos'),

  getNotifications: () => request<{ items: NotificationItem[]; count: number }>('GET', '/api/notifications'),

  getClientes: () => request<{ clientes: Cliente[]; links: { user_id: number; cliente_id: number; ativo: boolean }[] }>('GET', '/api/admin/clientes'),
  createCliente: (data: { nome: string; descricao?: string | null; ativo?: boolean }) =>
    request<Cliente>('POST', '/api/admin/clientes', data),
  updateCliente: (id: number, data: { nome?: string; descricao?: string | null; ativo?: boolean }) =>
    request<Cliente>('PUT', `/api/admin/clientes/${id}`, data),
  deleteCliente: (id: number) => request<void>('DELETE', `/api/admin/clientes/${id}`),
  assignUserToCliente: (clienteId: number, userId: number, ativo: boolean) =>
    request<void>('POST', `/api/admin/clientes/${clienteId}/assign`, { user_id: userId, ativo }),

  getEventos: () => request<{ eventos: Evento[] }>('GET', '/api/admin/eventos'),
  createEvento: (data: Omit<Evento, 'id' | 'clientes'>) => request<Evento>('POST', '/api/admin/eventos', data),
  updateEvento: (id: number, data: Partial<Omit<Evento, 'id'>>) => request<Evento>('PUT', `/api/admin/eventos/${id}`, data),
  deleteEvento: (id: number) => request<void>('DELETE', `/api/admin/eventos/${id}`),

  getMembers: () => request<Member[]>('GET', '/api/members'),
  getMemberByEmail: (email: string) => request<Member>('GET', `/api/members/by-email/${encodeURIComponent(email)}`),
  getMyMemberInfo: () => request<Member>('GET', '/api/members/me'),
  createMember: (data: Omit<Member, 'id'>) => request<Member>('POST', '/api/admin/members', data),
  updateMember: (id: number, data: Partial<Omit<Member, 'id'>>) => request<Member>('PUT', `/api/admin/members/${id}`, data),
  deleteMember: (id: number) => request<void>('DELETE', `/api/admin/members/${id}`),

  createUnavailability: (data: Pick<UnavailabilityRecord, 'unavailability_type' | 'start_date' | 'end_date'> & { department?: string }) =>
    request<UnavailabilityRecord>('POST', '/api/unavailability', data),
  updateUnavailability: (id: number, data: Partial<Pick<UnavailabilityRecord, 'start_date' | 'end_date' | 'unavailability_type'>>) =>
    request<UnavailabilityRecord>('PATCH', `/api/unavailability/${id}`, data),
  getUnavailability: () => request<{ data: UnavailabilityRecord[]; truncated: boolean }>('GET', '/api/unavailability'),
  getMyUnavailability: () => request<UnavailabilityRecord[]>('GET', '/api/unavailability/mine'),
  getPendingUnavailability: () => request<UnavailabilityRecord[]>('GET', '/api/unavailability/pending'),
  getActiveUnavailability: () => request<UnavailabilityRecord[]>('GET', '/api/unavailability/active'),
  getUnavailabilityImpact: () => request<{ impact: number }>('GET', '/api/unavailability/impact'),
  approveUnavailability: (id: number) => request<void>('POST', `/api/unavailability/${id}/approve`),
  rejectUnavailability: (id: number) => request<void>('POST', `/api/unavailability/${id}/reject`),
  deleteUnavailability: (id: number) => request<void>('DELETE', `/api/unavailability/${id}`),
  cancelUnavailability: (id: number, newEndDate?: string) =>
    request<void>('POST', `/api/unavailability/${id}/cancel`, newEndDate ? { new_end_date: newEndDate } : {}),
  getUnavailabilityHistory: (id: number) => request<{ id: number; action: string; actorName?: string | null; detail?: string | null; createdAt?: string | null }[]>('GET', `/api/unavailability/${id}/history`),
};
