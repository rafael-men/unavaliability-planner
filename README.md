# Sistema de Indisponibilidade

Aplicação web para controle de períodos de indisponibilidade de escala de trabalho com fluxo de aprovação hierárquica, gestão de clientes/eventos, detecção de conflitos em tempo real, métricas de observabilidade e auditoria completa.

---

## O que a aplicação faz

- **Solicitação de indisponibilidade** — tipo prolongado (mínimo 5 dias úteis) ou pontual (1 dia), com antecedência mínima de 15 dias e validação de sobreposição no banco.
- **Aprovação hierárquica** — líder do setor aprova pedidos do seu time (via departamento ou cadeia `report_to`); administradores aprovam qualquer pedido.
- **Cadastro de clientes e vínculos** — admins criam clientes e alocam/desalocam usuários a cada um.
- **Cadastro de eventos e clientes envolvidos** — admins criam eventos com intervalos de datas e clientes vinculados.
- **Detecção de conflitos em tempo real** — quando um usuário alocado a um cliente pede indisponibilidade durante um evento desse cliente, o líder vê banner de alerta na pendência e células destacadas no calendário.
- **Calendário consolidado** — visão unificada de indisponibilidades aprovadas, pendentes, eventos e conflitos de escala.
- **Notificações** — sineta na navbar com badge de contagem otimizada para novos cadastros e pedidos pendentes.
- **Gestão de usuários, setores e membros** — administração completa com permissões granulares por papel.
- **Dashboard analítico** — dias de indisponibilidade por setor e por mês, taxa de aprovação, tempo médio de aprovação e ranking de conflitos por cliente.
- **Forecast de capacidade** — projeção de colaboradores indisponíveis por semana nas próximas N semanas (1 a 26 semanas).
- **Exportação completa** — relatório de indisponibilidades e calendário em **CSV, Excel (XLSX) e PDF**.
- **Cancelamento e retorno antecipado** — admin/líder pode cancelar ou encurtar períodos aprovados; os dias voltam para a cota e o colaborador é notificado por e-mail.
- **Linha do tempo e auditoria** — histórico imutável de cada solicitação (criada → aprovada/rejeitada → cancelada/antecipada), registrando autor e timestamp.
- **Recuperação de senha segura por tickets** — "Esqueci a senha" cria um chamado com resposta neutra (anti-enumeração de usuários); o admin autoriza no painel e o sistema gera token seguro de uso único (SHA-256, 30 min) enviado por **e-mail**.
- **Notificações por e-mail** — disparadas via Resend SMTP para redefinição de senha e cancelamentos/antecipações de períodos (com fallback para log em desenvolvimento).
- **Garantia de não-sobreposição concorrente** — constraint `EXCLUDE USING gist` com extensão `btree_gist` no PostgreSQL impedindo requisições simultâneas sobrepostas.
- **Otimização de banco com índices compostos** — queries críticas de calendário, validação de sobreposição, paginação e analytics totalmente indexadas.
- **Métricas e observabilidade** — endpoints Spring Actuator e Prometheus em porta dedicada (`8081`) com logs estruturados em formato ECS.
- **Documentação interativa da API** — Swagger UI (OpenAPI 3) com autenticação Bearer.
- **UI responsiva** — tema "liquid glass" (gradiente verde petróleo e vidro fosco), suporte a telas móveis com menu hambúrguer.

---

## Papéis de Acesso

| Papel | Permissões |
| :--- | :--- |
| `admin_master` | Acesso irrestrito a todos os módulos, configurações, usuários e aprovações. |
| `admin_editor` | Gerenciamento de clientes, eventos, setores, membros e resolução de tickets. |
| `admin_leitor` | Visualização de relatórios, calendário e dashboard sem permissão de alteração. |
| `socio` | Acesso de leitura executiva completa a relatórios, calendário e analytics. |
| `lider` | Gestão e aprovação de pedidos dos membros sob sua liderança (`report_to` ou setor). |
| `colaborador` | Criação e acompanhamento de solicitações de indisponibilidade próprias. |

---

## Arquitetura

A aplicação é dividida em dois projetos:

