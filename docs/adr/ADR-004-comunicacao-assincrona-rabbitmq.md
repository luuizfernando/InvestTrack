# ADR-004 — Comunicação Assíncrona com RabbitMQ

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-06-01 |
| **Decisores** | Time InvestTrack |
| **Relacionado** | [ADR-001](ADR-001-arquitetura-microsservicos.md), [ADR-003](ADR-003-persistencia-poliglota.md) |

---

## Contexto

Alguns fluxos da plataforma envolvem múltiplos serviços em sequência, por exemplo:

> Criação de ordem → atualização de carteira → envio de notificação ao usuário

Implementar esses fluxos via chamadas HTTP síncronas encadeadas criaria:

- **Acoplamento temporal:** todos os serviços precisam estar disponíveis simultaneamente.
- **Acoplamento de contrato:** o `order-service` precisaria conhecer a API do `notification-service`.
- **Falha em cascata:** a indisponibilidade de qualquer serviço do fluxo derruba a operação inteira.

Alternativas avaliadas:

| Opção | Prós | Contras |
|---|---|---|
| HTTP síncrono encadeado | Simples de implementar | Acoplamento forte, falha em cascata |
| Apache Kafka | Alta throughput, log imutável, replay | Overhead operacional elevado; complexidade desnecessária no estágio atual |
| **RabbitMQ + AMQP** | Mensageria madura, baixa latência, suporte a streams | Menor throughput que Kafka para volumes muito altos |

---

## Decisão

Usar **RabbitMQ 4** como broker de mensagens para comunicação assíncrona entre serviços.

Padrão adotado: **publish/subscribe baseado em eventos de domínio**:

| Produtor | Evento | Consumidor |
|---|---|---|
| `order-service` | `OrderCreatedEvent` | `portfolio-service`, `notification-service` |
| `portfolio-service` | `PortfolioUpdatedEvent` | `notification-service` |

Protocolo: **AMQP** via `spring-boot-starter-amqp`.
Extensão: **Rabbit Streams** habilitado nos POMs de `admin-service`, `order-service` e `portfolio-service` para suporte futuro a replay de mensagens.

---

## Consequências

### Positivas

- Desacoplamento total entre produtores e consumidores — `order-service` não conhece `notification-service`.
- Resiliência: mensagens são persistidas no broker; consumidores podem processar no próprio ritmo.
- Facilita evolução: novos consumidores de um evento são adicionados sem alterar o produtor.
- Interface de gerenciamento (`:15672`) facilita debugging e monitoramento de filas.

### Negativas / Riscos

- **Consistência eventual:** o usuário pode receber a notificação segundos após a confirmação da ordem.
- **Ordering:** garantia de ordem de mensagens requer configuração explícita de filas.
- **Dead letter queues:** necessário definir política de DLQ para mensagens não processadas (a implementar).
- **Idempotência:** consumidores devem ser idempotentes para lidar com reentregas (at-least-once delivery).
