# ADR-003 — Persistência Poliglota: PostgreSQL + MongoDB

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-06-01 |
| **Decisores** | Time InvestTrack |
| **Relacionado** | [ADR-001](ADR-001-arquitetura-microsservicos.md) |

---

## Contexto

Os domínios da plataforma possuem características de dados distintas:

| Domínio | Natureza dos dados |
|---|---|
| Ordens, carteiras, produtos | Estruturados, relacionais, com integridade transacional |
| Notificações | Semiestruturados, schema variável por canal/tipo, sem joins |

Usar um único banco de dados relacional para todos os serviços criaria acoplamento de schema entre domínios e forçaria estrutura rígida onde flexibilidade é necessária.

---

## Decisão

Adotar **persistência poliglota**:

- **PostgreSQL 14** para `admin-service`, `order-service` e `portfolio-service`:
  - Dados transacionais com integridade referencial (ACID).
  - Acesso via **Spring Data JPA** / Hibernate.
  - Cada serviço possui seu próprio schema — sem banco compartilhado entre serviços.

- **MongoDB 8** para `notification-service`:
  - Documentos de notificação com estrutura variável por tipo (email, push, SMS).
  - Acesso via **Spring Data MongoDB**.
  - Facilita armazenamento de histórico e payloads heterogêneos.

---

## Consequências

### Positivas

- Cada serviço evolui seu modelo de dados de forma independente.
- PostgreSQL oferece transações ACID completas para operações financeiras críticas.
- MongoDB flexibiliza o schema de notificações sem migrações complexas.
- Alinhamento com o padrão **Database per Service** de microsserviços.

### Negativas / Riscos

- **Sem joins entre serviços:** consultas que cruzam domínios requerem agregação na camada de aplicação ou eventos assíncronos.
- **Consistência eventual:** operações que envolvem mais de um banco exigem cuidado (ex: saga pattern) para evitar inconsistências.
- **Operacional:** dois motores de banco distintos exigem conhecimento, monitoramento e backup separados.
- **Testcontainers:** os testes de integração já refletem esta decisão — `PostgreSQLContainer` e `MongoDBContainer` são provisionados independentemente por serviço.
