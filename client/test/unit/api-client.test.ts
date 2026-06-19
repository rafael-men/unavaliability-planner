import { beforeAll, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { API, clearCache } from '../../app/lib/api-client';


const locationMock = { pathname: '/some-page', href: '' };
beforeAll(() => {
  delete (window as any).location;
  (window as any).location = locationMock;
});

function mockFetch(responses: { status: number; body: unknown; contentType?: string }[]) {
  let call = 0;
  global.fetch = jest.fn().mockImplementation(() => {
    const r = responses[Math.min(call++, responses.length - 1)];
    const ct = r.contentType ?? 'application/json';
    return Promise.resolve({
      status: r.status,
      ok: r.status >= 200 && r.status < 300,
      headers: { get: (h: string) => (h === 'content-type' ? ct : null) },
      json: () => Promise.resolve(r.body),
    });
  }) as typeof global.fetch;
}

beforeEach(() => {
  clearCache();
  jest.clearAllMocks();
  locationMock.pathname = '/some-page';
  locationMock.href = '';
});


describe('API.getSetores', () => {
  it('retorna lista de setores em caso de sucesso', async () => {
    mockFetch([{ status: 200, body: ['Atendimento', 'Tecnologia'] }]);
    const result = await API.getSetores();
    expect(result).toEqual(['Atendimento', 'Tecnologia']);
  });
});

describe('API.login', () => {
  it('retorna user após login bem-sucedido', async () => {
    const user = { id: 1, email: 'a@b.com', full_name: 'Test', role: 'admin_master' };
    mockFetch([{ status: 200, body: { user } }]);
    const result = await API.login('a@b.com', '123');
    expect(result.user).toMatchObject({ email: 'a@b.com' });
  });
});


describe('cache de GET', () => {
  it('segunda chamada usa cache sem disparar fetch', async () => {
    mockFetch([{ status: 200, body: ['Atendimento'] }]);
    await API.getSetores();
    await API.getSetores();
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('clearCache faz próxima chamada ir ao servidor', async () => {
    mockFetch([
      { status: 200, body: ['Atendimento'] },
      { status: 200, body: ['Tecnologia'] },
    ]);
    await API.getSetores();
    clearCache();
    const result = await API.getSetores();
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(result).toEqual(['Tecnologia']);
  });

  it('POST invalida cache de unavailability', async () => {
    mockFetch([
      { status: 200, body: { data: [], truncated: false } },
      { status: 200, body: { id: 1 } },
      { status: 200, body: { data: [{ id: 1 }], truncated: false } },
    ]);
    await API.getUnavailability();
    await API.createUnavailability({
      unavailability_type: 'pontual',
      start_date: '2026-07-01',
      end_date: '2026-07-01',
    });
    await API.getUnavailability();
    expect(fetch).toHaveBeenCalledTimes(3);
  });

  it('cache retorna cópia profunda, não referência', async () => {
    mockFetch([{ status: 200, body: ['Atendimento'] }]);
    const r1 = await API.getSetores();
    r1.push('MUTADO');
    const r2 = await API.getSetores();
    expect(r2).not.toContain('MUTADO');
  });
});


describe('erros HTTP', () => {
  it('lança erro com mensagem do servidor em 400', async () => {
    mockFetch([{ status: 400, body: { error: 'Dados inválidos' } }]);
    await expect(API.getSetores()).rejects.toThrow('Dados inválidos');
  });

  it('lança erro genérico quando body não tem .error', async () => {
    mockFetch([{ status: 500, body: {} }]);
    await expect(API.getSetores()).rejects.toThrow('Erro desconhecido');
  });

  it('lança erro quando Content-Type não é JSON', async () => {
    mockFetch([{ status: 200, body: '<html>', contentType: 'text/html' }]);
    await expect(API.getSetores()).rejects.toThrow(/Resposta inválida/);
  });
});

describe('resposta 401', () => {
  it('limpa cache e lança erro de sessão expirada', async () => {
    mockFetch([
      { status: 200, body: ['Atendimento'] },
      { status: 401, body: { error: 'Token inválido' } },
      { status: 200, body: ['Tecnologia'] },
    ]);
    await API.getSetores();
    await expect(API.getMembers()).rejects.toThrow('Token inválido');
    await API.getSetores();
    expect(fetch).toHaveBeenCalledTimes(3);
  });

  it('não redireciona se já está em /login', async () => {
    locationMock.pathname = '/login';
    const hrefBefore = locationMock.href;
    mockFetch([{ status: 401, body: { error: 'Não autorizado' } }]);
    await expect(API.getSetores()).rejects.toThrow();
    expect(locationMock.href).toBe(hrefBefore);
  });
});


describe('retry em falha de rede', () => {
  it('tenta novamente após TypeError (rede) e resolve na segunda', async () => {
    let call = 0;
    global.fetch = jest.fn().mockImplementation(() => {
      call++;
      if (call === 1) return Promise.reject(new TypeError('Failed to fetch'));
      return Promise.resolve({
        status: 200,
        ok: true,
        headers: { get: () => 'application/json' },
        json: () => Promise.resolve(['Atendimento']),
      });
    }) as typeof global.fetch;
    const result = await API.getSetores();
    expect(result).toEqual(['Atendimento']);
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it('lança erro de conexão após esgotar retries', async () => {
    global.fetch = jest.fn().mockRejectedValue(new TypeError('Failed to fetch') as never) as typeof global.fetch;
    await expect(API.getSetores()).rejects.toThrow(/Sem conexão/);
    expect(fetch).toHaveBeenCalledTimes(3);
  });

  it('não faz retry em erros que não são TypeError', async () => {
    mockFetch([{ status: 500, body: { error: 'Erro interno' } }]);
    await expect(API.getSetores()).rejects.toThrow('Erro interno');
    expect(fetch).toHaveBeenCalledTimes(1);
  });
});


describe('API.logout', () => {
  it('limpa cache mesmo se a chamada ao servidor falhar', async () => {
    mockFetch([
      { status: 200, body: ['Atendimento'] },   
      { status: 500, body: { error: 'Falha' } },
      { status: 200, body: ['Tecnologia'] },     
    ]);
    await API.getSetores();
    await API.logout();
    const result = await API.getSetores();
    expect(result).toEqual(['Tecnologia']);
    expect(fetch).toHaveBeenCalledTimes(3);
  });
});
