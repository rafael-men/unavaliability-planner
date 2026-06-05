type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';


const TOKEN_KEY = 'auth_token';
const COOKIE_MAX_AGE = 7 * 24 * 60 * 60; 

function writeCookie(token: string | null) {
  if (typeof document === 'undefined') return;
  const secure = window.location.protocol === 'https:' ? '; Secure' : '';
  if (token) {
    document.cookie = `${TOKEN_KEY}=${encodeURIComponent(token)}; Path=/; Max-Age=${COOKIE_MAX_AGE}; SameSite=Strict${secure}`;
  } else {
    document.cookie = `${TOKEN_KEY}=; Path=/; Max-Age=0; SameSite=Strict${secure}`;
  }
}

export function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (typeof window === 'undefined') return;
  if (token) window.localStorage.setItem(TOKEN_KEY, token);
  else window.localStorage.removeItem(TOKEN_KEY);
  writeCookie(token);
}

// Chaves de estado por-usuário em localStorage que devem ser limpas no logout
// para não vazar entre contas no mesmo navegador.
const PER_USER_KEYS = ['notif:seen-ids'];

export function clearToken() {
  setToken(null);
  if (typeof window !== 'undefined') {
    PER_USER_KEYS.forEach((k) => window.localStorage.removeItem(k));
  }
  clearCache();
}

const cache: Record<string, any> = {};
const cacheTs: Record<string, number> = {};
const CACHE_TTL = 15000;

function invalidateCache(url: string) {
  if (url.includes('unavailability')) {
    Object.keys(cache).forEach((k) => {
      if (k.includes('unavailability') || k.includes('dashboard')) {
        delete cache[k];
        delete cacheTs[k];
      }
    });
  } else {
    Object.keys(cache).forEach((k) => { delete cache[k]; delete cacheTs[k]; });
  }
}

export function clearCache() {
  Object.keys(cache).forEach((k) => { delete cache[k]; delete cacheTs[k]; });
}

