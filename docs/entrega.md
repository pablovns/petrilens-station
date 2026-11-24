# Entrega e organização do trabalho

Prazo sem tolerância: **23/11/2026 às 23:59**, entrega de todos os itens no Classroom, com o vídeo. A apresentação com demonstração ao vivo é em sala, na semana seguinte. Fonte: [enunciado](enunciado/enunciado.txt), [especificação](enunciado/especificacoes.pdf) e [aviso do professor](enunciado/aviso-apresentacao.txt).

## Itens de entrega

| Item | Formato |
|---|---|
| Código (firmware, circuito Wokwi, servidor, painel) | este repositório |
| README de execução, exemplo de credenciais, `.gitignore` | este repositório |
| Relatório técnico, 8 a 15 páginas | escrito em Markdown no repositório, entregue em PDF |
| Link do projeto Wokwi com compartilhamento ativo | [https://wokwi.com/projects/480987947564554241](https://wokwi.com/projects/480987947564554241) (público) |
| Vídeo de 3 a 5 minutos | arquivo de vídeo |
| Slides para até 10 minutos | arquivo de slides |
| Teste do notebook da demo seguindo só o README | — |
| Envio no Google Classroom (um envia, os outros marcam como concluída) | — |

O repositório é privado e o professor é adicionado como colaborador na entrega (ADR 0001).

## Relatório

Escrito em Markdown e convertido para PDF. A contagem de 8 a 15 páginas vale para o PDF gerado: conferir depois de cada conversão, porque capa, diagramas e tabelas mudam a paginação. Estrutura obrigatória (seção 6 da especificação): capa; problema e solução; arquitetura com diagrama das quatro camadas; hardware (diagrama de ligação, componentes, tabela de pinos); software; comunicação com exemplo real de payload; testes, incluindo ao menos uma falha e sua correção; conclusão; uso de IA; referências. Segurança vai em seção própria, para o adicional de até 10%.

## Vídeo

**Vai junto com a entrega de 23/11.** O professor avisou que não aceita o vídeo enviado depois: ele é a garantia caso a demonstração ao vivo falhe na apresentação.

Gravação de tela contínua, **sem narração e sem edição**, com 3 a 5 minutos. A especificação só exige a duração e que o vídeo mostre o sistema em funcionamento, e não apenas o código. Cortar só o início e o fim, se preciso, para caber na duração. O vídeo precisa mostrar o fluxo rodando: simulação no Wokwi, sensores respondendo, dado chegando no painel e comando voltando para a estação.

[Roteiro cronometrado e plano B da apresentação](roteiro-video.md)

## Apresentação

Em sala de aula, **na semana seguinte à entrega**, no formato **pitch**: explicar como a solução funciona e vender a ideia, sem detalhar código nem linguagem. Até 10 minutos, dividida igualmente entre os integrantes, com demonstração ao vivo; sem demonstração funcionando, a nota é descontada. O professor lê os trabalhos antes e leva perguntas preparadas a partir do relatório. A defesa é individual: qualquer integrante pode ser questionado sobre qualquer parte, então todos precisam entender todo o código. Fonte: [aviso do professor](enunciado/aviso-apresentacao.txt).

[Roteiro da apresentação, contingência e preparação para a defesa](roteiro-apresentacao.md)

## Checklist do anexo A da especificação

- [ ] Requisitos R1 a R5 implementados e testados.
- [ ] Sistema executa do início ao fim sem intervenção manual no código.
- [ ] Repositório sem senhas, chaves de API ou tokens.
- [ ] README descreve como executar o projeto em outra máquina.
- [ ] Relatório com o diagrama das quatro camadas e um exemplo real de payload.
- [ ] Vídeo mostra o sistema em funcionamento, e não apenas o código.
- [ ] Todos os integrantes sabem explicar qualquer parte do projeto.
