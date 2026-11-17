# Monorrepo único para firmware, servidor e documentação

Firmware do ESP32, circuito do Wokwi, servidor, painel web e documentação (incluindo o relatório) ficam num único repositório, `petrilens-station`. A entrega pede um repositório com README, `.gitignore` e exemplo de credenciais, e o contrato do payload é compartilhado entre firmware e servidor: mudar os dois no mesmo commit evita desencontro. Vários repositórios trariam burocracia sem ganho para um grupo de 3 pessoas com 6 dias de prazo.

## Consequências

- O repositório é privado e a entrega é feita adicionando o professor como colaborador; a especificação aceita "repositório no GitHub ou pasta compactada" e não exige que seja público. Se ele virar público, os issues do GitHub também ficam públicos (não existe issue privado em repositório público).
- O enunciado e a especificação do professor ficam em `docs/enunciado/` como referência local e não são versionados (`.gitignore`).
