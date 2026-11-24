# Roteiro da apresentação

Apresentação em sala, na semana seguinte à entrega, no formato **pitch**: explicar como a solução funciona e vender a ideia, sem detalhar código nem linguagem (aviso do professor em [`docs/enunciado/aviso-apresentacao.txt`](enunciado/aviso-apresentacao.txt)). Até **10 min** com demonstração ao vivo, divididos entre os três integrantes, seguidos das perguntas do professor, preparadas a partir do relatório. Os slides estão no deck do grupo, com notas de fala em cada slide do pitch. Os anexos (A1 a A7) ficam depois do encerramento e só aparecem se uma pergunta pedir: eles mostram a evidência, e a explicação vem de quem responde. A especificação exige o sistema executando ao vivo: código compilando, sensores respondendo e dados circulando. [Roteiro do vídeo](roteiro-video.md) · [Roteiro de testes](roteiro-wokwi.md)

## Divisão

Os blocos têm durações próximas. O grupo define quem é A, B e C antes do ensaio.

| Bloco | Slides | Tempo | Quem fala | Quem opera |
|---|---|---|---|---|
| 1 | 1 Capa · 2 Problema · 3 A solução · 4 Como funciona | 2:45 | A | A (deck) |
| 2 | 5 Do sensor ao painel · 6 Confiança · 7 Testado | 2:05 | B | B (deck) |
| 3 | 8 Demonstração · 9 Vídeo (plano B) · 10 Próximos passos · 11 Encerramento | 4:00 | C | C (VS Code e painel) |

Na demonstração, quem opera chave, botão e LDR também narra, seguindo a coluna "O que falar" da demonstração.

## Na véspera, na sala ou na rede da apresentação

