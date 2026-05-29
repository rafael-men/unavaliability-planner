import {
  isMasterAdmin,
  isAdmin,
  isAdminEditor,
  isLider,
  canViewAll,
  cleanText,
  countBusinessDays,
} from '../../app/lib/auth';

describe('lib/auth — role helpers', () => {
  describe('isMasterAdmin', () => {
    it('retorna true apenas para admin_master', () => {
      expect(isMasterAdmin('admin_master')).toBe(true);
      expect(isMasterAdmin('admin_editor')).toBe(false);
      expect(isMasterAdmin('admin_leitor')).toBe(false);
      expect(isMasterAdmin('socio')).toBe(false);
      expect(isMasterAdmin('lider')).toBe(false);
      expect(isMasterAdmin('colaborador')).toBe(false);
    });
  });

  describe('isAdmin', () => {
    it('retorna true para qualquer admin (master/editor/leitor)', () => {
      expect(isAdmin('admin_master')).toBe(true);
      expect(isAdmin('admin_editor')).toBe(true);
      expect(isAdmin('admin_leitor')).toBe(true);
      expect(isAdmin('socio')).toBe(false);
      expect(isAdmin('lider')).toBe(false);
      expect(isAdmin('colaborador')).toBe(false);
    });
  });

  describe('isAdminEditor', () => {
    it('retorna true apenas para master e editor', () => {
      expect(isAdminEditor('admin_master')).toBe(true);
      expect(isAdminEditor('admin_editor')).toBe(true);
      expect(isAdminEditor('admin_leitor')).toBe(false);
      expect(isAdminEditor('colaborador')).toBe(false);
    });
  });

  describe('isLider', () => {
    it('retorna true apenas para lider', () => {
      expect(isLider('lider')).toBe(true);
      expect(isLider('admin_master')).toBe(false);
      expect(isLider('colaborador')).toBe(false);
    });
  });

  describe('canViewAll', () => {
    it('admins e socio veem tudo', () => {
      expect(canViewAll('admin_master')).toBe(true);
      expect(canViewAll('admin_editor')).toBe(true);
      expect(canViewAll('admin_leitor')).toBe(true);
      expect(canViewAll('socio')).toBe(true);
    });
    it('lider e colaborador não', () => {
      expect(canViewAll('lider')).toBe(false);
      expect(canViewAll('colaborador')).toBe(false);
    });
  });
});

describe('lib/auth — cleanText', () => {
  it('faz trim e converte para string', () => {
    expect(cleanText('  hello  ')).toBe('hello');
    expect(cleanText(123)).toBe('123');
  });
  it('preserva caracteres especiais (escape é responsabilidade do consumidor)', () => {
    expect(cleanText('<script>alert(1)</script>')).toBe('<script>alert(1)</script>');
    expect(cleanText('John "Doe"')).toBe('John "Doe"');
    expect(cleanText("O'Brien")).toBe("O'Brien");
    expect(cleanText('a&b')).toBe('a&b');
  });
  it('vazio / null / undefined retorna string vazia', () => {
    expect(cleanText('')).toBe('');
    expect(cleanText(null)).toBe('');
    expect(cleanText(undefined)).toBe('');
  });
  it('trunca em maxLen', () => {
    const long = 'a'.repeat(1000);
    expect(cleanText(long, 100).length).toBe(100);
    expect(cleanText(long).length).toBe(500);
  });
});

describe('lib/auth — countBusinessDays', () => {
  function utcMs(y: number, m: number, d: number) {
    return Date.UTC(y, m - 1, d);
  }

  it('conta 5 dias úteis em uma semana (seg→sex)', () => {
    expect(countBusinessDays(utcMs(2026, 5, 11), utcMs(2026, 5, 15))).toBe(5);
  });

  it('ignora sábado e domingo', () => {
    expect(countBusinessDays(utcMs(2026, 5, 16), utcMs(2026, 5, 17))).toBe(0);
  });

  it('conta apenas 1 dia útil se for um único dia útil', () => {
    expect(countBusinessDays(utcMs(2026, 5, 13), utcMs(2026, 5, 13))).toBe(1);
  });

  it('pula fim de semana no meio do período', () => {
    expect(countBusinessDays(utcMs(2026, 5, 15), utcMs(2026, 5, 18))).toBe(2);
  });

  it('intervalo de 2 semanas inteiras', () => {
    expect(countBusinessDays(utcMs(2026, 5, 11), utcMs(2026, 5, 22))).toBe(10);
  });

  it('períodos só com sábado ou domingo retornam 0', () => {
    expect(countBusinessDays(utcMs(2026, 5, 16), utcMs(2026, 5, 16))).toBe(0);
    expect(countBusinessDays(utcMs(2026, 5, 17), utcMs(2026, 5, 17))).toBe(0);
  });
});
