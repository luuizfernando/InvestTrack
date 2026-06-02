# ADR-005 — BFF em Node.js com TypeScript

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-06-01 |
| **Decisores** | Time InvestTrack |
| **Relacionado** | [ADR-001](ADR-001-arquitetura-microsservicos.md), [ADR-002](ADR-002-api-gateway-spring-cloud.md) |

---

## Contexto

O padrão **Backend for Frontend (BFF)** resolve o problema de múltiplos clientes (web, mobile, etc.) com necessidades de dados diferentes consumindo a mesma API de microsserviços. Sem um BFF, o cliente seria responsável por:

- Orquestrar chamadas a múltiplos serviços;
- Agregar e transformar as respostas;
- Lidar com diferenças de contrato entre versões de API.

A questão era: **em qual linguagem/runtime implementar o BFF?**

Alternativas avaliadas:

| Opção | Prós | Contras |
|---|---|---|
| Novo microsserviço Spring Boot | Consistência tecnológica com o backend | Verbosidade; overhead de JVM desnecessário para agregação |
| GraphQL (Apollo Federation) | Flexibilidade máxima para o cliente | Curva de aprendizado; complexidade para o estágio atual |
| **Node.js + TypeScript** | Leve, rápido para I/O, ecossistema rico, type-safety | Time deve dominar dois ecossistemas (JVM + Node) |

---

## Decisão

Implementar o BFF no módulo `bff-node` usando **Node.js com TypeScript 6** (strict mode).

Justificativas:

1. **Natureza do BFF é I/O bound:** Node.js é altamente eficiente para agregar chamadas HTTP paralelas a diferentes serviços.
2. **Type safety:** TypeScript 6 com `strict: true` e `exactOptionalPropertyTypes` garante contratos de API tipados no BFF.
3. **Separação de preocupações:** o BFF fica fora do ecossistema Spring, reforçando o isolamento entre camada de apresentação e domínio.
4. **Module system moderno:** configuração `"module": "nodenext"` com `verbatimModuleSyntax` adota as melhores práticas atuais do ecossistema TypeScript.

---

## Consequências

### Positivas

- BFF leve, sem overhead de JVM, ideal para agregação e transformação de dados.
- Contratos do cliente tipados em TypeScript reduzem bugs de integração.
- Facilidade de compartilhar tipos com um eventual frontend React (o `tsconfig.json` já inclui `"jsx": "react-jsx"`).
- Deploy independente do ecossistema Java.

### Negativas / Riscos

- **Dois ecossistemas:** o time precisa manter expertise em Java/Spring e Node.js/TypeScript.
- **Autenticação duplicada:** o BFF deve repassar e validar tokens JWT já verificados pelo gateway, exigindo cuidado na implementação.
- **Estado atual:** o `bff-node` está vazio (sem framework HTTP definido); a escolha de Express, Fastify ou Hono é uma decisão pendente.
- **Consistência de erros:** padronizar o formato de erro entre o BFF e os microsserviços requer contrato explícito.
