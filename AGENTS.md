# petrilens-station

Estação IoT de captura de placas de Petri: trabalho final da disciplina de IoT. ESP32 simulado no Wokwi web, servidor Java/Spring com painel web. Prazo final: 23/11/2026.

Antes de trabalhar, leia:

- `docs/proposta.md`: escopo aprovado pelo professor (não editar).
- `docs/entrega.md`: itens de entrega, responsáveis e checklist.
- `docs/adr/`: decisões difíceis de reverter.
- `docs/decisoes.md`: demais decisões de comportamento e implementação.
- `GLOSSARY.md`: termos do domínio.
- `docs/uso-ia.md`: regra da disciplina sobre IA; registre ali o que for produzido com apoio de IA.
- `docs/enunciado/`: enunciado e especificação do professor (só local, não versionados).

## Diretriz: simples e funcionando

O objetivo é atender ao trabalho com o mínimo que funcione: requisitos R1 a R5, o que a proposta aprovada promete e os itens baratos do adicional de segurança. Na dúvida entre duas soluções, escolha a mais simples de implementar e de explicar na defesa. Não adicione funcionalidade, abstração ou configuração que não foi pedida; melhorias ficam para depois que o fluxo completo estiver rodando.

Mantenha as responsabilidades separadas e o código agnóstico à ferramenta de simulação e ao transporte: só um módulo do firmware conhece a URL e o protocolo do servidor, e o servidor não sabe se há túnel na frente dele. Assim, trocar Wokwi, ngrok ou o protocolo afeta um ponto só (ADR 0003).

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues at `pablovns/petrilens-station` (via `gh`). See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