1. Ligue o notebook da demo **na rede que será usada**. Inicie a simulação com a aba do simulador visível e confira `est-01: online` no painel. O tooltip do Wi-Fi do Wokwi pode dizer "Public IoT Gateway" mesmo com a estação alcançando o servidor; vale o painel.
   - Se o painel não ficar online, a estação não alcança o servidor (veja as [particularidades do simulador](../README.md#particularidades-do-simulador)). Teste outra rede liberada pelo local ou o roteador do celular de um integrante. Não contorne restrições da rede da instituição.
   - Se nenhuma rede funcionar, a demonstração ao vivo segue o plano B (abaixo) e o grupo avisa o professor no início.
2. Faça o ensaio cronometrado completo, com troca de apresentadores, pelo menos uma vez.
3. Confirme que o vídeo de entrega está salvo **no notebook**, fora da nuvem, e abre no player.

## 30 minutos antes

1. Notebook na tomada, notificações silenciadas e só as janelas da demo abertas. `.env` e `secrets.h` fechados.
2. Na raiz: `java -jar servidor/target/servidor-0.0.1-SNAPSHOT.jar`. Abra o painel em http://localhost:8080, autentique-se e selecione **A1**.
3. Compile uma vez: `arduino-cli compile --fqbn esp32:esp32:esp32doit-devkit-v1 --build-path firmware/sketch/build firmware/sketch`. Isso deixa o cache pronto e a compilação ao vivo fica curta.
4. No VS Code, em `firmware/sketch`, inicie a simulação com a aba visível e confira `est-01: online` no painel.
5. Deixe o circuito no estado inicial: berço vazio (RGB amarelo, barra apagada) e LDR perto de **500 lx**.
6. Arrume a tela de demonstração: Wokwi à esquerda, painel à direita e um terminal pequeno na raiz do repositório, com o comando de compilação já digitado.
7. Abra o deck em modo apresentação no slide 1 e o vídeo pausado no início, em outra janela.

## Durante a apresentação

| Tempo | Slide | Fala principal (notas do slide têm o detalhe) |
|---|---|---|
| 0:00 | 1 Capa | Grupo e ideia em uma frase. |
| 0:15 | 2 Problema | Foto de celular varia e não dá para comparar placas. |
| 1:00 | 3 A solução | Padrão, registro ligado à amostra e simplicidade para o técnico. |
| 1:45 | 4 Como funciona | Os quatro passos na bancada; o LED mostra o estado. **Troca A → B.** |
| 2:45 | 5 Do sensor ao painel | As quatro camadas, sem detalhe técnico. |
| 3:30 | 6 Confiança | Foto protegida no caminho, painel com senha, senhas fora do código. |
| 4:10 | 7 Testado | Testes automáticos, foto idêntica, falhas corrigidas. **Troca B → C.** |
| 4:50 | 8 Demonstração | Diga em uma frase o que vai mostrar e saia do deck para a tela da demo (abaixo). Se a demo falhar, use o slide 9 (vídeo). |
| 7:50 | 10 Próximos passos | Câmera real, luz calibrada, contagem automática de colônias. |
| 8:30 | 11 Encerramento | Frase final e perguntas. |
| 8:45 | — | Perguntas (podem usar os anexos A1 a A7). |

### Demonstração ao vivo (cerca de 3 min)

Quem opera a simulação também narra: cada ação vem com uma fala curta, dirigida ao professor, apontando o que muda na tela. Uma ação por vez; fale enquanto espera a evidência aparecer (a estação sincroniza a cada 5 s e o painel atualiza a cada 2 s). Antes de começar: simulação parada, chave de presença à **direita** (berço vazio), LDR perto de 500 lx, painel em A1 mostrando **offline**.

| Tempo | Ação | Evidência | O que falar |
|---|---|---|---|
| 0:00 | Aponte a tela dividida. | Wokwi à esquerda, painel à direita, `est-01: offline`. | "À esquerda está a estação: um ESP32 com sensor de presença, sensor de luz, botão, a barra de LEDs que ilumina a placa e um LED colorido de status. À direita, o painel web que o laboratório usa. Agora a estação está desligada, por isso o painel mostra offline." |
| 0:20 | Rode o comando de compilação (ou **Start Simulator**, se o tempo estiver curto). | `Sketch uses …`; a simulação reinicia sozinha. | "Este é o firmware compilando agora, sem nada pré-gravado. Ao ligar, a estação entra no Wi-Fi e passa a conversar com o servidor a cada 5 segundos." |
| 0:40 | Aguarde. | `est-01: online` em verde; horário avançando; RGB amarelo. | "Pronto: online. Quem inicia a conversa é sempre a estação; ela manda o estado dos sensores e recebe os comandos pendentes. O LED amarelo diz 'conectada, sem placa'." |
| 1:00 | Chave de presença para a **esquerda**. | 10 segmentos acesos, RGB verde, painel **Presente**. | "Coloquei a placa no berço. Na estação real seria um sensor magnético; aqui é esta chave. A luz acende na hora, porque essa regra roda na própria estação, sem depender da rede. Verde quer dizer 'pronta para capturar'." |
| 1:15 | No painel, escolha **A5** e clique em **Selecionar**. | Seleção e "Amostra informada pela estação" em A5. | "O técnico escolhe no painel qual amostra está fotografando. Na próxima conversa, a estação recebe essa escolha, e toda foto fica ligada à amostra certa." |
| 1:35 | Pressione o **botão azul** uma vez. | Verde → azul → verde; linha nova em A5 com miniatura. | "Captura pelo botão da bancada. Azul é a estação lendo a imagem e enviando. A foto vai cifrada, então só o servidor consegue abrir. Aqui está ela no painel, com horário e a luz medida no momento." |
| 2:00 | Digite **4** em Brilho e clique em **Ajustar brilho**. | Comando `entregue`; 4 segmentos acesos; painel "4 de 10". | "O painel também manda comandos. Pedi brilho 4: o comando fica na fila e a estação o recebe na próxima conversa. 'Entregue' quer dizer que ela já recebeu." |
| 2:20 | No LDR, baixe para cerca de **250 lx** e confira a leitura no painel. **Com a placa ainda presente**, aperte o botão. | RGB vermelho por ~3 s; aviso **Captura recusada: luz fora da faixa ideal.**; nenhuma linha nova. | "Agora simulei pouca luz. A estação só aceita fotos entre 300 e 700 lux, para que todas sejam comparáveis. Ela recusa na hora, acende o vermelho e avisa o painel." |
| 2:45 | Chave de presença para a **direita**. | LEDs apagados, RGB amarelo, painel **Ausente**, ainda online. | "Retirei a placa: a luz apaga e a estação volta a esperar. Ela continua online, porque só a placa saiu; offline seria a estação perder a conexão." |
| 3:00 | Volte ao deck. | — | "Esse é o ciclo completo: placa, luz, captura ligada à amostra e controle remoto pelo painel." |

Se sobrar tempo, mostre **Capturar** pelo painel: "a mesma captura pode ser pedida de longe; o comando é entregue uma única vez".

### Fala sobre o vídeo (plano B)

Use se a demonstração ao vivo falhar. O vídeo de entrega (3:11, sem som) foi gravado em 23/11 e mostra o fluxo principal. Narre por cima, em tom de "aqui vocês veem…". Os tempos abaixo são do vídeo; dá para começar em 0:30 e acelerar os trechos de espera.

| Tempo no vídeo | O que aparece | O que falar |
|---|---|---|
| 0:00–0:30 | Painel offline e simulação parada. | "Gravamos a demonstração como garantia. À esquerda o painel do laboratório, à direita a estação simulada, ainda desligada." |
| 0:30–0:45 | Simulação inicia; painel fica **online**; a placa já está no berço (10 LEDs, RGB verde). | "Ao ligar, a estação conecta ao Wi-Fi e passa a falar com o servidor a cada 5 segundos. A placa já estava no berço, então a luz acende e o LED fica verde." |
| 0:45–1:00 | Luz medida passa a ~478 lx. | "O sensor de luz mede a iluminação: perto de 480 lux, dentro da faixa de 300 a 700." |
| 1:00–1:10 | Seleção de **A5** salva no painel. | "O técnico escolhe a amostra A5; a estação recebe essa escolha na próxima conversa." |
| 1:20–1:40 | Botão: RGB azul e duas capturas em A5 (o botão foi pressionado duas vezes). | "Captura pelo botão: o azul é o envio. Cada toque gera uma foto cifrada, que aparece no painel com horário e luz." |
| 1:50–2:05 | "Ajustar brilho (4)" **entregue**; barra com 4 segmentos. | "Comando pelo painel: brilho 4, entregue na conversa seguinte." |
| 2:05–2:20 | **Capturar** entregue; terceira foto em A5. | "A mesma captura pedida de longe, pelo painel." |
| 2:40–2:50 | LDR ajustado para 263 lx. | "Aqui reduzimos a luz para abaixo da faixa; com a luz assim, uma nova captura seria recusada." |
| 2:50–3:11 | Placa retirada: **Ausente**, RGB amarelo, LEDs apagados, estação ainda online. | "Ao retirar a placa, a luz apaga e a estação volta a esperar, conectada." |

O vídeo não mostra o berço vazio no início nem a recusa por luz baixa; se o professor perguntar, explique a regra e mostre o anexo de testes.

## Contingência

Decida rápido e diga ao professor o que está acontecendo.

1. **Estação não fica online em 30 s após a compilação:** confira se a aba do simulador está visível (ela pausa quando oculta). Se nada mudar, vá para o passo 3.
2. **Simulação travada ou fechada:** F1 → **Wokwi: Start Simulator** uma vez. Se não estiver online em 30 s, vá para o passo 3.
3. **Plano B:** passe ao slide 9 e exiba o vídeo a partir de 0:30, por até 2:30, narrando com a [fala sobre o vídeo](#fala-sobre-o-vídeo-plano-b). Em seguida, com o servidor e o painel ainda no ar, rode `./scripts/teste-local.sh` conforme o [roteiro do vídeo](roteiro-video.md#plano-b-da-apresentação) para mostrar dados chegando ao painel ao vivo.
4. Volte ao slide 10 em até 7:50 para não estourar os 10 min.

## Defesa individual

A nota é individual e qualquer integrante pode ser questionado sobre qualquer parte. O pitch evita detalhes técnicos, mas as perguntas do professor vêm da leitura do relatório. Antes do dia, cada integrante deve saber responder sem consultar:

- **Firmware:** como o laço decide sincronizar; onde fica a regra de presença; por que 300–700 lx; o que acontece sem Wi-Fi; como funciona o debounce.
- **Hardware:** por que pull-ups externos em 34–36; por que não há Monitor Serial; o que o microSD substitui.
- **Comunicação:** por que Wi-Fi e HTTP; por que a estação inicia tudo (ADR 0005); o que vai no payload; como o comando chega.
- **Servidor:** como o painel calcula online/offline; como um comando é entregue uma só vez; o que a API valida na captura.
- **Segurança:** onde ficam as credenciais; a diferença entre `X-Api-Key` e HTTP Basic; por que cifrar a imagem se o trecho local é HTTP; os riscos aceitos.
- **Testes e IA:** a falha do TLS e como foi corrigida; o que foi feito com IA e como o grupo verificou.
