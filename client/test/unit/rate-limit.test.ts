import { checkLoginRate } from '../../app/lib/auth';

describe('lib/auth — checkLoginRate', () => {
  it('permite até 10 tentativas por janela', () => {
    const ip = 'unit-test-ip-' + Math.random();
    for (let i = 0; i < 10; i++) {
      expect(checkLoginRate(ip).allowed).toBe(true);
    }
  });

  it('bloqueia 11ª tentativa com mensagem em minutos', () => {
    const ip = 'unit-test-ip-' + Math.random();
    for (let i = 0; i < 10; i++) checkLoginRate(ip);
    const res = checkLoginRate(ip);
    expect(res.allowed).toBe(false);
    expect(res.minutes).toBeGreaterThan(0);
    expect(res.minutes).toBeLessThanOrEqual(15);
  });

  it('isola contagem por IP', () => {
    const ipA = 'ip-A-' + Math.random();
    const ipB = 'ip-B-' + Math.random();
    for (let i = 0; i < 10; i++) checkLoginRate(ipA);
    expect(checkLoginRate(ipA).allowed).toBe(false);
    expect(checkLoginRate(ipB).allowed).toBe(true);
  });
});