- **`backend/`** — API REST em **Java 21 / Spring Boot 4** (Spring Security + JWT, JPA/Hibernate, Flyway, Actuator, Resend Mail). Centraliza 100% da lógica de negócio, auditoria e persistência no PostgreSQL.
- **`client/`** — front-end **Next.js 16** (App Router, React 19, Tailwind CSS 4, PrimeReact, Lucide). As rotas em `client/app/api/**` atuam como BFF (Backend for Frontend), repassando as requisições autenticadas ao backend Java.

```text
Navegador → /api/... (Next.js BFF / proxy)
             ↓
         http://localhost:8080/api/... (Spring Boot)
             ↓
         PostgreSQL (Aiven / Supabase / Local)
```

---

## Stack Tecnológica

- **Backend:** Java 21 · Spring Boot 4 (Web MVC, Data JPA, Security, Actuator, Mail) · JWT (`jjwt`) · Flyway · PostgreSQL · springdoc/OpenAPI (Swagger) · Apache POI (Excel) · OpenPDF · Micrometer / Prometheus · Maven
- **Frontend:** Next.js 16 (App Router) · React 19 · TypeScript · Tailwind CSS 4 · PrimeReact · Lucide Icons
- **Testes:** JUnit 5 + Mockito + AssertJ (backend) · Jest (frontend)

---

## Segurança

- **Autenticação JWT Stateless:** Tokens emitidos com assinatura HMAC-SHA (`Authorization: Bearer <token>`).
- **Senhas Criptografadas:** Algoritmo `bcrypt` com custo 10 e políticas de complexidade (mínimo 6 caracteres, letras e números).
- **Proteção contra Força Bruta:** Rate limiting por IP (10 requisições / 15 min) e bloqueio temporário de conta após falhas consecutivas.
- **Headers HTTP Defensivos:** HSTS, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` e CORS com allow-list restrita.
- **Recuperação Anti-Enumeração:** A rota pública de esqueci a senha retorna resposta genérica idêntica independentemente do e-mail existir ou não.
- **Tokens de Redefinição Temporários:** Tokens aleatórios criptograficamente seguros de 32 bytes codificados em URL Base64, armazenados em hash SHA-256, com expiração de 30 minutos e invalidação imediata após o uso.

---

## Banco de Dados & Migrations (Flyway)

O banco de dados é versionado e gerenciado automaticamente pelo **Flyway** (`backend/src/main/resources/db/migration`):

| Migração | Descrição |
| :--- | :--- |
| `V1__init.sql` | Criação do schema base: tabelas `members`, `users5`, `clientes`, `eventos`, `unavailability`, `user_clientes` e `evento_clientes`. |
| `V2__seed_admin_master.sql` | Inicialização controlada do primeiro usuário administrador master. |
| `V3__unavailability_audit.sql` | Tabela de auditoria e histórico de ciclo de vida das solicitações. |
| `V4__password_tickets.sql` | Tabela de tickets de recuperação de senha por aprovação administrativa. |
| `V5__notification_count_indexes.sql` | Índices para contagem rápida de notificações de pendências. |
| `V6__analytics_indexes.sql` | Índices cobrindo agrupamentos e filtros do dashboard analítico. |
| `V7__password_reset_token.sql` | Campos `token_hash`, `token_expires_at` e `token_used_at` com índice único seguro. |
| `V8__unavailability_no_overlap.sql` | Extensão `btree_gist` e restrição de exclusão impedindo sobreposições concorrentes no banco. |
| `V9__composite_indexes.sql` | Índices compostos estratégicos para `unavailability(status, start_date, end_date)`, `unavailability(user_id, status)`, `unavailability(created_at DESC)`, `members(report_to)` e `eventos(data_inicio, data_fim)`. |

---

## Como Rodar o Projeto

### Pré-requisitos
- **Java 21** e **Maven** (ou use o wrapper `./mvnw` incluso)
- **Node.js 20+** e **npm**
- Instância do **PostgreSQL** ativa (Aiven Cloud, Supabase ou local)

---

### 1. Configurando o Backend

Crie o arquivo `backend/.env` com as configurações do ambiente:

```env
# Banco de Dados PostgreSQL (ex: Aiven Cloud)
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<porta>/<database>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<senha>
DB_SCHEMA=unavaliability
SPRING_DATASOURCE_POOL_SIZE=5

