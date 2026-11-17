# Entrega e organização do trabalho

Prazo único e sem tolerância: **23/11/2026**, entrega de todos os itens e apresentação com demonstração ao vivo. Fonte: `docs/enunciado/` (não versionado).

## Itens de entrega

| Item | Formato | Responsável |
|---|---|---|
| Código (firmware, circuito Wokwi, servidor, painel) | este repositório | Pablo |
| README de execução, exemplo de credenciais, `.gitignore` | este repositório | Pablo |
| Relatório técnico, 8 a 15 páginas | escrito em Markdown no repositório, entregue em PDF | Pablo |
| Link do projeto Wokwi com compartilhamento ativo | link | Pablo |
| Vídeo de 3 a 5 minutos | arquivo de vídeo | Douglas e Entony |
| Slides para até 10 minutos | arquivo de slides | Douglas e Entony |
| Teste do notebook da demo seguindo só o README | — | Douglas e Entony |
| Envio no Google Classroom (um envia, os outros marcam como concluída) | — | Douglas ou Entony |

O repositório é privado e o professor é adicionado como colaborador na entrega (ADR 0001).

## Relatório

Escrito em Markdown e convertido para PDF. A contagem de 8 a 15 páginas vale para o PDF gerado: conferir depois de cada conversão, porque capa, diagramas e tabelas mudam a paginação. Estrutura obrigatória (seção 6 da especificação): capa; problema e solução; arquitetura com diagrama das quatro camadas; hardware (diagrama de ligação, componentes, tabela de pinos); software; comunicação com exemplo real de payload; testes, incluindo ao menos uma falha e sua correção; conclusão; uso de IA; referências. Segurança vai em seção própria, para o adicional de até 10%.

## Vídeo

Gravação de tela contínua, **sem narração e sem edição**, com 3 a 5 minutos. A especificação só exige a duração e que o vídeo mostre o sistema em funcionamento, e não apenas o código. Cortar só o início e o fim, se preciso, para caber na duração. O vídeo precisa mostrar o fluxo rodando: simulação no Wokwi, sensores respondendo, dado chegando no painel e comando voltando para a estação.

## Apresentação

Até 10 minutos, dividida igualmente entre Pablo, Douglas e Entony, com demonstração ao vivo. A defesa é individual: qualquer integrante pode ser questionado sobre qualquer parte, então os três precisam entender todo o código. Pablo repassa o código aos outros antes da apresentação.

## Checklist do anexo A da especificação

- [ ] Requisitos R1 a R5 implementados e testados.
- [ ] Sistema executa do início ao fim sem intervenção manual no código.
- [ ] Repositório sem senhas, chaves de API ou tokens.
- [ ] README descreve como executar o projeto em outra máquina.
- [ ] Relatório com o diagrama das quatro camadas e um exemplo real de payload.
- [ ] Vídeo mostra o sistema em funcionamento, e não apenas o código.
- [ ] Todos os integrantes sabem explicar qualquer parte do projeto.
