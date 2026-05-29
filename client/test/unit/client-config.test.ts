import {
  isMasterAdminRole,
  isAdminRole,
  isEditorRole,
  isLiderRole,
  canViewAllRole,
  formatDate,
  formatDateShort,
  countBusinessDays,
  getMinRequestDate,
  DEPT_COLORS,
  STATUS_MAP,
  ROLE_LABELS,
} from '../../app/lib/client-config';

describe('client-config — role helpers', () => {
  it('isMasterAdminRole / isAdminRole / isEditorRole / isLiderRole / canViewAllRole', () => {
    expect(isMasterAdminRole('admin_master')).toBe(true);
    expect(isMasterAdminRole('admin_editor')).toBe(false);

    expect(isAdminRole('admin_master')).toBe(true);
    expect(isAdminRole('admin_editor')).toBe(true);
    expect(isAdminRole('admin_leitor')).toBe(true);
    expect(isAdminRole('socio')).toBe(false);

    expect(isEditorRole('admin_editor')).toBe(true);
    expect(isEditorRole('admin_leitor')).toBe(false);

    expect(isLiderRole('lider')).toBe(true);
    expect(isLiderRole('socio')).toBe(false);

    expect(canViewAllRole('socio')).toBe(true);
    expect(canViewAllRole('colaborador')).toBe(false);
  });
});

describe('client-config — formatadores', () => {
  it('formatDate: yyyy-mm-dd → dd/mm/yyyy', () => {
    expect(formatDate('2026-05-13')).toBe('13/05/2026');
    expect(formatDate('2024-01-01')).toBe('01/01/2024');
  });
  it('formatDate: vazio retorna "-"', () => {
    expect(formatDate('')).toBe('-');
    expect(formatDate(null)).toBe('-');
    expect(formatDate(undefined)).toBe('-');
  });
  it('formatDateShort: yyyy-mm-dd → dd/mm', () => {
    expect(formatDateShort('2026-05-13')).toBe('13/05');
    expect(formatDateShort('2024-01-01')).toBe('01/01');
  });
});

describe('client-config — countBusinessDays', () => {
  it('5 dias úteis em uma semana (seg→sex)', () => {
    expect(countBusinessDays('2026-05-11', '2026-05-15')).toBe(5);
  });
  it('ignora fim de semana', () => {
    expect(countBusinessDays('2026-05-16', '2026-05-17')).toBe(0);
  });
  it('intervalo cruzando fim de semana', () => {
    expect(countBusinessDays('2026-05-15', '2026-05-18')).toBe(2);
  });
  it('mesmo dia útil = 1', () => {
    expect(countBusinessDays('2026-05-13', '2026-05-13')).toBe(1);
  });
});

describe('client-config — getMinRequestDate', () => {
  it('retorna data ISO yyyy-mm-dd com 15 dias de antecedência', () => {
    const min = getMinRequestDate();
    expect(min).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    const expected = new Date();
    expected.setDate(expected.getDate() + 15);
    expect(min).toBe(expected.toISOString().split('T')[0]);
  });
});

describe('client-config — maps estáticos', () => {
  it('DEPT_COLORS tem todas as cores básicas', () => {
    expect(DEPT_COLORS['Atendimento']).toBeDefined();
    expect(DEPT_COLORS['Tecnologia']).toBeDefined();
  });
  it('STATUS_MAP tem pending/approved/rejected', () => {
    expect(STATUS_MAP.pending.label).toBe('Pendente');
    expect(STATUS_MAP.approved.label).toBe('Aprovado');
    expect(STATUS_MAP.rejected.label).toBe('Rejeitado');
  });
  it('ROLE_LABELS traduz todas as roles', () => {
    expect(ROLE_LABELS.admin_master).toBe('Admin Master');
    expect(ROLE_LABELS.colaborador).toBe('Prestador');
    expect(ROLE_LABELS.lider).toBe('Líder de Setor');
  });
});
