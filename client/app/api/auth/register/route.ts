import { NextRequest, NextResponse } from 'next/server';
import bcrypt from 'bcryptjs';
import { queries } from '../../../lib/database';
import { cleanText, isValidEmail } from '../../../lib/auth';
import { loadSetores } from '../../../lib/setores';

export async function POST(req: NextRequest) {
  const { email, password, full_name, department } = await req.json();
  if (!email || !password || !full_name || !department) {
    return NextResponse.json({ error: 'Todos os campos são obrigatórios.' }, { status: 400 });
  }
  const emailLower = email.toLowerCase().trim();
  if (!isValidEmail(emailLower)) {
    return NextResponse.json({ error: 'Email inválido.' }, { status: 400 });
  }
  const validDepts = loadSetores();
  if (!validDepts.includes(department)) {
    return NextResponse.json({ error: 'Setor inválido.' }, { status: 400 });
  }
  if (password.length < 6) {
    return NextResponse.json({ error: 'Senha deve ter pelo menos 6 caracteres.' }, { status: 400 });
  }
  const existing = await queries.getUserByEmail(emailLower);
  if (existing) {
    return NextResponse.json({ error: 'Este email já está cadastrado.' }, { status: 400 });
  }
  try {
    const hash = bcrypt.hashSync(password, 10);
    let member = await queries.getMemberByEmail(emailLower);
    if (!member) {
      member = await queries.createMember({
        name: cleanText(full_name),
        email: emailLower,
        area: department,
        squad: null,
        funcao: null,
        report_to: null,
        operacoes: true,
        day_offs_quota: 20,
      });
    }
    await queries.createUser({
      email: emailLower,
      password: hash,
      full_name: cleanText(full_name),
      department,
      member_id: (member as any).id,
      role: 'colaborador',
    });
    return NextResponse.json({ success: true, message: 'Cadastro realizado! Aguarde aprovação de um administrador.' });
  } catch (e: any) {
    return NextResponse.json({ error: 'Erro ao criar conta: ' + e.message }, { status: 500 });
  }
}
