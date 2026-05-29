
import 'dotenv/config';
import { createClient } from '@supabase/supabase-js';
import bcrypt from 'bcryptjs';

const supabase = createClient(process.env.SUPABASE_URL!, process.env.SUPABASE_KEY!);
const USERS_TABLE = 'users5';
const MEMBERS_TABLE = 'members';

async function main() {
  if (!process.env.SUPABASE_URL || !process.env.SUPABASE_KEY) {
    console.error('SUPABASE_URL ou SUPABASE_KEY não definidos no .env');
    process.exit(1);
  }

  const email = 'gustavo.romao@macfor.com.br';
  const senha = 'admin123';

  console.log('Verificando admins existentes...');
  const adminsExist = await supabase
    .from(USERS_TABLE)
    .select('id, email, role')
    .in('role', ['admin_master', 'admin_editor', 'admin_leitor', 'socio'])
    .limit(5);
  console.log('Admins atuais:', adminsExist.data);

  const memberRes = await supabase.from(MEMBERS_TABLE).select('id').eq('email', email).single();
  const memberId = memberRes.data ? (memberRes.data as any).id : null;

  const existing = await supabase.from(USERS_TABLE).select('id, role').eq('email', email).single();

  if (existing.data) {
    console.log(`Usuário ${email} já existe (id=${(existing.data as any).id}). Atualizando para admin_master...`);
    const hash = await bcrypt.hash(senha, 10);
    const upd = await supabase
      .from(USERS_TABLE)
      .update({ role: 'admin_master', status: 'approved', passw: hash, member_id: memberId })
      .eq('email', email);
    if (upd.error) {
      console.error('Erro ao atualizar:', upd.error);
      process.exit(1);
    }
    console.log(`✓ ${email} agora é admin_master (senha resetada para "${senha}")`);
  } else {
    console.log(`Criando ${email}...`);
    const hash = await bcrypt.hash(senha, 10);
    const ins = await supabase.from(USERS_TABLE).insert([{
      email,
      passw: hash,
      nome: 'Gustavo Romão (Admin)',
      role: 'admin_master',
      status: 'approved',
      member_id: memberId,
    }]);
    if (ins.error) {
      console.error('Erro ao criar:', ins.error);
      process.exit(1);
    }
    console.log(`✓ ${email} criado como admin_master`);
  }

  console.log('\nLogin:');
  console.log(`  Email: ${email}`);
  console.log(`  Senha: ${senha}`);
}

main().catch((err) => { console.error(err); process.exit(1); });
