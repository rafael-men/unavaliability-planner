import 'dotenv/config';
import { createClient } from '@supabase/supabase-js';
import bcrypt from 'bcryptjs';

const supabase = createClient(process.env.SUPABASE_URL!, process.env.SUPABASE_KEY!);
const USERS_TABLE = 'users5';
const MEMBERS_TABLE = 'members';

const SENHA = 'qa123456';
const DEPT = 'Projetos/Operacoes';

const LIDER = {
  nome: 'QA Reporte Lider',
  email: 'qa.reporte@macfor.com.br',
  role: 'lider',
  department: DEPT,
};

const PRESTADOR = {
  nome: 'QA Subordinado Prestador',
  email: 'qa.subordinado@macfor.com.br',
  role: 'colaborador',
  department: DEPT,
  report_to: 'QA Reporte Lider',
};

async function findOrCreateMember(data: any) {
  const existing = await supabase.from(MEMBERS_TABLE).select('id').eq('email', data.email).single();
  if (existing.data) {
    console.log(`  Membro já existe: ${data.email} (id=${(existing.data as any).id})`);
    return existing.data as any;
  }
  const created = await supabase.from(MEMBERS_TABLE).insert([data]).select();
  if (created.error) throw created.error;
  console.log(`  Membro criado: ${data.email} (id=${created.data![0].id})`);
  return created.data![0];
}

async function findOrCreateUser({ email, nome, role, department, member_id }: any) {
  const existing = await supabase.from(USERS_TABLE).select('id').eq('email', email).single();
  if (existing.data) {
    console.log(`  Usuário já existe: ${email} (id=${(existing.data as any).id})`);
    return existing.data as any;
  }
  const hash = bcrypt.hashSync(SENHA, 10);
  const created = await supabase.from(USERS_TABLE).insert([{
    email,
    passw: hash,
    nome,
    department,
    role,
    member_id,
    status: 'approved',
  }]).select();
  if (created.error) throw created.error;
  console.log(`  Usuário criado e aprovado: ${email} (id=${created.data![0].id})`);
  return created.data![0];
}

async function main() {
  if (!process.env.SUPABASE_URL || !process.env.SUPABASE_KEY) {
    console.error('SUPABASE_URL ou SUPABASE_KEY não definidos no .env');
    process.exit(1);
  }

  console.log('\n=== Seed QA: fluxo reporte ===\n');

  console.log('1. Criando membro líder...');
  const memberLider = await findOrCreateMember({
    name: LIDER.nome,
    email: LIDER.email,
    area: DEPT,
    squad: null,
    funcao: 'QA Lider',
    report_to: null,
    operacoes: true,
    day_offs_quota: 20,
  });

  console.log('2. Criando membro prestador...');
  const memberPrestador = await findOrCreateMember({
    name: PRESTADOR.nome,
    email: PRESTADOR.email,
    area: DEPT,
    squad: null,
    funcao: 'QA Prestador',
    report_to: PRESTADOR.report_to,
    operacoes: true,
    day_offs_quota: 20,
  });

  console.log('3. Criando usuário líder...');
  await findOrCreateUser({
    email: LIDER.email,
    nome: LIDER.nome,
    role: LIDER.role,
    department: LIDER.department,
    member_id: memberLider.id,
  });

  console.log('4. Criando usuário prestador...');
  await findOrCreateUser({
    email: PRESTADOR.email,
    nome: PRESTADOR.nome,
    role: PRESTADOR.role,
    department: PRESTADOR.department,
    member_id: memberPrestador.id,
  });

  console.log('\n=== Concluído ===');
  console.log('Líder:     qa.reporte@macfor.com.br     / qa123456');
  console.log('Prestador: qa.subordinado@macfor.com.br / qa123456');
  console.log('\nFluxo de teste:');
  console.log('  1. Login como prestador → submete solicitação de indisponibilidade');
  console.log('  2. Login como líder     → aba Pendentes mostra a solicitação do prestador');
}

main().catch((err) => { console.error('Erro:', err); process.exit(1); });
