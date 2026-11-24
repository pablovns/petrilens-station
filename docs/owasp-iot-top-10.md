# Checklist OWASP IoT Top 10 (2018)

Avaliação da issue #10 para a demonstração acadêmica da PetriLens. O escopo abrange uma estação ESP32 simulada no Wokwi para VS Code, o gateway privado e o servidor no notebook, com o painel aberto por localhost. Todos os dados da demo são fictícios, incluindo os lotes, as amostras, a telemetria simulada e o JPEG da placa. Os status descrevem o que foi inspecionado no código integrado e os riscos aceitos nesse escopo. [OWASP IoT Top 10 — referência oficial](https://owasp.org/www-project-internet-of-things/) · [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md) · [Contrato da API](api.md)

## I1 — Weak, Guessable, or Hardcoded Passwords

**Aceito.** O operador usa HTTP Basic e a estação usa uma chave no cabeçalho `X-Api-Key`. As credenciais do servidor vêm de `.env` ou do ambiente, e a configuração local da estação fica em `secrets.h`. Esses arquivos são ignorados pelo Git. Ainda assim, qualquer pessoa com acesso ao projeto Wokwi público pode obter a chave configurada ali e se passar por `est-01` enquanto ela estiver ativa. A chave deve ser exclusiva e descartável para a demo. A senha do operador e a chave privada RSA ficam no notebook. [Credenciais.java](../servidor/src/main/java/br/com/petrilens/seguranca/Credenciais.java) · [Seguranca.java](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java) · [.gitignore](../.gitignore) · [ADR 0003](adr/0003-simulacao-no-wokwi-web.md)

## I2 — Insecure Network Services

**Mitigado.** A estação opera como cliente Wi-Fi e inicia as conexões de sincronização e de envio de captura, sem serviço de entrada no firmware. O servidor escuta em `127.0.0.1`. Na demo, o gateway privado encaminha as requisições da estação ao servidor no mesmo notebook. A API e o painel exigem autenticação. [rede.cpp](../firmware/sketch/rede.cpp) · [cliente_servidor.cpp](../firmware/sketch/cliente_servidor.cpp) · [application.properties](../servidor/src/main/resources/application.properties) · [README](../README.md)

## I3 — Insecure Ecosystem Interfaces

**Mitigado.** A API da estação exige `X-Api-Key`. O painel, seus arquivos estáticos, as consultas de captura e o download do JPEG exigem HTTP Basic do operador. As credenciais não são intercambiáveis. O servidor mantém proteção CSRF nas alterações do painel, valida os campos da sincronização e limita o corpo da captura a 64 KiB antes de interpretar o JSON. O uso indevido da chave de demo continua possível nas rotas da estação (I1). [Seguranca.java](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java) · [EstadoInformado.java](../servidor/src/main/java/br/com/petrilens/estacao/EstadoInformado.java) · [CapturaController.java](../servidor/src/main/java/br/com/petrilens/captura/CapturaController.java) · [AutenticacaoHttpTest.java](../servidor/src/test/java/br/com/petrilens/AutenticacaoHttpTest.java)

## I4 — Lack of Secure Update Mechanism

**Não se aplica.** No escopo da estação simulada, o grupo atualiza o firmware copiando o sketch para o Wokwi. Não há atualização remota (OTA) nem mecanismo de distribuição de firmware implantado. A edição manual do sketch não oferece garantias de atualização segura para uma estação física. [Proposta](proposta.md) · [README](../README.md)

## I5 — Use of Insecure or Outdated Components

**Aceito.** O Maven identifica a versão do Spring Boot e declara os componentes do servidor, incluindo o H2. O firmware declara a versão do ArduinoJson, e o README registra a versão do core ESP32 usada na compilação local. Isso ajuda a reproduzir o ambiente. A avaliação não inclui auditoria de CVEs, de dependências transitivas ou dos serviços de terceiros Wokwi e ngrok. O risco de componentes vulneráveis permanece. [pom.xml](../servidor/pom.xml) · [libraries.txt](../firmware/sketch/libraries.txt) · [README](../README.md)

## I6 — Insufficient Privacy Protection

**Mitigado.** A demo usa um catálogo fictício e um desenho de placa de Petri no microSD simulado, sem dados pessoais ou amostras de laboratório reais. O painel exige autenticação para consultar as capturas. O JPEG de origem pode ser visto no projeto Wokwi público. A mitigação depende de manter os dados fictícios. O campo `simulacao` é uma declaração no pedido e não comprova a origem da imagem. [data.sql](../servidor/src/main/resources/data.sql) · [JPEG fictício](../exemplos/placa-ficticia.jpg) · [CapturaInformada.java](../servidor/src/main/java/br/com/petrilens/captura/CapturaInformada.java) · [ADR 0003](adr/0003-simulacao-no-wokwi-web.md)

