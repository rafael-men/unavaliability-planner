jest.mock('../../app/lib/database', () => ({
  queries: {
    getUserById: jest.fn(),
    getUserActiveUnavailability: jest.fn(),
    createUnavailability: jest.fn(),
    getAllUnavailability: jest.fn(),
    getUserUnavailability: jest.fn(),
  },
}));
jest.mock('../../app/lib/session', () => ({
  getSession: jest.fn(),
}));
jest.mock('../../app/lib/setores', () => ({
  loadSetores: jest.fn(() => ['Tecnologia', 'Atendimento', 'Conteudo']),
}));

import { POST, GET } from '../../app/api/unavailability/route';
import { queries } from '../../app/lib/database';
import { getSession } from '../../app/lib/session';

const mockQueries = queries as jest.Mocked<typeof queries>;
const mockGetSession = getSession as jest.Mock;

function authedUser(overrides: Partial<any> = {}) {
  return {
    id: 1, email: 'u@macfor.com.br', full_name: 'User', role: 'colaborador',
    status: 'approved', department: 'Tecnologia', member_id: null,
    ...overrides,
  };
}

function buildRequest(body: any): any {
  return { json: async () => body };
}

function futureDate(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().split('T')[0];
}

function nextWeekdayUTC(daysAhead: number): string {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() + daysAhead);
  while (d.getUTCDay() === 0 || d.getUTCDay() === 6) {
    d.setUTCDate(d.getUTCDate() + 1);
  }
  return d.toISOString().split('T')[0];
}

beforeEach(() => {
  jest.clearAllMocks();
  mockGetSession.mockResolvedValue({
    userId: 1,
    save: jest.fn(),
    destroy: jest.fn(),
  });
  mockQueries.getUserById.mockResolvedValue(authedUser() as any);
});

describe('POST /api/unavailability', () => {
  it('401 sem sessão', async () => {
    mockGetSession.mockResolvedValueOnce({ userId: undefined, save: jest.fn(), destroy: jest.fn() });
    const res = await POST(buildRequest({}));
    expect(res.status).toBe(401);
  });

  it('400 com campos faltando', async () => {
    const res = await POST(buildRequest({ unavailability_type: 'prolongado' }));
    expect(res.status).toBe(400);
    const json = await res.json();
    expect(json.error).toMatch(/obrigat/i);
  });

  it('400 com tipo inválido', async () => {
    const res = await POST(buildRequest({
      unavailability_type: 'X', department: 'Tecnologia',
      start_date: futureDate(20), end_date: futureDate(22), total_days: 3,
    }));
    expect(res.status).toBe(400);
    const json = await res.json();
    expect(json.error).toMatch(/inválid/i);
  });

  it('400 com setor inválido', async () => {
    const res = await POST(buildRequest({
      unavailability_type: 'prolongado', department: 'NaoExiste',
      start_date: futureDate(20), end_date: futureDate(22), total_days: 3,
    }));
    expect(res.status).toBe(400);
  });

  it('400 quando start_date < hoje+15', async () => {
    const start = futureDate(5);
    const end = futureDate(7);
    const res = await POST(buildRequest({
      unavailability_type: 'prolongado', department: 'Tecnologia',
      start_date: start, end_date: end, total_days: 3,
    }));
    expect(res.status).toBe(400);
    const json = await res.json();
    expect(json.error).toMatch(/15 dias/i);
  });

  it('400 quando end < start', async () => {
    const start = futureDate(25);
    const end = futureDate(20);
    const res = await POST(buildRequest({
      unavailability_type: 'prolongado', department: 'Tecnologia',
      start_date: start, end_date: end, total_days: 1,
    }));
    expect(res.status).toBe(400);
    const json = await res.json();
    expect(json.error).toMatch(/posterior/i);
  });

  it('400 quando total_days não bate com cálculo', async () => {
    mockQueries.getUserActiveUnavailability.mockResolvedValueOnce([]);
    const start = nextWeekdayUTC(20);
    const end = start;
    const res = await POST(buildRequest({
      unavailability_type: 'pontual', department: 'Tecnologia',
      start_date: start, end_date: end, total_days: 99,
    }));
    expect(res.status).toBe(400);
    const json = await res.json();
    expect(json.error).toMatch(/dias úteis inválido/i);
  });

  it('400 quando há overlap com solicitação aprovada', async () => {
    const start = nextWeekdayUTC(20);
    mockQueries.getUserActiveUnavailability.mockResolvedValueOnce([
      { id: 999, start_date: start, end_date: start, status: 'approved' } as any,
    ]);
    const res = await POST(buildRequest({
      unavailability_type: 'pontual', department: 'Tecnologia',
      start_date: start, end_date: start, total_days: 1,
    }));
    expect(res.status).toBe(400);
    const json = await res.json();
    expect(json.error).toMatch(/sobrep/i);
  });

  it('200 quando tudo válido', async () => {
    mockQueries.getUserActiveUnavailability.mockResolvedValueOnce([]);
    mockQueries.createUnavailability.mockResolvedValueOnce({ id: 1 } as any);
    const start = nextWeekdayUTC(20);
    const res = await POST(buildRequest({
      unavailability_type: 'pontual', department: 'Tecnologia',
      start_date: start, end_date: start, total_days: 1,
    }));
    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.success).toBe(true);
    expect(mockQueries.createUnavailability).toHaveBeenCalledWith(
      expect.objectContaining({
        user_id: 1, unavailability_type: 'pontual', department: 'Tecnologia',
        start_date: start, end_date: start, total_days: 1, status: 'pending',
      })
    );
  });
});

describe('GET /api/unavailability', () => {
  it('401 sem sessão', async () => {
    mockGetSession.mockResolvedValueOnce({ userId: undefined, save: jest.fn(), destroy: jest.fn() });
    const res = await GET();
    expect(res.status).toBe(401);
  });

  it('colaborador recebe apenas suas solicitações (sem truncated)', async () => {
    mockQueries.getUserUnavailability.mockResolvedValueOnce([{ id: 1 }, { id: 2 }] as any);
    const res = await GET();
    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.truncated).toBe(false);
    expect(json.data).toHaveLength(2);
    expect(mockQueries.getUserUnavailability).toHaveBeenCalledWith(1);
  });

  it('admin recebe todas (limit 500, truncated quando atinge)', async () => {
    mockQueries.getUserById.mockResolvedValueOnce(authedUser({ role: 'admin_master' }) as any);
    const fake500 = Array.from({ length: 500 }, (_, i) => ({ id: i }));
    mockQueries.getAllUnavailability.mockResolvedValueOnce(fake500 as any);
    const res = await GET();
    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.truncated).toBe(true);
    expect(json.data).toHaveLength(500);
  });

  it('socio recebe todas (canViewAll)', async () => {
    mockQueries.getUserById.mockResolvedValueOnce(authedUser({ role: 'socio' }) as any);
    mockQueries.getAllUnavailability.mockResolvedValueOnce([] as any);
    const res = await GET();
    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.data).toEqual([]);
  });
});
