# ADR-002 — API Gateway com Spring Cloud Gateway

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-06-01 |
| **Decisores** | Time InvestTrack |
| **Relacionado** | [ADR-001](ADR-001-arquitetura-microsservicos.md) |

---

## Contexto

Com múltiplos microsserviços expostos, o cliente (BFF ou diretamente o frontend) precisaria conhecer os endereços e contratos de cada serviço individualmente. Isso gera:

- Acoplamento direto entre cliente e topologia interna;
- Ausência de ponto único para autenticação e autorização;
- Dificuldade de implementar cross-cutting concerns (rate limiting, CORS, logging de borda, circuit breaker).

Alternativas consideradas:

| Opção | Prós | Contras |
|---|---|---|
| NGINX / Kong | Performático, agnóstico à linguagem | Configuração externa ao ecossistema Spring; mais difícil de versionar junto ao código |
| Netflix Zuul 1 | Amplamente documentado | Bloqueante (não reativo); descontinuado pelo Netflix |
| **Spring Cloud Gateway (WebMVC)** | Integrado ao ecossistema Spring Boot; suporte a filtros em Java; configuração como código | Limitado ao ecossistema Java |

---

## Decisão

Usar **Spring Cloud Gateway na variante WebMVC** (síncrona, servlet-based) como único ponto de entrada da plataforma.

Responsabilidades atribuídas ao gateway:

1. **Roteamento** baseado em path para os microsserviços internos.
2. **Autenticação/Autorização** via Spring Security (JWT).
3. **Rate Limiting** com Redis como backend de contagem.
4. **CORS** centralizado.
5. **Observabilidade de borda:** métricas via Micrometer + Prometheus.

---

## Consequências

### Positivas

- Ponto único de entrada simplifica a configuração do cliente e do BFF.
- Filtros e rotas declarados em Java/YAML versionados no mesmo repositório.
- Integração nativa com Spring Security elimina duplicação de lógica de autenticação nos serviços internos.
- Redis como backend de rate limiting é compartilhado e horizontalmente escalável.

### Negativas / Riscos

- **Single point of failure:** o gateway em queda derruba o acesso a todos os serviços; mitigado por múltiplas instâncias e health checks.
- **Variante WebMVC vs. WebFlux:** a versão síncrona (WebMVC) é menos performática sob altíssima carga de I/O concorrente que a reativa (WebFlux). Escolhida para simplicidade neste estágio do projeto.
- Todos os serviços internos dependem implicitamente do contrato de roteamento do gateway.