async function request<T = any>(method: Method, url: string, body: any = null, retries = 2): Promise<T> {
  if (method === 'GET' && cache[url] && (Date.now() - cacheTs[url]) < CACHE_TTL) {
    return JSON.parse(JSON.stringify(cache[url]));
  }

  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const opts: RequestInit = {
    method,
    headers,
    credentials: 'same-origin',
  };
  if (body) opts.body = JSON.stringify(body);

  try {
    const res = await fetch(url, opts);
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
      throw new Error(res.ok ? 'Resposta invalida do servidor' : `Erro ${res.status} do servidor`);
    }
    const data = await res.json();
    if (res.status === 401) {
      clearToken();
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/register')) {
        window.location.href = '/login';
      }
      throw new Error(data.error || 'Sessao expirada. Faca login novamente.');
    }
    if (!res.ok) throw new Error(data.error || 'Erro desconhecido');
    if (method === 'GET') {
      cache[url] = data;
      cacheTs[url] = Date.now();
    } else {
      invalidateCache(url);
    }
    return data;
  } catch (e: any) {
    if (retries > 0 && e instanceof TypeError) {
      const delay = (3 - retries) * 800 + 300;
      await new Promise((r) => setTimeout(r, delay));
      return request(method, url, body, retries - 1);
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
    // Limpa cache ANTES de logar para não servir dados de um usuário anterior
    // (o cache de 15s é global e não tem o usuário na chave).
    clearCache();
    const data = await request<any>('POST', '/api/auth/login', { email, password });
    if (data?.token) setToken(data.token);
    return data;
  },
  register: (data: any) => request('POST', '/api/auth/register', data),
  logout: async () => {
    clearCache();
    try {
      await request('POST', '/api/auth/logout');
    } finally {
      clearToken();
    }
  },
  me: () => request('GET', '/api/auth/me'),

  getSetores: () => request<string[]>('GET', '/api/setores'),
  createSetor: (name: string) => request('POST', '/api/admin/setores', { name }),
  updateSetor: (index: number, name: string) => request('PUT', `/api/admin/setores/${index}`, { name }),
  deleteSetor: (index: number) => request('DELETE', `/api/admin/setores/${index}`),
  assignUserSetor: (id: number, data: any) => request('POST', `/api/admin/users/${id}/assign-setor`, data),

  getUsers: () => request<any[]>('GET', '/api/admin/users'),
  getPending: () => request<any[]>('GET', '/api/admin/pending'),
  approveUser: (id: number) => request('POST', `/api/admin/approve/${id}`),
  rejectUser: (id: number) => request('POST', `/api/admin/reject/${id}`),
  deleteUser: (id: number) => request('DELETE', `/api/admin/users/${id}`),
  changeUserRole: (id: number, role: string) => request('POST', `/api/admin/change-role/${id}`, { role }),
  createUserDirect: (data: any) => request('POST', '/api/admin/users/create', data),

  getDashboard: () => request('GET', '/api/dashboard'),

  getEventosPublic: () => request<{ eventos: any[] }>('GET', '/api/eventos'),

  getNotifications: () => request<{ items: any[]; count: number }>('GET', '/api/notifications'),

  getClientes: () => request<{ clientes: any[]; links: any[] }>('GET', '/api/admin/clientes'),
  createCliente: (data: { nome: string; descricao?: string | null; ativo?: boolean }) =>
    request('POST', '/api/admin/clientes', data),
  updateCliente: (id: number, data: { nome?: string; descricao?: string | null; ativo?: boolean }) =>
    request('PUT', `/api/admin/clientes/${id}`, data),
  deleteCliente: (id: number) => request('DELETE', `/api/admin/clientes/${id}`),
  assignUserToCliente: (clienteId: number, userId: number, ativo: boolean) =>
    request('POST', `/api/admin/clientes/${clienteId}/assign`, { user_id: userId, ativo }),

  getEventos: () => request<{ eventos: any[] }>('GET', '/api/admin/eventos'),
  createEvento: (data: { nome: string; descricao?: string | null; data_inicio: string; data_fim: string; cliente_a?: boolean; cliente_b?: boolean; cliente_c?: boolean }) =>
    request('POST', '/api/admin/eventos', data),
  updateEvento: (id: number, data: any) =>
    request('PUT', `/api/admin/eventos/${id}`, data),
  deleteEvento: (id: number) => request('DELETE', `/api/admin/eventos/${id}`),

  getMembers: () => request<any[]>('GET', '/api/members'),
  getMemberByEmail: (email: string) => request('GET', `/api/members/by-email/${encodeURIComponent(email)}`),
  getMyMemberInfo: () => request<any>('GET', '/api/members/me'),
  createMember: (data: any) => request('POST', '/api/admin/members', data),
  updateMember: (id: number, data: any) => request('PUT', `/api/admin/members/${id}`, data),
  deleteMember: (id: number) => request('DELETE', `/api/admin/members/${id}`),

  createUnavailability: (data: any) => request('POST', '/api/unavailability', data),
  updateUnavailability: (id: number, data: any) => request('PATCH', `/api/unavailability/${id}`, data),
  getUnavailability: () => request<{ data: any[]; truncated: boolean }>('GET', '/api/unavailability'),
  getMyUnavailability: () => request<any[]>('GET', '/api/unavailability/mine'),
  getPendingUnavailability: () => request<any[]>('GET', '/api/unavailability/pending'),
  getActiveUnavailability: () => request<any[]>('GET', '/api/unavailability/active'),
  getUnavailabilityImpact: () => request<any>('GET', '/api/unavailability/impact'),
  approveUnavailability: (id: number) => request('POST', `/api/unavailability/${id}/approve`),
  rejectUnavailability: (id: number) => request('POST', `/api/unavailability/${id}/reject`),
  deleteUnavailability: (id: number) => request('DELETE', `/api/unavailability/${id}`),
};
