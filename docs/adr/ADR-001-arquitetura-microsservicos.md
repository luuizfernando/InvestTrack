# ADR-001 — Adotar Arquitetura de Microsserviços

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-06-01 |
| **Decisores** | Time InvestTrack |

---

## Contexto

A plataforma InvestTrack precisa lidar com diferentes domínios de negócio com características distintas: gestão de produtos financeiros, processamento de ordens, consolidação de carteiras e envio de notificações. Cada domínio possui:

- Requisitos de escalabilidade diferentes (ex: ordens têm picos durante o pregão);
- Modelos de dados distintos;
- Frequências de deploy independentes;
- Times de desenvolvimento que podem evoluir em paralelo.

A alternativa mais simples seria uma aplicação monolítica com todos os módulos integrados.

---

## Decisão

Adotar **arquitetura de microsserviços**, dividindo a plataforma em serviços independentes por domínio de negócio:

| Serviço | Domínio |
|---|---|
| `admin-service` | Administração de produtos e usuários |
| `order-service` | Ordens de compra e venda |
| `portfolio-service` | Posições e carteiras consolidadas |
| `notification-service` | Notificações e comunicação com usuários |
| `gateway` | Borda: roteamento, autenticação e segurança |
| `bff-node` | Agregação e adaptação para o cliente |

Cada serviço é implantado, escalado e evoluído de forma independente.

---

## Consequências

### Positivas

- **Escalabilidade granular:** serviços com alta demanda (ex: `order-service` no pregão) escalam independentemente.
- **Isolamento de falhas:** a queda de um serviço não derruba toda a plataforma.
- **Autonomia de deploy:** cada domínio pode ser atualizado sem janela de manutenção geral.
- **Tecnologia por domínio:** liberdade para escolher a stack mais adequada a cada serviço (ex: BFF em Node.js).

### Negativas / Riscos

- **Complexidade operacional:** mais serviços exigem orquestração, observabilidade centralizada e service discovery.
- **Latência de rede:** chamadas inter-serviço introduzem overhead inexistente em monolitos.
- **Consistência eventual:** transações distribuídas são mais complexas; optamos por eventos assíncronos onde possível.
- **Overhead de infraestrutura:** necessidade de Gateway, mensageria, pipelines de log e métricas desde o início.

### Mitigações adotadas

- Spring Cloud Gateway centraliza o roteamento e autenticação, reduzindo acoplamento entre cliente e serviços.
- RabbitMQ permite comunicação assíncrona e desacoplada entre serviços.
- ELK + Prometheus/Grafana fornecem observabilidade centralizada desde o início.
- Testcontainers garante testes de integração isolados por serviço.
