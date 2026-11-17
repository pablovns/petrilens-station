# Uso de ferramentas de IA

## Regra da disciplina

A especificação (seção 8) permite IA "como apoio", com três condições: o relatório declara o que foi produzido com esse apoio e como o grupo verificou o resultado; todo o código entregue precisa ser compreendido pelos integrantes, o que é cobrado na defesa individual; e usar projeto de terceiros sem citar a fonte e sem contribuição própria é plágio (nota zero). A penalidade para quem não demonstrar domínio do trabalho é ajuste da nota individual, mesmo com o projeto funcionando.

## Práticas do grupo

- As decisões de projeto são do grupo e ficam registradas em `docs/adr/`; a IA executa e propõe.
- Commits feitos com apoio de IA mantêm a linha `Co-Authored-By`, coerente com esta declaração.
- Todo código passa por verificação (teste ou execução no Wokwi) antes de entrar na `main`.
- Este arquivo é atualizado a cada entrega e alimenta a seção "Uso de ferramentas de IA" do relatório.

## Registro

| Data | O que foi produzido com IA | Ferramenta | Como foi verificado | Quem verificou |
|---|---|---|---|---|
| 2026-11-17 | Análise do enunciado, pesquisa sobre o Wokwi (`docs/pesquisa/wokwi.md`), planejamento, estrutura do repositório, ADRs 0001–0004, este documento e `docs/entrega.md` | Claude Code | Leitura e aprovação das decisões; fontes da pesquisa conferidas nos links citados | Pablo |
| 2026-11-17 | Sessão de grill (decisões em `docs/decisoes.md`, ADRs 0005–0006, `GLOSSARY.md`) e spec publicada na issue #1 | Claude Code | Cada decisão foi tomada e aprovada pelo grupo durante a sessão; a spec foi sintetizada a partir delas | Pablo |
