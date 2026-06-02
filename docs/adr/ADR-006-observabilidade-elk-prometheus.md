# ADR-006 — Observabilidade com ELK + Prometheus/Grafana

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-06-01 |
| **Decisores** | Time InvestTrack |
| **Relacionado** | [ADR-001](ADR-001-arquitetura-microsservicos.md) |

---

## Contexto

Em uma arquitetura de microsserviços, depurar e monitorar o sistema requer visibilidade em múltiplas dimensões:

| Pilar | Pergunta que responde |
|---|---|
| **Métricas** | O sistema está saudável? Qual a latência média? Quantas requisições por segundo? |
| **Logs** | O que exatamente aconteceu em determinado momento em determinado serviço? |
| **Traces** | Qual o caminho percorrido por uma requisição entre os serviços? |

Sem observabilidade centralizada, problemas em microsserviços são extremamente difíceis de diagnosticar — logs dispersos em cada serviço, sem correlação entre eles.

---

## Decisão

Adotar **duas stacks complementares** para cobrir métricas e logs:

### Métricas — Prometheus + Grafana

- **Prometheus** faz scraping das métricas expostas pelos serviços via endpoint `/actuator/prometheus` (Micrometer).
- **Grafana** consome o Prometheus como datasource e exibe dashboards consolidados.
- Todos os serviços Spring Boot incluem `micrometer-registry-prometheus` no classpath.

### Logs — ELK Stack

- **Logstash** recebe logs dos serviços via TCP (porta 5000, formato JSON) ou Beats (porta 5044).
- **Elasticsearch** indexa os logs com padrão diário (`investtrack-YYYY.MM.dd`).
- **Kibana** provê interface de busca, filtros e visualizações sobre os logs indexados.

### Por que não um único stack?

| Stack | Métricas | Logs | Observação |
|---|---|---|---|
| ELK sozinho | Possível, mas não idiomático | Excelente | Elasticsearch para métricas time-series é menos eficiente que Prometheus |
| Grafana + Loki | Bom | Bom | Loki é mais simples que ELK, mas menos poderoso para busca full-text |
| **Prometheus + ELK** | Excelente | Excelente | Melhor ferramenta para cada finalidade |

---

## Consequências

### Positivas

- Visibilidade completa do sistema sem instrumentação manual complexa (Micrometer abstrai o Prometheus).
- Logs estruturados em JSON no Elasticsearch permitem queries avançadas no Kibana.
- Dashboards do Grafana podem correlacionar métricas de diferentes serviços em um único painel.
- `spring-boot-actuator` expõe health, info e métricas prontas para consumo.

### Negativas / Riscos

- **Consumo de recursos:** ELK é pesado — Elasticsearch requer ao menos 512 MB de heap (configurado no compose). Em ambientes com pouca memória, pode competir com os microsserviços.
- **Versões do ELK:** Logstash e Kibana devem usar exatamente a mesma versão major do Elasticsearch (ambos em 8.13.4).
- **Traces distribuídos:** esta decisão cobre apenas métricas e logs. Tracing distribuído (ex: Zipkin, Jaeger, OpenTelemetry) não está contemplado neste momento e é uma lacuna para ambientes produtivos.
- **Segurança do Elasticsearch:** `xpack.security.enabled=false` está definido para facilitar o desenvolvimento local. **Deve ser habilitado antes de qualquer deploy em ambiente não local.**
- **Provisioning do Grafana:** os datasources e dashboards ainda precisam ser configurados em `infra/grafana/provisioning/`.
