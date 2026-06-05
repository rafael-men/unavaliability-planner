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

## Arquitetura

A aplicação é dividida em dois projetos:

- **`backend/`** — API REST em **Java 21 / Spring Boot 4** (Spring Security + JWT, JPA/Hibernate, Flyway). É a única fonte de lógica de negócio e o que fala com o banco (PostgreSQL/Supabase).
- **`client/`** — front-end **Next.js 16** (App Router). As rotas em `client/app/api/**` são uma camada **BFF/proxy** fina: encaminham `/api/**` para o backend Java, repassando o token. Não há mais lógica de negócio no Next.

```text
Browser → /api/...  (mesma origem, sem CORS)
   → client/app/api/**/route.ts   (proxy via client/app/lib/backend.ts)
      → http://localhost:8080/api/...   (backend Java)
```

## Stack

**Backend:** Java 21 · Spring Boot 4 (Web MVC, Data JPA, Security) · JWT (jjwt) · Flyway · PostgreSQL (Supabase) · Maven

**Front:** Next.js 16 (App Router) · React 19 · TypeScript · PrimeReact · Tailwind 4 · Lucide

## Segurança

- **Autenticação JWT** stateless (`Authorization: Bearer`). Senhas com **bcrypt** (custo 10).
- **Autorização por papel** validada na camada de serviço (admin/líder por setor ou `report_to`).
- **Rate limit** por IP (10 tentativas / 15 min) + **lockout por conta** após falhas consecutivas, com limpeza agendada.
- **CORS** com allow-list, **HSTS**, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` e HTTPS opcional (`REQUIRE_HTTPS`).

## Como rodar

**Pré-requisitos:** Java 21+, Node.js 20+ e um banco PostgreSQL (ex.: Supabase).

### 1. Backend (porta 8080)

Crie `backend/.env` (lido automaticamente via spring-dotenv):

```env
# Conexão JDBC (Session Pooler do Supabase: Connect → JDBC → Session pooler)
SPRING_DATASOURCE_URL=jdbc:postgresql://<host-pooler>:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres.<project-ref>
SUPABASE_DB_PASSWORD=<senha-do-banco>
SUPABASE_SCHEMA=unavaliability
SESSION_SECRET=<string-aleatoria-de-32-ou-mais-caracteres>
```

```bash
cd backend
./mvnw spring-boot:run
```

No primeiro boot o **Flyway** aplica as migrations em `src/main/resources/db/migration`:
`V1` cria o schema e as tabelas; `V2` insere o admin master inicial.

### 2. Front (porta 3000)

`client/.env.local` aponta para o backend (default já configurado):

```env
BACKEND_API_URL=http://localhost:8080
```

```bash
cd client
npm install
npm run dev                  # dev em http://localhost:3000
npm run build && npm start   # produção
npm test                     # testes (utilitários de UI)
```

### Conta inicial (seed da migration V2)

| Email              | Senha      | Papel          |
| ------------------ | ---------- | -------------- |
| `rafael@gmail.com` | `admin123` | `admin_master` |

> Troque a senha após o primeiro acesso.

## Estrutura

```text
backend/                         # API Java / Spring Boot
  src/main/java/com/unavaliability/backend/
    models/        # entidades JPA (User, Member, Cliente, Evento, Unavailability, junções)
    repositories/  # Spring Data JPA
    service/       # regras de negócio + validação + permissões
    controllers/   # REST controllers (espelham /api/**)
    security/      # JWT, filtro, roles, UserDetails
    config/        # SecurityConfig (CORS, headers, HTTPS)
    exception/     # ApiException + handler global
  src/main/resources/
    application.yml
    db/migration/  # V1__init.sql, V2__seed_admin_master.sql (Flyway)

client/                          # Front-end Next.js
  app/
    (pages)/       # UI: admin (users/setores/members/clientes/eventos), unavailability, login, register
    api/           # BFF: proxies que repassam /api/** ao backend Java
    components/    # Navbar, NotificationBell, UnavailForm, UnavailCalendar, etc.
    lib/           # api-client (JWT), backend (proxy), client-config (UI utils)
  test/            # testes de utilitários de UI (Jest)
```

## Regras de negócio

- Indisponibilidade `prolongado` exige mínimo **5 dias úteis**.
- Data de início ≥ **hoje + 15 dias**.
- Sem sobreposição com pedidos próprios pendentes/aprovados.
- Cota anual de dias resetada por ano civil.
- `admin_master` pode aprovar a própria solicitação; demais não.

## Migrations

O schema é versionado com **Flyway** em `backend/src/main/resources/db/migration`. Para alterar o banco, crie um novo arquivo `V<n>__descricao.sql` — nunca edite uma migration já aplicada (o Flyway valida o checksum).
