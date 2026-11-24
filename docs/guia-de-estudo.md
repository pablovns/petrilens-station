# Guia de estudo para a defesa individual

## 1. O projeto em 1 minuto

A PetriLens é uma estação IoT (Internet das Coisas) simulada para padronizar a captura de imagens de placas de Petri. O operador de laboratório escolhe uma amostra no painel, coloca a placa no berço e dispara pelo botão ou pelo painel. O ESP32 acende a iluminação conforme a presença, verifica a luz e envia a imagem ao servidor. Na demonstração, um JPEG fictício no microSD representa a câmera. O servidor guarda a imagem e registra sua ligação com a amostra. O painel permite acompanhar a estação e consultar as capturas. A contagem de colônias fica fora do escopo da estação. [Relatório](relatorio.md) · [Roteiro da defesa](roteiro-apresentacao.md#defesa-individual)

## 2. Mapa do código

Os arquivos do firmware ficam em `firmware/sketch/`. No servidor, os caminhos Java abaixo começam em `servidor/src/main/java/br/com/petrilens/` e os recursos em `servidor/src/main/resources/`.

| Área | Arquivo | Responsabilidade |
|---|---|---|
| Firmware | `sketch.ino` | Inicializa os módulos e coordena sensores, sincronização, comandos e botão. |
| Firmware | `sensores.cpp` | Lê presença, filtra oscilações do botão e estima a luz em lux. |
| Firmware | `sensores.h` | Define a leitura conjunta dos três sensores e suas funções públicas. |
| Firmware | `iluminacao.cpp` | Configura os pinos e acende a quantidade de segmentos da barra. |
| Firmware | `iluminacao.h` | Declara as funções de inicialização e atualização da barra. |
| Firmware | `status.cpp` | Escolhe a cor do RGB e controla a duração da indicação de erro. |
| Firmware | `status.h` | Declara as funções de estado do RGB e sinalização de erro. |
| Firmware | `rede.cpp` | Conecta o Wi-Fi, habilita reconexão e lê a força do sinal. |
| Firmware | `rede.h` | Declara as funções de Wi-Fi usadas pelos demais módulos. |
| Firmware | `cliente_servidor.cpp` | Configura o destino, troca JSON e transmite a captura cifrada. |
| Firmware | `cliente_servidor.h` | Define estado, amostra, tipos de comando e funções de comunicação. |
| Firmware | `captura.cpp` | Aplica as condições de captura e coordena leitura e envio. |
| Firmware | `captura.h` | Declara a captura e a consulta do indicador de recusa por luz. |
| Firmware | `imagem.cpp` | Abre o microSD, decodifica o arquivo e cifra o JPEG. |
| Firmware | `imagem.h` | Define o resultado cifrado e declara as funções de imagem. |
| Firmware | `secrets.example.h` | Modelo vazio de configuração local do cliente do servidor. |
| Simulação | `diagram.json` | Define peças e ligações do circuito. |
| Simulação | `libraries.txt` | Declara a biblioteca externa usada no sketch. |
| Simulação | `placa-base64.txt` | Guarda o JPEG fictício em Base64, representação textual de bytes. |
| Simulação | `wokwi.toml` | Indica à extensão do Wokwi qual binário carregar. |
| Simulação | `sdcard/placa-base64.txt` | Aponta para o arquivo usado no cartão da simulação local. |
| Java | `ServidorApplication.java` | Inicia o Spring, configura o relógio e reforça a validação dos tipos JSON. |
| Java | `amostra/AmostraController.java` | Expõe o catálogo e recebe mudanças de seleção. |
| Java | `amostra/Amostras.java` | Consulta lotes e amostras e grava a seleção no H2. |
| Java | `captura/CapturaController.java` | Limita e interpreta o pedido de captura e expõe listas e imagens. |
| Java | `captura/CapturaInformada.java` | Define os campos e restrições do pedido de captura. |
| Java | `captura/Capturas.java` | Confere lote e amostra, valida o JPEG e coordena disco e banco. |
| Java | `captura/ImagemCifrada.java` | Carrega a chave privada e verifica e decifra o conteúdo recebido. |
| Java | `captura/DelimitadorPemPkcs8.java` | Gera os delimitadores do arquivo textual da chave privada. |
| Java | `comando/ComandoController.java` | Valida pedidos de comando e expõe o histórico. |
| Java | `comando/Comandos.java` | Grava, lista e entrega comandos a partir da fila do banco. |
| Java | `erro/TratamentoErro.java` | Padroniza erros HTTP em JSON sem expor detalhes internos ao cliente. |
| Java | `estacao/EstacaoController.java` | Recebe a sincronização e calcula o estado exibido no painel. |
| Java | `estacao/EstadoInformado.java` | Define e valida os campos informados pela estação. |
| Java | `seguranca/Credenciais.java` | Exige credenciais de autenticação e configura o usuário do painel. |
| Java | `seguranca/CsrfController.java` | Fornece ao navegador o token de proteção das alterações. |
| Java | `seguranca/Seguranca.java` | Aplica autenticação por chave à estação e HTTP Basic e CSRF ao painel. |
| Recursos | `application.properties` | Configura endereço local, leitura de configuração, H2 e validação JSON. |
| Recursos | `schema.sql` | Cria tabelas, restrições, sequências de IDs e índice de capturas. |
| Recursos | `data.sql` | Preenche o catálogo fictício e seleciona A1 se ainda não houver seleção. |
| Painel | `static/index.html` | Define seletor, indicadores, controles e tabelas da página. |
| Painel | `static/painel.js` | Consulta a API, envia alterações e atualiza a página. |

## 3. Fluxos passo a passo

### Inicialização e laço principal

1. `setup()` configura sensores, barra e RGB, verifica a configuração do cliente e inicia o Wi-Fi. O Wi-Fi roda em modo cliente, com reconexão automática. [sketch.ino:40](../firmware/sketch/sketch.ino#L40) · [rede.cpp:5](../firmware/sketch/rede.cpp#L5)
2. Em cada volta, `loop()` lê sensores e Wi-Fi, atualiza a barra e escolhe o RGB. Detecta um aperto novo comparando a leitura estável atual com a anterior. O brilho inicial é 10 e a pausa no fim da volta é de 10 ms. [sketch.ino:10](../firmware/sketch/sketch.ino#L10) · [sketch.ino:12](../firmware/sketch/sketch.ino#L12) · [sketch.ino:50](../firmware/sketch/sketch.ino#L50) · [sketch.ino:55](../firmware/sketch/sketch.ino#L55) · [sketch.ino:83](../firmware/sketch/sketch.ino#L83)
3. Com cliente configurado e Wi-Fi conectado, o primeiro contato é imediato. Depois, o laço tenta a cada 5 s, ou antes disso se houver um aperto apto à captura ou um pedido de atualização imediata. A referência dos 5 s é o início da última tentativa. Como o laço espera as chamadas de rede, o intervalo é nominal. [sketch.ino:9](../firmware/sketch/sketch.ino#L9) · [sketch.ino:16](../firmware/sketch/sketch.ino#L16) · [sketch.ino:63](../firmware/sketch/sketch.ino#L63) · [sketch.ino:67](../firmware/sketch/sketch.ino#L67) · [sketch.ino:72](../firmware/sketch/sketch.ino#L72)
4. Se a sincronização funcionar, executa primeiro o comando recebido e então a captura pelo botão, se pedida. Se o botão exigiu a troca e ela falhar, sinaliza erro. Depois relê sensores e atualiza as saídas. [sketch.ino:72](../firmware/sketch/sketch.ino#L72) · [sketch.ino:79](../firmware/sketch/sketch.ino#L79)

### Presença, barra e RGB

1. A chave de presença está no GPIO 35 (pino digital do ESP32). Nível baixo significa placa presente. A barra recebe a decisão local a cada volta, antes da comunicação. [sensores.cpp:8](../firmware/sketch/sensores.cpp#L8) · [sensores.cpp:51](../firmware/sketch/sensores.cpp#L51) · [sketch.ino:52](../firmware/sketch/sketch.ino#L52)
2. Com placa, a barra acende os primeiros segmentos até o nível configurado. Sem placa, todos apagam. Os pinos, na ordem, são 5, 3, 4, 13, 14, 16, 17, 21, 22 e 27. [iluminacao.cpp:7](../firmware/sketch/iluminacao.cpp#L7) · [iluminacao.cpp:20](../firmware/sketch/iluminacao.cpp#L20)
3. O RGB usa 25 para vermelho, 26 para verde e 32 para azul. Com Wi-Fi, indica amarelo sem placa, verde com placa e azul durante captura e envio. Sem Wi-Fi, mostra vermelho. Uma recusa ou falha também acende vermelho por 3 s. [status.cpp:6](../firmware/sketch/status.cpp#L6) · [status.cpp:7](../firmware/sketch/status.cpp#L7) · [status.cpp:8](../firmware/sketch/status.cpp#L8) · [status.cpp:9](../firmware/sketch/status.cpp#L9) · [status.cpp:24](../firmware/sketch/status.cpp#L24) · [status.cpp:30](../firmware/sketch/status.cpp#L30) · [status.cpp:32](../firmware/sketch/status.cpp#L32) · [captura.cpp:45](../firmware/sketch/captura.cpp#L45)

### LDR e botão

1. O LDR é um resistor que muda com a luz. O ADC, conversor de tensão para número, lê o GPIO 34 em 12 bits, com atenuação de 11 dB. O código limita a leitura a 1–4094 para evitar divisões por zero nos extremos. [sensores.cpp:7](../firmware/sketch/sensores.cpp#L7) · [sensores.cpp:10](../firmware/sketch/sensores.cpp#L10) · [sensores.cpp:11](../firmware/sketch/sensores.cpp#L11) · [sensores.cpp:30](../firmware/sketch/sensores.cpp#L30) · [sensores.cpp:45](../firmware/sketch/sensores.cpp#L45)
2. O firmware converte o número em tensão com a referência de 3,3 V. Usa o divisor de tensão de 10 kΩ para obter a resistência do LDR. Aplica a curva nominal do Wokwi (50 kΩ a 10 lx e expoente 0,7) para estimar lux, unidade de iluminância. [sensores.cpp:12](../firmware/sketch/sensores.cpp#L12) · [sensores.cpp:13](../firmware/sketch/sensores.cpp#L13) · [sensores.cpp:14](../firmware/sketch/sensores.cpp#L14) · [sensores.cpp:16](../firmware/sketch/sensores.cpp#L16) · [sensores.cpp:17](../firmware/sketch/sensores.cpp#L17) · [sensores.cpp:47](../firmware/sketch/sensores.cpp#L47) · [sensores.cpp:48](../firmware/sketch/sensores.cpp#L48) · [sensores.cpp:49](../firmware/sketch/sensores.cpp#L49)
3. O botão no GPIO 36 está pressionado em nível baixo. Cada mudança na leitura reinicia a contagem. A leitura vira estável após 30 ms sem mudança. Esse filtro é o debounce. A captura usa a transição de solto para pressionado, por isso manter o botão pressionado só gera um disparo após ser reconhecido. [sensores.cpp:9](../firmware/sketch/sensores.cpp#L9) · [sensores.cpp:18](../firmware/sketch/sensores.cpp#L18) · [sensores.cpp:34](../firmware/sketch/sensores.cpp#L34) · [sensores.cpp:38](../firmware/sketch/sensores.cpp#L38) · [sensores.cpp:40](../firmware/sketch/sensores.cpp#L40) · [sketch.ino:55](../firmware/sketch/sketch.ino#L55)

### Sincronização e seleção

1. A estação faz `POST /api/estacao/sincronizacao` com a chave no cabeçalho `X-Api-Key` e um corpo JSON, formato de troca de dados. Envia ID `est-01`, presença, lux, brilho, amostra em uso (nula antes de receber seleção), RSSI (força do sinal Wi-Fi) e tempo ligada em segundos. Inclui o motivo de recusa quando o indicador de lux estiver ativo. [cliente_servidor.cpp:83](../firmware/sketch/cliente_servidor.cpp#L83) · [cliente_servidor.cpp:88](../firmware/sketch/cliente_servidor.cpp#L88) · [cliente_servidor.cpp:92](../firmware/sketch/cliente_servidor.cpp#L92) · [cliente_servidor.cpp:99](../firmware/sketch/cliente_servidor.cpp#L99) · [sketch.ino:69](../firmware/sketch/sketch.ino#L69)
2. Após autenticar e validar o pedido, o servidor guarda o último estado e o horário de recebimento juntos em memória. Para o painel, a estação começa offline e fica online até 15 s após a última sincronização, inclusive. Depois o painel ainda pode mostrar o último estado conhecido. [Seguranca.java:59](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L59) · [EstacaoController.java:30](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L30) · [EstacaoController.java:31](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L31) · [EstacaoController.java:40](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L40) · [EstacaoController.java:43](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L43) · [EstacaoController.java:44](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L44)
3. Em uma transação, operação única do banco, o servidor bloqueia a linha da seleção, lê a amostra atual e busca até um comando pendente, o de menor ID. Marca-o como entregue antes de devolver a resposta. O bloqueio serializa as entregas concorrentes e a alteração da seleção usa a mesma linha. [Comandos.java:35](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L35) · [Comandos.java:38](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L38) · [Comandos.java:41](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L41) · [Comandos.java:45](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L45) · [Amostras.java:46](../servidor/src/main/java/br/com/petrilens/amostra/Amostras.java#L46)
4. Voltam `amostra_selecionada`, com lote e amostra, e `comandos`, uma lista com zero ou um item. Em HTTP 200, o cliente confere o formato e IDs não vazios antes de atualizar a seleção e interpretar o comando. A estação informa a nova seleção numa sincronização seguinte. [EstacaoController.java:33](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L33) · [cliente_servidor.cpp:104](../firmware/sketch/cliente_servidor.cpp#L104) · [cliente_servidor.cpp:109](../firmware/sketch/cliente_servidor.cpp#L109) · [cliente_servidor.cpp:114](../firmware/sketch/cliente_servidor.cpp#L114) · [cliente_servidor.cpp:117](../firmware/sketch/cliente_servidor.cpp#L117) · [sketch.ino:69](../firmware/sketch/sketch.ino#L69)
5. Para mudar a seleção, o painel pede o token CSRF e envia `PUT /api/painel/amostra-selecionada`. O servidor atualiza a seleção no H2 se a amostra existir. A mudança chega à estação na resposta de sincronização. [painel.js:93](../servidor/src/main/resources/static/painel.js#L93) · [painel.js:94](../servidor/src/main/resources/static/painel.js#L94) · [AmostraController.java:30](../servidor/src/main/java/br/com/petrilens/amostra/AmostraController.java#L30) · [Amostras.java:43](../servidor/src/main/java/br/com/petrilens/amostra/Amostras.java#L43) · [EstacaoController.java:33](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L33)

### Comando pelo painel

1. Um clique envia o tipo `capturar` ou `status`. O formulário de brilho acrescenta um valor numérico. O navegador obtém o token CSRF, proteção contra requisições induzidas por outro site, e envia `POST /api/painel/comandos` com o token no cabeçalho informado pelo servidor. [painel.js:136](../servidor/src/main/resources/static/painel.js#L136) · [painel.js:142](../servidor/src/main/resources/static/painel.js#L142) · [painel.js:125](../servidor/src/main/resources/static/painel.js#L125) · [painel.js:126](../servidor/src/main/resources/static/painel.js#L126) · [painel.js:80](../servidor/src/main/resources/static/painel.js#L80) · [CsrfController.java:13](../servidor/src/main/java/br/com/petrilens/seguranca/CsrfController.java#L13)
2. O servidor exige login e valida o token. Aceita `capturar` e `status` com valor ausente ou nulo, e `ajustar_brilho` com inteiro de 0 a 10. Grava como pendente no H2 e responde HTTP 201. [Seguranca.java:76](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L76) · [Seguranca.java:79](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L79) · [ComandoController.java:36](../servidor/src/main/java/br/com/petrilens/comando/ComandoController.java#L36) · [ComandoController.java:54](../servidor/src/main/java/br/com/petrilens/comando/ComandoController.java#L54) · [ComandoController.java:55](../servidor/src/main/java/br/com/petrilens/comando/ComandoController.java#L55) · [ComandoController.java:57](../servidor/src/main/java/br/com/petrilens/comando/ComandoController.java#L57) · [Comandos.java:25](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L25)
3. Na próxima sincronização que escolher esse comando, a estação recebe a seleção e o comando juntos. O firmware também valida o tipo e o valor. Entregue significa incluído na resposta pelo servidor. A execução depende de a estação receber a resposta e cumprir as condições locais. [Comandos.java:47](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L47) · [cliente_servidor.cpp:64](../firmware/sketch/cliente_servidor.cpp#L64) · [cliente_servidor.cpp:70](../firmware/sketch/cliente_servidor.cpp#L70) · [painel.js:162](../servidor/src/main/resources/static/painel.js#L162)
4. `capturar` chama a mesma rotina de captura usada pelo botão. `status` pede outra sincronização na próxima volta apta. `ajustar_brilho` atualiza o número de segmentos e reaplica a regra de presença. [sketch.ino:24](../firmware/sketch/sketch.ino#L24) · [sketch.ino:28](../firmware/sketch/sketch.ino#L28) · [sketch.ino:31](../firmware/sketch/sketch.ino#L31) · [sketch.ino:32](../firmware/sketch/sketch.ino#L32)

### Captura pelo botão e recebimento no servidor

1. Um aperto apto exige placa, cliente configurado e Wi-Fi. A estação sincroniza antes de fotografar para obter a seleção atual. Se a resposta for válida, `capturar()` relê os sensores e exige também um ID de amostra. [sketch.ino:57](../firmware/sketch/sketch.ino#L57) · [sketch.ino:64](../firmware/sketch/sketch.ino#L64) · [sketch.ino:74](../firmware/sketch/sketch.ino#L74) · [captura.cpp:34](../firmware/sketch/captura.cpp#L34)
2. A leitura deve estar entre 300 e 700 lx, inclusive. Fora da faixa, o vermelho acende, a rotina para antes de abrir o cartão e o indicador de recusa pode seguir nas sincronizações até outra tentativa passar na verificação de lux. [captura.cpp:12](../firmware/sketch/captura.cpp#L12) · [captura.cpp:13](../firmware/sketch/captura.cpp#L13) · [captura.cpp:16](../firmware/sketch/captura.cpp#L16) · [captura.cpp:40](../firmware/sketch/captura.cpp#L40) · [captura.cpp:43](../firmware/sketch/captura.cpp#L43) · [cliente_servidor.cpp:99](../firmware/sketch/cliente_servidor.cpp#L99)
3. O RGB passa à sinalização de captura. O firmware inicia o cartão se necessário, abre `/placa-base64.txt` e valida o tamanho do arquivo. Depois da abertura, verifica novamente a presença. Se a placa saiu, fecha o arquivo e recusa o envio. [captura.cpp:45](../firmware/sketch/captura.cpp#L45) · [imagem.cpp:107](../firmware/sketch/imagem.cpp#L107) · [imagem.cpp:111](../firmware/sketch/imagem.cpp#L111) · [imagem.cpp:112](../firmware/sketch/imagem.cpp#L112) · [captura.cpp:54](../firmware/sketch/captura.cpp#L54)
4. O firmware gera uma chave AES aleatória de 16 bytes e um IV (valor inicial da cifra) de 12 bytes. AES-GCM cifra o JPEG à medida que o texto do cartão é decodificado em blocos e anexa uma tag de 16 bytes, usada para detectar adulteração. RSA-OAEP com chave pública de 2048 bits e SHA-256 protege a chave AES. Só o servidor possui a chave privada correspondente. [imagem.cpp:24](../firmware/sketch/imagem.cpp#L24) · [imagem.h:10](../firmware/sketch/imagem.h#L10) · [imagem.cpp:23](../firmware/sketch/imagem.cpp#L23) · [imagem.cpp:25](../firmware/sketch/imagem.cpp#L25) · [imagem.cpp:127](../firmware/sketch/imagem.cpp#L127) · [imagem.cpp:128](../firmware/sketch/imagem.cpp#L128) · [imagem.cpp:88](../firmware/sketch/imagem.cpp#L88) · [imagem.cpp:65](../firmware/sketch/imagem.cpp#L65) · [imagem.cpp:46](../firmware/sketch/imagem.cpp#L46)
5. O cliente monta os metadados (estação, lote, amostra, lux, resolução e indicação de simulação), a chave cifrada e o IV. Gera o Base64 da imagem cifrada em blocos de 192 bytes binários, entre o prefixo e o fechamento do JSON. Calcula o tamanho exato antes de enviar um único POST por tentativa, sem guardar todo o Base64 na RAM. [cliente_servidor.cpp:222](../firmware/sketch/cliente_servidor.cpp#L222) · [cliente_servidor.cpp:228](../firmware/sketch/cliente_servidor.cpp#L228) · [cliente_servidor.cpp:144](../firmware/sketch/cliente_servidor.cpp#L144) · [cliente_servidor.cpp:164](../firmware/sketch/cliente_servidor.cpp#L164) · [cliente_servidor.cpp:177](../firmware/sketch/cliente_servidor.cpp#L177) · [cliente_servidor.cpp:196](../firmware/sketch/cliente_servidor.cpp#L196)
6. O firmware limita o corpo a 64 KiB (65.536 bytes) e tenta até três vezes, com HTTP 201 como confirmação. Se esgotar as tentativas, fecha o arquivo e sinaliza erro. Perder a resposta depois de o servidor gravar pode gerar outra captura na tentativa seguinte. [cliente_servidor.cpp:20](../firmware/sketch/cliente_servidor.cpp#L20) · [cliente_servidor.cpp:21](../firmware/sketch/cliente_servidor.cpp#L21) · [cliente_servidor.cpp:198](../firmware/sketch/cliente_servidor.cpp#L198) · [cliente_servidor.cpp:235](../firmware/sketch/cliente_servidor.cpp#L235) · [captura.cpp:60](../firmware/sketch/captura.cpp#L60) · [captura.cpp:61](../firmware/sketch/captura.cpp#L61) · [Contrato da API](api.md)
7. No servidor, o mesmo limite vale para todo o corpo. O controlador verifica o tamanho declarado e lê no máximo 65.537 bytes para detectar excesso mesmo sem esse cabeçalho. Excesso retorna HTTP 413 antes de interpretar o JSON. Em seguida valida os campos, incluindo lux finito, e confirma o vínculo amostra–lote. [CapturaController.java:24](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L24) · [CapturaController.java:46](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L46) · [CapturaController.java:50](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L50) · [CapturaController.java:52](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L52) · [CapturaController.java:65](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L65) · [Capturas.java:39](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L39)
8. O servidor abre a chave AES com a privada RSA e decifra e verifica a tag com AES-GCM. Confere os marcadores de início e fim do JPEG, o formato e as dimensões de 800 × 600 antes de ler os pixels. Um JPEG inválido recebe HTTP 400. [ImagemCifrada.java:63](../servidor/src/main/java/br/com/petrilens/captura/ImagemCifrada.java#L63) · [ImagemCifrada.java:74](../servidor/src/main/java/br/com/petrilens/captura/ImagemCifrada.java#L74) · [Capturas.java:107](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L107) · [Capturas.java:120](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L120) · [Capturas.java:121](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L121) · [Capturas.java:122](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L122) · [Capturas.java:130](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L130)
9. O servidor usa o horário de recebimento e um ID do H2. Cria `dados/capturas/<id>.jpg` sem sobrescrever arquivo existente e só então registra os metadados no H2. Se a gravação ou registro falhar depois da criação, tenta limpar o arquivo. Se tudo der certo, responde HTTP 201 com ID e horário. [CapturaController.java:39](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L39) · [CapturaController.java:42](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L42) · [Capturas.java:28](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L28) · [Capturas.java:45](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L45) · [Capturas.java:50](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L50) · [Capturas.java:55](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L55) · [Capturas.java:59](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L59) · [Capturas.java:68](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L68) · [Capturas.java:102](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L102)

### Painel e login

1. O painel é servido pelo Spring no próprio notebook, em `localhost:8080`. O navegador autentica o operador por HTTP Basic, mecanismo padrão de usuário e senha em cabeçalho. Sem login válido o servidor responde 401 e solicita autenticação. As alterações também exigem CSRF. [application.properties:1](../servidor/src/main/resources/application.properties#L1) · [application.properties:2](../servidor/src/main/resources/application.properties#L2) · [Seguranca.java:76](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L76) · [Seguranca.java:80](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L80) · [Seguranca.java:81](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L81) · [Seguranca.java:82](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L82)
2. O JavaScript consulta histórico, catálogo quando necessário, estado e capturas da amostra selecionada. Agenda o próximo ciclo 2 s após as consultas terminarem, para evitar sobreposição dos ciclos. [painel.js:148](../servidor/src/main/resources/static/painel.js#L148) · [painel.js:206](../servidor/src/main/resources/static/painel.js#L206) · [painel.js:209](../servidor/src/main/resources/static/painel.js#L209) · [painel.js:213](../servidor/src/main/resources/static/painel.js#L213) · [painel.js:225](../servidor/src/main/resources/static/painel.js#L225) · [painel.js:233](../servidor/src/main/resources/static/painel.js#L233)
3. A página exibe seleção do servidor, amostra informada pela estação, estado online, última sincronização, presença, lux, brilho, RSSI, tempo ligada e aviso de recusa por luz. Mostra o histórico de comandos e, para a amostra selecionada, horário, lux e miniatura com link para o JPEG protegido. [painel.js:11](../servidor/src/main/resources/static/painel.js#L11) · [painel.js:185](../servidor/src/main/resources/static/painel.js#L185) · [painel.js:186](../servidor/src/main/resources/static/painel.js#L186) · [painel.js:192](../servidor/src/main/resources/static/painel.js#L192) · [painel.js:197](../servidor/src/main/resources/static/painel.js#L197) · [painel.js:153](../servidor/src/main/resources/static/painel.js#L153) · [painel.js:23](../servidor/src/main/resources/static/painel.js#L23) · [painel.js:25](../servidor/src/main/resources/static/painel.js#L25) · [painel.js:28](../servidor/src/main/resources/static/painel.js#L28) · [painel.js:32](../servidor/src/main/resources/static/painel.js#L32) · [Seguranca.java:79](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L79)

## 4. Decisões e porquês

- **ADR 0001 · Um repositório.** Firmware, servidor e documentação compartilham o mesmo contrato. Guardá-los juntos permite atualizar os dois lados da API e a documentação na mesma mudança. [ADR 0001](adr/0001-monorrepo-unico.md)
- **ADR 0002 · Servidor próprio em Java/Spring.** O grupo já conhecia a tecnologia. Um servidor pequeno resolve captura, histórico, comandos e painel no mesmo processo, enquanto o sistema mais amplo do PetriLens fica fora da entrega. [ADR 0002](adr/0002-servidor-proprio-em-spring.md)
- **ADR 0003 · Wokwi e servidor no notebook.** A montagem simulada era parte da proposta. O plano inicial usava o Wokwi web com túnel HTTPS para alcançar o notebook. Concentrar URL e protocolo em um módulo permitiu mudar o caminho da rede no ensaio. [ADR 0003](adr/0003-simulacao-no-wokwi-web.md)
- **ADR 0004 · JPEG de exemplo no microSD.** O Wokwi não simula a câmera desejada e copia arquivos de texto do projeto para o cartão. O arquivo Base64 permite exercitar leitura, envio e armazenamento de um JPEG válido. [ADR 0004](adr/0004-camera-simulada-por-jpeg-no-sd.md)
- **ADR 0005 · A estação inicia as trocas.** O gateway público original permitia conexões de saída. A sincronização periódica aproveita a resposta da própria estação para levar seleção e comando, enquanto o painel conversa apenas com o servidor. [ADR 0005](adr/0005-estacao-inicia-toda-comunicacao.md)
- **ADR 0006 · Presença acende a luz, operador dispara.** A luz responde ao sensor local e o operador escolhe o momento da captura. O controle de lux do Wokwi é independente da barra, por isso o firmware verifica a faixa e o brilho é ajustado por comando. [ADR 0006](adr/0006-gatilho-da-captura-e-sensor-de-luz.md) · [Decisões](decisoes.md#recusa-por-lux-9)
- **ADR 0007 · Cifra até a API.** O túnel original terminava o HTTPS antes de chegar ao servidor. Cifrar o próprio JPEG faz a proteção seguir até a API e permite verificar adulterações. A chave pública pode ficar no firmware. [ADR 0007](adr/0007-imagem-cifrada-para-a-api.md)
- **ADR 0008 · Gateway local na demo.** O gateway público apresentou instabilidade e a conexão TLS do simulador com o ngrok falhou mesmo pelo gateway privado. A extensão para VS Code encaminha HTTP ao mesmo notebook. A exceção é limitada a esse destino e a imagem segue cifrada. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)
- **Wi-Fi e HTTP.** O ESP32 tem Wi-Fi integrado e pode usar a rede simulada. As operações do projeto cabem em requisições HTTP com JSON e respostas diretas. A demo usa HTTP local e outros destinos exigem HTTPS com certificado validado. [Relatório](relatorio.md) · [cliente_servidor.cpp:54](../firmware/sketch/cliente_servidor.cpp#L54) · [cliente_servidor.cpp:56](../firmware/sketch/cliente_servidor.cpp#L56)
- **H2 em arquivo e painel estático.** O banco mantém seleção, fila e metadados entre execuções sem exigir outro servidor. HTML e JavaScript servidos pelo Spring permitem operar a estação pelo navegador sem uma etapa de build do painel. [Decisões](decisoes.md) · [application.properties:17](../servidor/src/main/resources/application.properties#L17)
- **Seleção, horário e tolerância a falhas.** Sincronizar antes do botão reduz o risco de usar a seleção anterior. O servidor registra a hora da chegada usando seu relógio. O limite de tentativas encerra o envio com erro quando a estação não obtém confirmação. [Decisões](decisoes.md) · [Contrato da API](api.md)
- **Laço único.** O fluxo síncrono é mais simples de acompanhar na demo. Enquanto o laço aguarda a rede, também atrasa novas leituras do botão e atualizações locais. [Decisões](decisoes.md#decisões-a-revisar-orquestração-final) · [sketch.ino:72](../firmware/sketch/sketch.ino#L72)

## 5. Perguntas prováveis e respostas curtas

### 1. Quais são as partes da PetriLens?

O ESP32 simulado lê sensores, controla luz e envia dados. O Spring fornece a API e o painel.  
O H2 conserva os registros e o disco guarda os JPEGs. [Relatório](relatorio.md)

### 2. Colocar a placa já tira uma foto?

A presença acende a barra conforme o brilho configurado.  
A captura começa por um aperto novo do botão ou pelo comando do painel. [sketch.ino:55](../firmware/sketch/sketch.ino#L55) · [sketch.ino:24](../firmware/sketch/sketch.ino#L24) · [iluminacao.cpp:20](../firmware/sketch/iluminacao.cpp#L20)

### 3. A barra funciona sem internet ou Wi-Fi?

A regra roda na estação e usa presença e brilho, mesmo sem conexão.  
A falta de Wi-Fi é indicada no RGB, e impede sincronização e captura. [sketch.ino:52](../firmware/sketch/sketch.ino#L52) · [sketch.ino:57](../firmware/sketch/sketch.ino#L57) · [status.cpp:32](../firmware/sketch/status.cpp#L32)

### 4. O que o RGB mostra?

Amarelo indica Wi-Fi com berço vazio, verde indica placa presente com Wi-Fi e azul acompanha captura e envio.  
Vermelho indica falta de Wi-Fi ou um erro recente. [status.cpp:31](../firmware/sketch/status.cpp#L31) · [status.cpp:9](../firmware/sketch/status.cpp#L9)

### 5. Como o botão evita disparos repetidos?

O código espera 30 ms de leitura sem mudança antes de aceitar um novo estado estável.  
O laço dispara apenas na transição para pressionado. [sensores.cpp:18](../firmware/sketch/sensores.cpp#L18) · [sensores.cpp:40](../firmware/sketch/sensores.cpp#L40) · [sketch.ino:55](../firmware/sketch/sketch.ino#L55)

### 6. Por que há resistores externos na chave e no botão?

Os GPIOs 34 a 36 são entradas sem pull-up interno. Na chave e no botão, o pull-up externo mantém nível alto em repouso.  
Quando o contato vai ao terra, o firmware lê nível baixo como presença ou aperto. [Relatório](relatorio.md) · [sensores.cpp:34](../firmware/sketch/sensores.cpp#L34) · [sensores.cpp:51](../firmware/sketch/sensores.cpp#L51)

### 7. Por que o Monitor Serial não foi iniciado?

O GPIO 3, recepção da porta serial padrão, está ligado a um segmento da barra.  
O firmware deixa a porta sem inicialização para usar esse pino na iluminação. [sketch.ino:41](../firmware/sketch/sketch.ino#L41) · [iluminacao.cpp:7](../firmware/sketch/iluminacao.cpp#L7)

### 8. Como o firmware obtém lux?

O ADC converte a tensão do sensor em número. O firmware calcula a resistência do LDR a partir da tensão.  
A curva nominal do componente no Wokwi transforma a resistência em uma estimativa de lux. [sensores.cpp:45](../firmware/sketch/sensores.cpp#L45) · [sensores.cpp:47](../firmware/sketch/sensores.cpp#L47) · [sensores.cpp:48](../firmware/sketch/sensores.cpp#L48) · [sensores.cpp:49](../firmware/sketch/sensores.cpp#L49)

### 9. Por que a faixa de 300 a 700 lx?

Ela demonstra a checagem de luz ao redor do valor padrão do Wokwi. Os dois extremos são aceitos.  
O ensaio usa o modelo do simulador, e a calibração de laboratório fica fora da demo. [captura.cpp:12](../firmware/sketch/captura.cpp#L12) · [captura.cpp:13](../firmware/sketch/captura.cpp#L13) · [captura.cpp:16](../firmware/sketch/captura.cpp#L16) · [Decisões](decisoes.md#recusa-por-lux-9)

### 10. Por que a barra não corrige o lux automaticamente?

O controle do LDR no Wokwi é manual e independente da luz da barra.  
O brilho controla quantos segmentos acendem, e a leitura do LDR é uma condição para capturar. [ADR 0006](adr/0006-gatilho-da-captura-e-sensor-de-luz.md) · [captura.cpp:40](../firmware/sketch/captura.cpp#L40)

### 11. O que o microSD substitui?

Ele fornece o JPEG fictício no ponto em que um aparelho físico pediria a imagem à câmera.  
O firmware lê do cartão um arquivo de texto Base64, que representa os bytes do JPEG. [ADR 0004](adr/0004-camera-simulada-por-jpeg-no-sd.md) · [imagem.cpp:111](../firmware/sketch/imagem.cpp#L111)

### 12. Quem inicia a comunicação e por quê?

O ESP32 inicia a sincronização e o envio da captura. O caminho original do simulador permitia conexões de saída.  
O servidor aproveita a resposta da sincronização para mandar a seleção e um eventual comando. [ADR 0005](adr/0005-estacao-inicia-toda-comunicacao.md) · [EstacaoController.java:33](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L33)

### 13. Quando a estação sincroniza?

Com configuração e Wi-Fi válidos, tenta no primeiro contato e depois em intervalos nominais de 5 s.  
Um aperto apto à captura ou o comando `status` antecipa a troca. [sketch.ino:9](../firmware/sketch/sketch.ino#L9) · [sketch.ino:16](../firmware/sketch/sketch.ino#L16) · [sketch.ino:28](../firmware/sketch/sketch.ino#L28) · [sketch.ino:63](../firmware/sketch/sketch.ino#L63)

### 14. O que vai e o que volta na sincronização?

Vão ID, presença, lux, brilho, amostra em uso, força do sinal, tempo ligada e eventual recusa por lux.  
Voltam o lote e a amostra selecionados e uma lista de zero ou um comando. [cliente_servidor.cpp:92](../firmware/sketch/cliente_servidor.cpp#L92) · [cliente_servidor.cpp:99](../firmware/sketch/cliente_servidor.cpp#L99) · [EstacaoController.java:33](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L33) · [Comandos.java:41](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L41)

### 15. Como o painel sabe que a estação está offline?

O servidor compara seu relógio com o instante do último estado recebido.  
A estação começa offline, fica online até 15 s inclusive após sincronizar e volta a offline depois disso. [EstacaoController.java:31](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L31) · [EstacaoController.java:40](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L40) · [EstacaoController.java:43](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L43)

### 16. Como o servidor evita devolver o mesmo comando duas vezes?

A entrega bloqueia a linha da seleção dentro de uma transação. Assim, duas sincronizações não escolhem o mesmo pendente.  
A primeira marca o comando como entregue antes de sair da transação. [Comandos.java:35](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L35) · [Comandos.java:38](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L38) · [Comandos.java:45](../servidor/src/main/java/br/com/petrilens/comando/Comandos.java#L45)

### 17. Entregue quer dizer executado?

Entregue significa que o servidor incluiu o comando na resposta.  
A mensagem pode se perder ou a estação pode recusar a captura pelas condições locais. [painel.js:162](../servidor/src/main/resources/static/painel.js#L162) · [captura.cpp:34](../firmware/sketch/captura.cpp#L34) · [Contrato da API](api.md)

### 18. O que muda quando o operador ajusta o brilho?

O inteiro de 0 a 10 configura o número de segmentos acesos com placa presente.  
O servidor e o firmware conferem a faixa, e a barra permanece apagada com o berço vazio. [ComandoController.java:55](../servidor/src/main/java/br/com/petrilens/comando/ComandoController.java#L55) · [cliente_servidor.cpp:70](../firmware/sketch/cliente_servidor.cpp#L70) · [iluminacao.cpp:20](../firmware/sketch/iluminacao.cpp#L20)

### 19. Qual amostra recebe a imagem se o operador mudar a seleção no meio do envio?

A captura fica com a amostra informada no pedido pelo firmware.  
O servidor confere se a amostra enviada pertence ao lote informado. [cliente_servidor.cpp:224](../firmware/sketch/cliente_servidor.cpp#L224) · [Capturas.java:39](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L39) · [Contrato da API](api.md)

### 20. O que acontece se a luz estiver fora da faixa?

A rotina sinaliza erro antes de abrir o microSD. O painel pode mostrar o motivo recebido na sincronização.  
Outra tentativa que passar na checagem de luz limpa esse motivo, mesmo se falhar depois. [captura.cpp:40](../firmware/sketch/captura.cpp#L40) · [captura.cpp:46](../firmware/sketch/captura.cpp#L46) · [painel.js:192](../servidor/src/main/resources/static/painel.js#L192)

### 21. O que são as partes do envio?

O firmware emite o JSON como prefixo, Base64 da imagem cifrada em blocos e fechamento do JSON.  
Cada tentativa é um POST completo, com tamanho calculado e sem uma cópia integral do Base64 em memória. [cliente_servidor.cpp:138](../firmware/sketch/cliente_servidor.cpp#L138) · [cliente_servidor.cpp:144](../firmware/sketch/cliente_servidor.cpp#L144) · [cliente_servidor.cpp:196](../firmware/sketch/cliente_servidor.cpp#L196)

### 22. O que ocorre quando o servidor não responde à captura?

O firmware pode repetir o POST até completar três tentativas. Se todas falharem, indica erro e descarta a captura.  
Se a gravação funcionou e só a resposta se perdeu, a repetição pode gerar outra captura. [cliente_servidor.cpp:20](../firmware/sketch/cliente_servidor.cpp#L20) · [cliente_servidor.cpp:235](../firmware/sketch/cliente_servidor.cpp#L235) · [captura.cpp:61](../firmware/sketch/captura.cpp#L61) · [Contrato da API](api.md)

### 23. Como a API confere uma captura?

Limita o JSON a 64 KiB, valida os campos e confere o lote da amostra.  
Depois verifica a cifra e testa se os bytes formam um JPEG legível de 800 × 600. [CapturaController.java:24](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L24) · [CapturaController.java:65](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java#L65) · [Capturas.java:39](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L39) · [Capturas.java:43](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L43) · [Capturas.java:121](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L121)

### 24. Onde ficam os dados depois de reiniciar o servidor?

O JPEG fica em disco e o H2 em arquivo guarda metadados, seleção e histórico de comandos.  
O último estado da estação fica em memória e só reaparece quando a estação volta a sincronizar. [Capturas.java:28](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java#L28) · [application.properties:17](../servidor/src/main/resources/application.properties#L17) · [schema.sql:5](../servidor/src/main/resources/schema.sql#L5) · [schema.sql:10](../servidor/src/main/resources/schema.sql#L10) · [schema.sql:22](../servidor/src/main/resources/schema.sql#L22) · [EstacaoController.java:21](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java#L21)

### 25. Como o painel atualiza as informações?

O JavaScript consulta estado, histórico e capturas da seleção pelo servidor.  
Agenda outro ciclo 2 s depois que o atual termina. [painel.js:206](../servidor/src/main/resources/static/painel.js#L206) · [painel.js:213](../servidor/src/main/resources/static/painel.js#L213) · [painel.js:226](../servidor/src/main/resources/static/painel.js#L226) · [painel.js:233](../servidor/src/main/resources/static/painel.js#L233)

### 26. Qual a diferença entre a chave da estação e o login do painel?

A estação envia `X-Api-Key` nas suas rotas. O operador usa usuário e senha via HTTP Basic no navegador.  
O servidor lê as credenciais de configuração local e aplica cada mecanismo nas rotas correspondentes. [Seguranca.java:58](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L58) · [Seguranca.java:80](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L80) · [application.properties:10](../servidor/src/main/resources/application.properties#L10) · [Credenciais](credenciais.md)

### 27. Por que as alterações do painel exigem CSRF?

O navegador pode reenviar o login Basic automaticamente. O servidor exige um token adicional quando o painel muda a seleção ou cria um comando.  
O JavaScript pede o token e envia no cabeçalho indicado pela API. [Seguranca.java:75](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java#L75) · [painel.js:80](../servidor/src/main/resources/static/painel.js#L80) · [painel.js:125](../servidor/src/main/resources/static/painel.js#L125)

### 28. O que AES-GCM, RSA-OAEP e Base64 fazem?

AES-GCM cifra o JPEG e permite detectar adulteração. RSA-OAEP protege a chave AES com a chave pública do servidor.  
Base64 só representa os bytes como texto para transportar no JSON. [ADR 0007](adr/0007-imagem-cifrada-para-a-api.md) · [cliente_servidor.cpp:164](../firmware/sketch/cliente_servidor.cpp#L164)

### 29. Qual risco do OWASP foi tratado e o que ainda fica em aberto?

O checklist OWASP IoT Top 10 aponta o risco de transferência e armazenamento inseguros. A imagem é cifrada até a API.  
Na demo, a chave da estação, a telemetria e os metadados passam por HTTP local, e o JPEG recuperado fica aberto no disco. [Checklist OWASP](owasp-iot-top-10.md) · [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)

### 30. Quais outros riscos o grupo aceitou?

O projeto público pode expor a chave da demo. Por isso ela é exclusiva e descartável.  
Também foram aceitos troca manual da chave, ausência de fila offline e risco de componentes sem auditoria de vulnerabilidades. [Checklist OWASP](owasp-iot-top-10.md)

### 31. Qual foi a falha do TLS e como foi corrigida?

A borda do ngrok encerrou a negociação TLS no ESP32 simulado. Trocar a âncora de confiança reduziu a demora, mas a troca ainda falhou.  
Com HTTP restrito ao gateway privado do notebook, o reteste retornou HTTP 200 na sincronização. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md) · [Registro do ensaio](roteiro-wokwi.md#registro-de-falha-e-reteste)

### 32. Qual foi a falha da barra e como foi corrigida?

No primeiro ensaio um segmento ficou apagado no brilho 10. O segmento ligado ao GPIO 1 (TX0) foi movido para o GPIO 5 no código e no circuito.  
Ao reiniciar a simulação com o diagrama atualizado, o reteste confirmou todos os segmentos acesos. [iluminacao.cpp:7](../firmware/sketch/iluminacao.cpp#L7) · [Registro do ensaio](roteiro-wokwi.md#registro-de-falha-e-reteste)

### 33. O que as provas registradas verificaram?

O relatório registra 13 testes HTTP, com casos de autenticação, sincronização, comandos e captura, incluindo dados adulterados e chave RSA errada.  
No Wokwi, o ensaio de imagem comparou o SHA-256 do JPEG salvo com o do arquivo de referência. [Relatório](relatorio.md) · [Roteiro de testes](roteiro-wokwi.md)

### 34. Onde a IA ajudou e como o resultado foi conferido?

A IA apoiou planejamento, documentação, código, roteiros e testes. O registro descreve as revisões, os testes HTTP, a compilação do ESP32 e as provas por execução.  
A regra da disciplina exige declarar o apoio e cada estudante deve conseguir explicar todo o código na defesa individual. [Registro de uso de IA](uso-ia.md) · [Roteiro da defesa](roteiro-apresentacao.md#defesa-individual)

## 6. Glossário rápido

| Termo | Significado no projeto |
|---|---|
| IoT, ESP32, firmware | IoT conecta a estação à rede. ESP32 é o microcontrolador. Firmware é o programa executado nele. |
| GPIO, nível baixo, pull-up | GPIO é um pino de entrada ou saída. Nível baixo representa tensão próxima de zero. Pull-up mantém a entrada em nível alto em repouso. |
| GPIO 1 / TX0 e GPIO 3 / RX0 | Pinos padrão de transmissão e recepção da porta serial do ESP32. |
| LDR, divisor de tensão, ADC, lux | O LDR varia a resistência com a luz. O divisor transforma a variação em tensão. O ADC digitaliza essa tensão. Lux (`lx`) mede iluminância. |
| Debounce | Espera pela estabilidade do contato antes de reconhecer a mudança do botão. |
| Barra e RGB | A barra ilumina com segmentos. O LED RGB combina vermelho, verde e azul para indicar estado. |
| RSSI, dBm, uptime | RSSI indica força do sinal Wi-Fi em dBm. Uptime é o tempo desde que a estação ligou. |
| API, HTTP, POST e JSON | API é a interface do servidor. HTTP carrega pedidos e respostas. POST envia dados para processamento. JSON representa os campos do pedido e da resposta. |
| Gateway e ngrok | O gateway encaminha o tráfego do simulador. O ngrok é um túnel para alcançar um serviço local de fora do notebook. |
| Localhost, TLS, HTTPS e CA | Localhost aponta para a própria máquina. TLS protege a conexão, HTTPS é HTTP sobre TLS e a CA é a autoridade usada para verificar a identidade do servidor. |
| Handshake e âncora de confiança | O handshake negocia a conexão TLS. A âncora é o certificado de confiança a partir do qual a cadeia do servidor é verificada. |
| HTTP Basic e X-Api-Key | Basic leva usuário e senha do operador. X-Api-Key leva a chave da estação em um cabeçalho HTTP. |
| CSRF | Requisição induzida por outro site usando credenciais que o navegador pode enviar automaticamente. Um token adicional protege alterações do painel. |
| JPEG, Base64, KiB | JPEG é o formato da imagem. Base64 representa bytes em texto. KiB equivale a 1.024 bytes. |
| AES-GCM, IV e tag | AES-GCM cifra com uma chave secreta. IV é o valor inicial da operação. A tag permite verificar se os bytes cifrados foram alterados. |
| RSA-OAEP e SHA-256 | RSA-OAEP protege a chave AES com um par público/privado. SHA-256 gera um resumo criptográfico e também é usado na configuração do OAEP. |
| H2, transação e metadados | H2 é o banco local. Uma transação reúne operações do banco. Metadados descrevem a captura, por exemplo amostra, lux e hora. |
| ADR e OWASP | ADR registra uma decisão de arquitetura. O OWASP IoT Top 10 lista categorias de risco em sistemas IoT. |

Para os termos do domínio, como berço, amostra, lote e captura, consulte o [glossário do projeto](../GLOSSARY.md).

Este guia foi redigido com apoio do Codex e conferido contra o relatório, as decisões e as linhas do código citadas. [Regra de uso de IA](uso-ia.md)
