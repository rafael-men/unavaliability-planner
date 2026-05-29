# Sistema de Indisponibilidade

Aplicação web para controle de períodos de indisponibilidade de agenda (férias, day-offs) com fluxo de aprovação, gestão de clientes/eventos e detecção de conflitos.

## O que a aplicação faz

- **Solicitação de indisponibilidade** — tipo prolongado (mínimo 5 dias úteis) ou pontual (1 dia), com antecedência mínima de 15 dias.
- **Aprovação hierárquica** — líder do setor aprova pedidos do seu time; admins aprovam qualquer pedido.
- **Cadastro de clientes** — admins criam clientes e alocam/desalocam usuários a cada um.
- **Cadastro de eventos** — admins criam eventos com datas e clientes envolvidos.
- **Alerta de conflito** — quando um usuário alocado a um cliente pede indisponibilidade durante um evento desse cliente, o líder vê banner vermelho na pendência e células destacadas no calendário.
- **Calendário consolidado** — visão de indisponibilidades aprovadas, pendentes, eventos e conflitos.
- **Notificações** — sineta na navbar com badge vermelho para novos cadastros e pedidos pendentes.
- **Gestão de usuários, setores e membros** — admin master.

## Papéis

| Papel           | Pode                                          |
| --------------- | --------------------------------------------- |
| `admin_master`  | Tudo                                          |
| `admin_editor`  | Gerenciar clientes, eventos, aprovar pedidos  |
| `admin_leitor`  | Visualizar                                    |
| `socio`         | Visualizar tudo                               |
| `lider`         | Aprovar pedidos do seu setor/subordinados     |
| `colaborador`   | Solicitar para si                             |

## Stack

Next.js 16 (App Router) · React 19 · TypeScript · Supabase (Postgres) · iron-session · PrimeReact · Tailwind 4 · Lucide

## Como rodar

**1. Pré-requisitos:** Node.js 20+ e conta Supabase.

**2. Crie `.env.local` na raiz:**

```env
SUPABASE_URL=https://<projeto>.supabase.co
SUPABASE_KEY=<service-role-key>
SESSION_SECRET=<string-aleatoria-de-32-ou-mais-caracteres>
```

**3. Instale e configure o banco:**

```bash
npm install
```

Rode [scripts/schema.sql](scripts/schema.sql) no SQL Editor do Supabase (é idempotente).

**4. Popule dados iniciais:**

```bash
npm run seed:admin    # cria admin master
npm run seed:members  # importa membros
npm run seed:qa       # opcional: usuários de teste
```

**5. Start:**

```bash
npm run dev                  # dev em http://localhost:3000
npm run build && npm start   # produção
npm test                     # testes
```

## Estrutura

```text
app/
  (pages)/         # rotas de UI
    admin/         # users, setores, members, clientes, eventos
    unavailability/  # painel principal
    login, register
  api/             # rotas REST (auth, admin, unavailability, eventos, notifications)
    models/        # camada Evento, Cliente
  components/      # Navbar, NotificationBell, UnavailForm, UnavailCalendar, etc.
  lib/             # auth, database, session, helpers
scripts/           # schema.sql + seeds
test/              # unit + integration (Jest)
```

## Regras de negócio

- Indisponibilidade `prolongado` exige mínimo **5 dias úteis**.
- Data de início ≥ **hoje + 15 dias**.
- Sem sobreposição com pedidos próprios pendentes/aprovados.
- Cota anual de dias resetada por ano civil.
- `admin_master` pode aprovar a própria solicitação; demais não.
