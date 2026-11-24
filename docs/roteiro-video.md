# Roteiro do vídeo de entrega

**Duração-alvo de 4 min.** Grave a tela em uma tomada contínua, sem narração. Se precisar, corte apenas o início e o fim para ficar entre 3 e 5 min.

## Antes de gravar

- [ ] Suba o servidor conforme o README. Deixe o [painel](http://localhost:8080) autenticado em A1, com a tabela de capturas acessível. [Configuração da demo](../README.md#simular-no-wokwi-para-vs-code)
- [ ] Prepare a licença e compile o sketch conforme o README. Abra `firmware/sketch` no VS Code com a simulação parada. Comece com o berço vazio e o painel indicando offline.
- [ ] Ponha Wokwi e painel lado a lado, de preferência em **1920 × 1080**. Teste a legibilidade dos campos, LEDs e miniaturas. Use OBS ou ajuste o limite padrão de 30 s do gravador nativo do Ubuntu para caber a tomada.
- [ ] Use somente dados e imagem fictícios. Deixe credenciais fora da tela, com `secrets.h` e `.env` fechados.
- [ ] Antes da tomada, faça a conferência rápida do firmware atual (etapa 1 do [roteiro de testes](roteiro-wokwi.md): P2, F1 e C1): estação online, placa acendendo os 10 segmentos e uma captura pelo botão com a imagem idêntica à fixture. Essa captura fica na amostra selecionada; anote a contagem de A5 antes de gravar para conferir a linha nova no vídeo.

## Gravação

Grave no Wokwi para VS Code com o gateway privado local. Faça uma ação por vez e espere a evidência antes de avançar. A estação sincroniza a cada 5 s e o painel consulta a cada 2 s. [Passos P2, F1, F3, F4, F6, F8 e L2 do ensaio](roteiro-wokwi.md)

| Tempo | Ação | O que deve aparecer |
|---|---|---|
| 0:00–0:40 | Inicie a simulação com o berço vazio. | `est-01: online` e horário da última sincronização avançando. RGB amarelo e LED bar apagado. |
| 0:40–1:00 | Coloque a placa pela chave de presença, na posição ligada ao terminal 1. | LED bar acende com 10 segmentos, RGB fica verde e **Placa no berço** indica **Presente**. |
| 1:00–1:25 | Mova o controle do LDR e deixe a leitura perto de 500 lx. | **Luz medida** muda no painel. Confira a leitura indicada pela estação. |
| 1:25–1:50 | Escolha A5 do lote L2026-002 e clique em **Selecionar**. Aguarde até duas sincronizações e a atualização do painel. | **Seleção no servidor** e **Amostra informada pela estação** mostram A5. |
| 1:50–2:15 | Pressione o botão no Wokwi e solte ao fim da captura. | RGB passa de verde a azul e volta a verde. Surge uma linha em A5 com horário, lux e miniatura. |
| 2:15–2:40 | Digite `4` em **Brilho (0 a 10)** e clique em **Ajustar brilho**. | Comando `entregue`, quatro segmentos acesos no Wokwi e **Brilho configurado** em `4 de 10 segmentos` na sincronização seguinte. |
| 2:40–3:05 | Clique uma vez em **Capturar** no painel. | Comando `entregue`, RGB verde → azul → verde e outra miniatura em A5. |
| 3:05–3:40 | Ajuste o LDR para cerca de 250 lx. Confirme no painel a leitura abaixo de 300 lx e pressione o botão no Wokwi. | RGB vermelho por cerca de 3 s e volta ao verde. O painel mostra **Captura recusada: luz fora da faixa ideal.** A lista de capturas fica igual. |
| 3:40–4:00 | Retire a placa pela chave de presença. | LED bar apaga, RGB volta a amarelo e **Placa no berço** indica **Ausente**. |

## Plano B da apresentação

Se a simulação local falhar na apresentação, exiba o vídeo salvo no notebook e demonstre ao vivo o servidor e o painel. Prepare o ambiente de teste e as variáveis em outro terminal conforme o README, com o JAR rodando e o painel autenticado. Da raiz do repositório, execute `./scripts/teste-local.sh`. [Servidor e prova local](../README.md#servidor-e-prova-local)

O script envia sincronizações em nome da estação e registra a imagem fictícia. Deixe o painel visível ao rodar o comando para mostrar `est-01: online` e A5 na seleção e na amostra informada. A nova miniatura em A5 continua visível quando o painel voltar a `offline`, após mais de 15 s sem sincronizar.

Explique que o simulador usa o gateway privado para alcançar o servidor no próprio notebook. A prova local usa `localhost` e demonstra o recebimento e a exibição dos dados. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)
