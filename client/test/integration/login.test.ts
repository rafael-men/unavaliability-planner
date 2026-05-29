jest.mock('../../app/lib/database', () => ({
  queries: {
    getUserByEmail: jest.fn(),
    getUserById: jest.fn(),
  },
}));
jest.mock('../../app/lib/session', () => ({
  getSession: jest.fn().mockResolvedValue({
    userId: undefined,
    save: jest.fn(),
    destroy: jest.fn(),
  }),
}));

import { POST } from '../../app/api/auth/login/route';
import { queries } from '../../app/lib/database';
import bcrypt from 'bcryptjs';

const mockQueries = queries as jest.Mocked<typeof queries>;

function buildRequest(body: any, headers: Record<string, string> = {}): any {
  return {
    json: async () => body,
    headers: {
      get: (k: string) => headers[k.toLowerCase()] || null,
    },
  };
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe('POST /api/auth/login', () => {
  it('400 quando email ou senha estão faltando', async () => {
    const res = await POST(buildRequest({ email: '', password: '' }));
    expect(res.status).toBe(400);
    const json = await res.json();
    expect(json.error).toMatch(/obrigat/i);
  });

  it('401 quando usuário não existe', async () => {
    mockQueries.getUserByEmail.mockResolvedValueOnce(null);
    const res = await POST(buildRequest({ email: 'unknown@macfor.com.br', password: 'x' }));
    expect(res.status).toBe(401);
    const json = await res.json();
    expect(json.error).toMatch(/incorretos/i);
  });

  it('401 quando senha errada', async () => {
    const hash = bcrypt.hashSync('senha-correta', 10);
    mockQueries.getUserByEmail.mockResolvedValueOnce({
      id: 1, email: 'a@macfor.com.br', password: hash, full_name: 'A', role: 'colaborador', status: 'approved',
    } as any);
    const res = await POST(buildRequest({ email: 'a@macfor.com.br', password: 'senha-errada' }));
    expect(res.status).toBe(401);
  });

  it('403 quando user.status = pending', async () => {
    const hash = bcrypt.hashSync('senha', 10);
    mockQueries.getUserByEmail.mockResolvedValueOnce({
      id: 1, email: 'a@macfor.com.br', password: hash, full_name: 'A', role: 'colaborador', status: 'pending',
    } as any);
    const res = await POST(buildRequest({ email: 'a@macfor.com.br', password: 'senha' }));
    expect(res.status).toBe(403);
    const json = await res.json();
    expect(json.error).toMatch(/aprovação/i);
  });

  it('403 quando user.status = rejected', async () => {
    const hash = bcrypt.hashSync('senha', 10);
    mockQueries.getUserByEmail.mockResolvedValueOnce({
      id: 1, email: 'a@macfor.com.br', password: hash, full_name: 'A', role: 'colaborador', status: 'rejected',
    } as any);
    const res = await POST(buildRequest({ email: 'a@macfor.com.br', password: 'senha' }));
    expect(res.status).toBe(403);
    const json = await res.json();
    expect(json.error).toMatch(/rejeit/i);
  });

  it('200 e devolve user em login válido', async () => {
    const hash = bcrypt.hashSync('senha', 10);
    mockQueries.getUserByEmail.mockResolvedValueOnce({
      id: 42, email: 'ok@macfor.com.br', password: hash, full_name: 'OK', role: 'admin_master', status: 'approved',
    } as any);
    const res = await POST(buildRequest({ email: 'ok@macfor.com.br', password: 'senha' }));
    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.success).toBe(true);
    expect(json.user).toEqual({ id: 42, email: 'ok@macfor.com.br', full_name: 'OK', role: 'admin_master' });
  });

  it('429 após 10 tentativas no mesmo IP', async () => {
    mockQueries.getUserByEmail.mockResolvedValue(null);
    const ip = 'rate-limit-ip-' + Math.random();
    for (let i = 0; i < 10; i++) {
      await POST(buildRequest({ email: 'x@macfor.com.br', password: 'x' }, { 'x-forwarded-for': ip }));
    }
    const res = await POST(buildRequest({ email: 'x@macfor.com.br', password: 'x' }, { 'x-forwarded-for': ip }));
    expect(res.status).toBe(429);
  });
});
