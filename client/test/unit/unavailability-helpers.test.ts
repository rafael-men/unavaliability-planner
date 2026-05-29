jest.mock('../../app/lib/database', () => ({
  queries: {
    getUserById: jest.fn(),
    getMemberById: jest.fn(),
    getMemberByEmail: jest.fn(),
  },
}));

import { canApproveUnavailability } from '../../app/lib/unavailability-helpers';
import { queries } from '../../app/lib/database';
import type { AuthUser } from '../../app/lib/auth';

const mockQueries = queries as jest.Mocked<typeof queries>;

beforeEach(() => {
  jest.resetAllMocks();
});

function user(overrides: Partial<AuthUser>): AuthUser {
  return {
    id: 1,
    email: 'x@macfor.com.br',
    full_name: 'X',
    role: 'colaborador',
    status: 'approved',
    department: null,
    member_id: null,
    ...overrides,
  };
}

describe('canApproveUnavailability', () => {
  it('retorna false se aprovador é o dono da solicitação', async () => {
    const u = user({ id: 5, role: 'lider' });
    const record = { user_id: 5 };
    expect(await canApproveUnavailability(u, record)).toBe(false);
  });

  it('retorna true para admin_editor independentemente do setor', async () => {
    const u = user({ id: 1, role: 'admin_editor' });
    const record = { user_id: 99 };
    expect(await canApproveUnavailability(u, record)).toBe(true);
  });

  it('retorna true para admin_master', async () => {
    const u = user({ id: 1, role: 'admin_master' });
    const record = { user_id: 99 };
    expect(await canApproveUnavailability(u, record)).toBe(true);
  });

  it('retorna false para colaborador', async () => {
    const u = user({ id: 1, role: 'colaborador' });
    const record = { user_id: 99 };
    expect(await canApproveUnavailability(u, record)).toBe(false);
  });

  it('lider aprova requester do mesmo department', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'Tecnologia', email: 'lider@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'Tecnologia', member_id: null,
    } as any);
    expect(await canApproveUnavailability(lider, record)).toBe(true);
  });

  it('lider NÃO aprova requester de outro department sem report_to', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'Tecnologia', email: 'lider@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'Conteudo', member_id: null,
    } as any);
    mockQueries.getMemberByEmail.mockResolvedValueOnce(null);
    expect(await canApproveUnavailability(lider, record)).toBe(false);
  });

  it('lider aprova requester com report_to apontando para seu email', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'A', email: 'lider@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'B', member_id: 50,
    } as any);
    mockQueries.getMemberById.mockResolvedValueOnce({
      id: 50, report_to: 'lider@macfor.com.br',
    } as any);
    mockQueries.getMemberByEmail.mockResolvedValueOnce({
      id: 88, email: 'lider@macfor.com.br', name: 'Joao Lider',
    } as any);
    expect(await canApproveUnavailability(lider, record)).toBe(true);
  });

  it('lider aprova requester com report_to apontando para seu nome (sem email)', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'A', email: 'lider@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'B', member_id: 50,
    } as any);
    mockQueries.getMemberById.mockResolvedValueOnce({
      id: 50, report_to: 'Joao Lider',
    } as any);
    mockQueries.getMemberByEmail.mockResolvedValueOnce({
      id: 88, email: 'lider@macfor.com.br', name: 'Joao Lider',
    } as any);
    expect(await canApproveUnavailability(lider, record)).toBe(true);
  });

  it('lider com report_to múltiplo (separado por vírgula) — aprova se nome bate', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'A', email: 'lider@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'B', member_id: 50,
    } as any);
    mockQueries.getMemberById.mockResolvedValueOnce({
      id: 50, report_to: 'outro@macfor.com.br, Joao Lider, mais@macfor.com.br',
    } as any);
    mockQueries.getMemberByEmail.mockResolvedValueOnce({
      id: 88, email: 'lider@macfor.com.br', name: 'Joao Lider',
    } as any);
    expect(await canApproveUnavailability(lider, record)).toBe(true);
  });

  it('retorna false se member tem report_to mas não aponta para o lider', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'A', email: 'lider@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'B', member_id: 50,
    } as any);
    mockQueries.getMemberById.mockResolvedValueOnce({
      id: 50, report_to: 'outra.pessoa@macfor.com.br',
    } as any);
    mockQueries.getMemberByEmail.mockResolvedValueOnce({
      id: 88, email: 'lider@macfor.com.br', name: 'Joao Lider',
    } as any);
    expect(await canApproveUnavailability(lider, record)).toBe(false);
  });

  it('socio NÃO aprova (apenas visualiza)', async () => {
    const u = user({ id: 1, role: 'socio' });
    const record = { user_id: 99 };
    expect(await canApproveUnavailability(u, record)).toBe(false);
  });

  it('NÃO aprova quando nome do líder é substring do nome no report_to (falso positivo evitado)', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'A', email: 'ana@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'B', member_id: 50,
    } as any);
    mockQueries.getMemberById.mockResolvedValueOnce({
      id: 50, report_to: 'Mariana Costa',
    } as any);
    mockQueries.getMemberByEmail.mockResolvedValueOnce({
      id: 88, email: 'ana@macfor.com.br', name: 'Ana',
    } as any);
    expect(await canApproveUnavailability(lider, record)).toBe(false);
  });

  it('NÃO aprova quando email do líder contém parcialmente outro email no report_to', async () => {
    const lider = user({ id: 10, role: 'lider', department: 'A', email: 'joao.silva@macfor.com.br' });
    const record = { user_id: 99 };
    mockQueries.getUserById.mockResolvedValueOnce({
      id: 99, email: 'sub@macfor.com.br', department: 'B', member_id: 50,
    } as any);
    mockQueries.getMemberById.mockResolvedValueOnce({
      id: 50, report_to: 'j@macfor.com.br',
    } as any);
    mockQueries.getMemberByEmail.mockResolvedValueOnce({
      id: 88, email: 'joao.silva@macfor.com.br', name: 'Joao Silva',
    } as any);
    expect(await canApproveUnavailability(lider, record)).toBe(false);
  });
});