## I7 — Insecure Data Transfer and Storage

**Mitigado.** A correção implementada na #8, conforme a ADR 0007, é cifrar **a imagem para a API** antes do envio. A estação gera uma chave AES de 128 bits e um IV de 12 bytes aleatórios por captura, cifra o JPEG com AES-GCM e cifra a chave AES com a chave pública RSA de 2048 bits do servidor (OAEP com SHA-256 e MGF1 com SHA-256). A API usa a chave privada mantida fora do repositório para recuperar a chave AES e verifica a tag GCM antes de aceitar o JPEG. O código de teste cobre a recusa de adulteração e de chave incorreta sem registrar a captura. [ADR 0007](adr/0007-imagem-cifrada-para-a-api.md) · [imagem.cpp](../firmware/sketch/imagem.cpp) · [ImagemCifrada.java](../servidor/src/main/java/br/com/petrilens/captura/ImagemCifrada.java) · [CapturaHttpTest.java](../servidor/src/test/java/br/com/petrilens/CapturaHttpTest.java)

A demo usa HTTP pelo gateway privado para o servidor em localhost. O grupo aceita que a chave de API, a sincronização e os metadados trafeguem sem TLS dentro do notebook. A imagem continua cifrada até a API. Em um ESP32 real na rede do laboratório, use HTTPS com validação da CA. Com o túnel opcional, o ngrok é o terminador TLS e pode ler cabeçalhos e metadados. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)

No notebook, o servidor grava o JPEG recuperado sem cifragem pela aplicação e mantém os metadados no H2. A proteção em repouso depende do acesso local aos arquivos e ao notebook. [cliente_servidor.cpp](../firmware/sketch/cliente_servidor.cpp) · [secrets.example.h](../firmware/sketch/secrets.example.h) · [Capturas.java](../servidor/src/main/java/br/com/petrilens/captura/Capturas.java) · [Contrato da API](api.md)

## I8 — Lack of Device Management

**Aceito.** O painel dá visibilidade básica sobre a única estação configurada, `est-01`, marcando-a offline após mais de 15 segundos sem sincronização. O servidor mantém o último estado informado em memória. A troca da chave da estação é manual, com alteração da configuração do servidor e do firmware. Também se aceita a ausência de fila offline: a captura requer conexão e sincronização prévia, o envio tenta até três vezes e, se falhar, a estação sinaliza erro e descarta a captura sem reenviá-la após reconectar. [EstacaoController.java](../servidor/src/main/java/br/com/petrilens/estacao/EstacaoController.java) · [captura.cpp](../firmware/sketch/captura.cpp) · [cliente_servidor.cpp](../firmware/sketch/cliente_servidor.cpp) · [Decisões do projeto](decisoes.md)

## I9 — Insecure Default Settings

**Mitigado.** O servidor recusa a inicialização com credenciais da aplicação ausentes, vazias ou em branco. Com o exemplo vazio, a estação impede comunicação com a API até configurar a chave e a URL. A CA é obrigatória no modo HTTPS. Sem a chave pública RSA, o firmware também bloqueia o envio da captura. Esses padrões evitam expor a API com uma senha de fábrica ou permitir HTTP fora do endereço reservado ao gateway privado. A qualidade das credenciais preenchidas e a CA escolhida dependem da configuração local. [Credenciais.java](../servidor/src/main/java/br/com/petrilens/seguranca/Credenciais.java) · [.env.example](../.env.example) · [secrets.example.h](../firmware/sketch/secrets.example.h) · [cliente_servidor.cpp](../firmware/sketch/cliente_servidor.cpp)

## I10 — Lack of Physical Hardening

**Não se aplica.** A estação entregue é simulada, com circuito virtual e JPEG no microSD simulado. Resistência a violação física, acesso a pinos de depuração e extração de dados de um cartão ou ESP32 real ficam fora da entrega. [Diagrama do circuito](../firmware/sketch/diagram.json) · [ADR 0003](adr/0003-simulacao-no-wokwi-web.md) · [ADR 0004](adr/0004-camera-simulada-por-jpeg-no-sd.md)

## Verificação na demonstração

O reteste confirmou a sincronização HTTP pelo gateway privado. O ensaio de ponta a ponta ainda precisa verificar offline, captura cifrada com comparação do JPEG recuperado, recusa com outra chave pública e ausência de reenvio após reconectar. Os casos de CA se aplicam somente ao modo HTTPS. [Roteiro de testes](roteiro-wokwi.md)