# Segurança JWT
SESSION_SECRET=<chave-secreta-de-pelo-menos-32-caracteres>

# E-mail (Resend SMTP)
RESEND_HOST=smtp.resend.com
RESEND_PORT=587
RESEND_USER=resend
RESEND_API_KEY=re_sua_api_key_aqui
RESEND_FROM=onboarding@resend.dev  # Em produção: seu-email@seu-dominio-verificado.com

# Administrador Inicial
ADMIN_EMAIL=admin@empresa.com
ADMIN_PASSWORD=SenhaForte123
INITIAL_ADMIN_NAME=Admin Master

# URL do Frontend (para links de redefinição de senha)
APP_FRONTEND_URL=http://localhost:3000
```

Para iniciar o backend:

```powershell
cd backend
./mvnw spring-boot:run
```

- A API estará disponível em: `http://localhost:8080`
- Documentação interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`
- Métricas Actuator/Prometheus: `http://localhost:8081/actuator/prometheus`

---

### 2. Configurando o Frontend

Crie o arquivo `client/.env.local`:

```env
BACKEND_API_URL=http://localhost:8080
```

Instale as dependências e inicie o servidor Next.js:

```powershell
cd client
npm install
npm run dev
```

Acesse o sistema no navegador em: **`http://localhost:3000`**

---

## Endpoints Notáveis da API

### Autenticação & Recuperação de Senha
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Login com e-mail e senha, retornando token JWT e perfil do usuário. |
| `POST` | `/api/auth/register` | Auto-cadastro de novos colaboradores (requer aprovação posterior de admin). |
| `GET` | `/api/auth/me` | Retorna os dados do usuário autenticado atual. |
| `POST` | `/api/auth/forgot-password` | Público. Abre chamado de recuperação de senha com resposta neutra. |
| `GET` | `/api/admin/tickets?onlyOpen=` | Restrito a admins. Lista os tickets de redefinição pendentes. |
| `POST` | `/api/admin/tickets/{id}/resolve` | Restrito a admins. Gera token seguro e envia o link de redefinição por e-mail. |
| `POST` | `/api/auth/reset-password` | Público. Recebe o token do link e a nova senha para redefinição. |

### Indisponibilidades & Auditoria
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/unavailability` | Lista indisponibilidades com filtros por status, setor e data. |
| `POST` | `/api/unavailability` | Cria nova solicitação com validação de dias úteis e conflitos. |
| `POST` | `/api/unavailability/{id}/approve` | Aprovação hierárquica por líder ou admin. |
| `POST` | `/api/unavailability/{id}/reject` | Rejeição de solicitação informando justificativa. |
| `POST` | `/api/unavailability/{id}/cancel` | Cancela ou antecipa retorno (`{ "new_end_date": "YYYY-MM-DD" }`) com notificação por e-mail. |
| `GET` | `/api/unavailability/{id}/history` | Linha do tempo completa (auditoria) da solicitação. |

### Analytics & Exportações
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/analytics/dashboard?de=&ate=` | Indicadores de dias por setor, aprovações e conflitos. |
| `GET` | `/api/analytics/forecast?semanas=8` | Projeção semanal de pessoas ausentes. |
| `GET` | `/api/export/relatorio?formato=csv\|xlsx\|pdf` | Exporta relatório gerencial de solicitações. |
| `GET` | `/api/export/calendario?formato=csv\|xlsx\|pdf` | Exporta grade de calendário ativa formatada. |

---

## Executando os Testes

```powershell
# Testes do Backend (JUnit 5 + Mockito + Migrations)
cd backend
./mvnw test

# Testes do Frontend (Jest)
cd client
npm test
```

---

## Executando com Docker Compose

Para subir toda a infraestrutura com um único comando:

```powershell
docker compose up --build
```

O compose inicializa os contêineres do backend (porta 8080) e frontend (porta 3000) com verificações de healthcheck integradas.
