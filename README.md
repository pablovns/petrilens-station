# PetriLens Station

Issues #2–#9: uma estação `est-01` ilumina a placa por presença e envia seu estado a cada 5 s; o botão e o painel podem solicitar a captura da imagem fictícia do SD, que a estação envia cifrada ao servidor Spring; o painel mostra as capturas da amostra selecionada em localhost. Online significa última sincronização há **até 15 s**, inclusive. O painel consulta a API a cada 2 s.

**Validação parcial.** O ensaio no Wokwi para VS Code confirmou a sincronização HTTP pelo gateway privado local. As provas anteriores cobrem a compilação do firmware e o servidor com curl em localhost. Falta concluir o ensaio de ponta a ponta no simulador. O [projeto Wokwi web](https://wokwi.com/projects/480987947564554241) segue como item de entrega.

## Servidor e prova local

Requisitos: JDK 21 ou superior (verificado com Java 25), Maven 3.9, OpenSSL para gerar as chaves, curl e Python 3 para a prova local. Spring Boot 3.5.16, alvo Java 21.

Execute da raiz. O script gera o par RSA 2048 em `~/.config/petrilens`, escolhe uma chave aleatória para a estação e solicita o usuário e a senha do operador. Preserva o par e o `.env` se já existirem. [Credenciais da demonstração](docs/credenciais.md)

```sh
./scripts/preparar-demo.sh
mvn -f servidor/pom.xml package
java -jar servidor/target/servidor-0.0.1-SNAPSHOT.jar
```

O servidor lê o `.env` do **diretório de execução** via Spring. Use a raiz na demo. O formato é Java properties (`NOME=valor`, sem `export`, aspas ou expansão de variáveis) e variáveis de ambiente de mesmo nome têm precedência. Chave da estação, usuário e senha vazios impedem a inicialização. O script grava em `CAPTURA_CHAVE_PRIVADA` o caminho absoluto do PEM privado PKCS#8. Com essa chave indisponível, o servidor responde 503 a um POST de captura válido. Após trocar o par, reinicie o servidor e regenere o `secrets.h` com a nova chave pública.

Mantenha o servidor em primeiro plano. Para a prova curl, copie `scripts/preparar-demo.sh` para a pasta `scripts/` de um diretório descartável criado com `mktemp -d` e execute a cópia com um `HOME` temporário dentro desse diretório. Informe credenciais sintéticas. Inicie o JAR com caminho absoluto a partir do diretório descartável, onde o script gravou o `.env`. Assim, o H2, os JPEGs e as chaves da prova ficam separados dos dados da demo.

Em outro terminal Bash, a partir da raiz do repositório, exporte o caminho PEM da chave pública de teste e as três credenciais sintéticas do mesmo servidor. O script lê as credenciais do ambiente. Evite colocar valores no histórico ou nos argumentos e mantenha `set -x` desligado.

```sh
read -rsp 'Chave da estação: ' ESTACAO_API_KEY; printf '\n'
read -rsp 'Usuário do painel: ' PAINEL_USUARIO; printf '\n'
read -rsp 'Senha do painel: ' PAINEL_SENHA; printf '\n'
read -rp 'Caminho do PEM da chave pública de teste: ' CAPTURA_CHAVE_PUBLICA_PEM
export ESTACAO_API_KEY PAINEL_USUARIO PAINEL_SENHA CAPTURA_CHAVE_PUBLICA_PEM
./scripts/teste-local.sh
unset ESTACAO_API_KEY PAINEL_USUARIO PAINEL_SENHA CAPTURA_CHAVE_PUBLICA_PEM
```

O script obtém CSRF, **muda a seleção para A5**, envia [exemplos/sincronizacao.json](exemplos/sincronizacao.json), confirma a seleção devolvida e simula o POST seguinte informando A5. Verifica por JSON a igualdade entre seleção e amostra informada, `comandos: []` e `online: true`; cifra o JPEG fictício para A5, mede o corpo HTTP, consulta a lista e compara os bytes do JPEG recuperado com a fixture. A prova termina com `OK` se tudo passar. Cada execução acrescenta uma captura. Para outra porta local de teste, defina `PETRILENS_URL_BASE=http://127.0.0.1:PORTA`. Não imprime credenciais nem as passa em argumentos do curl. Pare de enviar sincronizações e, após **mais de 15 s**, consulte o painel autenticado.

Esperado: `online: false`, preservando os campos e o horário recebidos. Abra <http://localhost:8080> e informe o usuário e a senha do `.env` no diálogo Basic do navegador para acompanhar no painel. Interrompa o servidor com Ctrl+C. Ao terminar a prova, remova o diretório temporário criado para ela.

Para executar só os testes: `mvn -f servidor/pom.xml test`. O teste de integração usa servidor HTTP real em porta aleatória e relógio controlado: estado inicial, POST, GET, limite de 15 s, offline após 15 s, nova sincronização, validação de campos e JSON malformado. Os testes HTTP também cobrem chaves e login ausentes/incorretos, acesso permitido, isolamento dos mecanismos, proteção dos assets, JSON de erros, CSRF e ausência de sessão. A prova da seleção usa exclusivamente HTTP e dois contextos reais em portas aleatórias sobre o mesmo H2 temporário: catálogo, seleção inicial, troca, 400 sem alteração, CSRF ausente/incorreto (403), propagação e conservação após fechar/reabrir o servidor. Os testes anteriores usam H2 em memória isolado e não criam banco na raiz. Não há teste automatizado do firmware ou do painel.

O painel permite solicitar captura, pedir status e ajustar brilho. Os comandos ficam no H2 e aparecem como pendentes até serem enviados à estação, um por sincronização. Entregue significa que o servidor incluiu o comando na resposta, sem confirmação de execução. O H2 em arquivo fica em `dados/petrilens.mv.db`, relativo ao diretório de execução, assim como `.env`. **Execute sempre o JAR da raiz**; o goal `spring-boot:run` do Maven pode usar `servidor/` como diretório e criar outro banco/configuração. O catálogo idempotente contém L2026-001 (A1–A3) e L2026-002 (A4–A6). Somente no banco novo a seleção começa em A1; reinícios conservam a escolha. Os JPEGs originais ficam em `dados/capturas/<id>.jpg`, com identificadores numéricos gerados pelo servidor. Metadados e seleção persistem no H2. Não apague `dados/` para reiniciar: perde seleção e capturas. Faça backup/restauração do banco e das imagens juntos, com o servidor parado. Todo o diretório `dados/` é ignorado pelo Git. O arquivo é gravado antes do registro H2; falhas tratadas removem o arquivo, e falhas de disco não registram captura. Uma interrupção abrupta entre a escrita e o INSERT pode deixar arquivo órfão, sem captura consultável; não há recuperação automática nesta fatia. O último estado informado permanece em memória e desaparece ao reiniciar. O servidor escuta exclusivamente `127.0.0.1`; **um túnel expõe a API mesmo assim**, portanto use somente dados fictícios e encerre-o após a validação supervisionada. A estação envia `X-Api-Key`; painel, assets e demais APIs exigem HTTP Basic. Uma credencial não substitui a outra. Não há sessão no servidor nem senha gerada automaticamente. Na demo, o painel usa HTTP em localhost e a estação usa o gateway privado local. Para acesso fora do notebook, use HTTPS com validação do certificado.

## Firmware local

Requisitos já usados: arduino-cli 1.6.0, core `esp32:esp32` 3.3.13 e ArduinoJson 7.4.3. Dependência declarada em `firmware/sketch/libraries.txt`. Se faltarem no ambiente de quem reproduzir:

```sh
arduino-cli core install esp32:esp32@3.3.13
arduino-cli lib install ArduinoJson@7.4.3
```

Sem `secrets.h`, a compilação usa `secrets.example.h`, com valores vazios. A sincronização fica bloqueada. Sensores, iluminação e Wi-Fi continuam funcionando. Para um ESP32 real, use um servidor HTTPS com a CA correspondente. O ngrok é uma alternativa de túnel HTTPS para o dispositivo real. [Configuração HTTPS](docs/credenciais.md#modo-https-opcional)

O sketch plano pode ser copiado 1:1 para o Wokwi web, incluindo `diagram.json`. `rede.cpp` cuida do Wi-Fi `Wokwi-GUEST`; `cliente_servidor.cpp` concentra URL, transporte, certificado, cabeçalhos e JSON. `sketch.ino` coordena sensores, iluminação, status e sincronização com presença, lux, brilho, RSSI e uptime. A amostra começa nula; a sincronização recebe a seleção, copia as strings do JSON para memória própria e informa o identificador no POST seguinte.

Com placa presente, o LED bar acende o número de segmentos configurado (10 por padrão); sem placa, apaga, inclusive sem rede ou configuração do servidor. O RGB fica vermelho sem Wi-Fi, amarelo conectado sem placa e verde conectado com placa. O botão tem debounce de 30 ms e dispara só na borda de pressão: segurar não produz sequência. Com placa presente, o botão provoca uma sincronização antes de capturar. Se a sincronização falhar, a estação recusa a captura. O comando do painel usa a seleção da mesma resposta que o trouxe. Ambos verificam presença e usam a amostra e o lux do momento da captura. Retirada antes de iniciar o envio recusa a captura. Não há captura automática por presença. O RGB fica azul ao ler/enviar a imagem e vermelho por 3 s na recusa/falha; depois volta a amarelo/verde conforme sensores e Wi-Fi. Sem Wi-Fi o vermelho fica fixo. O envio usa até 3 tentativas com os mesmos dados e imagem, sem fila offline; após a terceira falha descarta. Como não há idempotência, uma resposta perdida após o servidor salvar pode gerar captura duplicada em uma tentativa seguinte. A fórmula nominal do LDR usa 3,3 V e ADC de 12 bits, limitando extremos para manter lux finito; a conferência na simulação está pendente. As chamadas HTTP e HTTPS são síncronas: durante uma tentativa, o laço local aguarda os timeouts do cliente.

| Componente | GPIO / ligação |
|---|---|
| LDR AO | 34; VCC 3V3, GND |
| Presença, terminal central 2 | 35; terminal 1 GND, terminal 3 livre; pull-up externo de 10 kΩ para 3V3; posição ligada ao terminal 1 = presente |
| Botão | 36 (VP), contato oposto GND; pull-up externo de 10 kΩ para 3V3 |
| LED bar, segmentos 1–10 | 5, 3, 4, 13, 14, 16, 17, 21, 22, 27; resistor individual de 1 kΩ por anodo, cátodos GND |
| RGB R/G/B | 25/26/32, resistor de 1 kΩ por canal; comum GND |
| microSD SCK/DO/DI/CS | 18/19/23/33; VCC 3V3, GND |

GPIOs 34–36 não possuem pull-up interno. O GPIO 3 pertence à iluminação, portanto a UART não é inicializada e as mensagens Serial do cliente não aparecem. Confira o estado pelo RGB e pelo painel; o boot pode causar um transiente nesses segmentos. O módulo `imagem.cpp` abre `/placa-base64.txt`, decodifica o JPEG em blocos e guarda os bytes cifrados em um buffer binário de até 30.016 bytes. `captura.cpp` coordena o fluxo e `cliente_servidor.cpp` monta o upload com prefixo JSON, Base64 da imagem cifrada em blocos e fechamento. O Content-Length é a soma exata dessas partes. As tentativas reutilizam a imagem cifrada.

## Imagem fictícia e cifragem

A fixture [exemplos/placa-ficticia.jpg](exemplos/placa-ficticia.jpg) é um desenho JPEG 800×600, 28.754 bytes (menos de 30 KB). Seu Base64 sem quebras de linha está em `firmware/sketch/placa-base64.txt`. O arquivo `exemplos/captura.json` mostra a estrutura do pedido com marcadores para os campos cifrados. Para regenerar e conferir dimensões, tamanho e igualdade da decodificação, use `java -Djava.awt.headless=true exemplos/GerarImagem.java` da raiz (Java Graphics2D/ImageIO, sem dependência adicional).

O Wokwi para VS Code copia `sdcard/` para o microSD simulado a cada início. Nessa pasta, `placa-base64.txt` é um link simbólico para o Base64 do sketch. Mantenha o conteúdo sem quebras de linha. No projeto Wokwi web, o mesmo texto fica na raiz do projeto.

Cada captura recebe uma chave aleatória AES de 128 bits e IV aleatório de 12 bytes. O firmware usa AES-GCM e anexa a tag de 16 bytes ao ciphertext. Usa RSA 2048 com OAEP SHA-256 e MGF1 SHA-256 para cifrar a chave AES. A estação transmite `chave_cifrada`, `iv` e `imagem_cifrada` em Base64. A API usa a chave privada do servidor para decifrar a imagem. Com o túnel opcional, o ngrok termina o TLS e vê os metadados. Use apenas a fixture fictícia no Wokwi público.

Os testes HTTP geram chaves efêmeras e verificam o download do JPEG byte a byte, a associação à amostra informada, validação de campos e JPEG, adulteração, chave errada ou ausente, autenticação, persistência e falha de disco. O limite de 65.536 bytes vale para o corpo inteiro e os testes cobrem o limite exato e o excesso com Content-Length ou chunked. Firmware e painel são verificados manualmente no simulador.

## Simular no Wokwi para VS Code

1. Suba o servidor conforme a seção [Servidor e prova local](#servidor-e-prova-local).
2. Abra `firmware/sketch` como pasta do projeto no VS Code. Instale a extensão Wokwi (versão 4.1.0 no ensaio). Na paleta (`F1`), execute **Wokwi: Request a New License** para obter a licença pessoal gratuita de 30 dias.
3. Da raiz do repositório, gere a configuração e compile com o comando de `wokwi.toml`:

   ```sh
   ./scripts/gerar-secrets.sh http://host.wokwi.internal:8080
   arduino-cli compile --fqbn esp32:esp32:esp32doit-devkit-v1 --build-path firmware/sketch/build firmware/sketch
   ```

4. No VS Code, use `F1` → **Wokwi: Start Simulator**. Abra o painel em <http://localhost:8080> e autentique-se. Confira `est-01: online` e a atualização da última sincronização.

### Particularidades do simulador

- **A simulação pausa quando a aba do simulador não está visível.** O relógio para, a estação deixa de sincronizar e o painel passa a offline depois de 15 s. Deixe o VS Code e o navegador lado a lado.
- **Mudou o `diagram.json`?** Reiniciar ou recompilar recarrega só o firmware. Feche a aba do simulador e rode `F1` → **Wokwi: Start Simulator** de novo. Recompilar o firmware reinicia a simulação sozinho.
- O tooltip do Wi-Fi do simulador pode dizer **"Public IoT Gateway"** e mesmo assim funcionar; vale o que o painel mostra.
- **Não há Monitor Serial:** o GPIO 3 é usado pela barra de LEDs. O estado aparece no LED RGB e no painel.
- **Cores do RGB:** amarelo = Wi-Fi ok e sem placa; verde = com placa; azul = capturando e enviando; vermelho por 3 s = captura recusada ou falha (fixo = sem Wi-Fi). A cor não muda ao mexer no LDR: a luz só é conferida no disparo.
- **Onde operar:** a presença é a chave deslizante logo abaixo do LDR (esquerda = placa presente); o LDR muda pelo controle de lux ao clicar nele; a captura é o botão azul. Para capturar, deixe a leitura de **Luz medida** no painel entre 300 e 700 lx.

O gateway privado embutido encaminha o HTTP ao servidor no mesmo notebook. Para concluir o ensaio de ponta a ponta, siga o roteiro de testes. Use somente dados fictícios e mantenha `secrets.h` local. [Configuração do Wokwi](firmware/sketch/wokwi.toml) · [ADR 0008](docs/adr/0008-wokwi-vscode-com-gateway-local.md) · [Roteiro de testes](docs/roteiro-wokwi.md)

Contrato atual e decisões do projeto. [API](docs/api.md) · [Decisões](docs/decisoes.md) · [ADRs](docs/adr) · [Glossário](GLOSSARY.md)

## Recusa por lux (#9)

A captura exige placa presente e uma leitura de **300 a 700 lx**, inclusive. No Wokwi, o controle do LDR é independente do LED bar. Com a estação online e a placa no berço, execute cada passo pelo botão e pelo comando **Capturar** no painel. Aguarde a sincronização e a atualização do painel entre as tentativas.

1. Dentro da faixa, ajuste o LDR para 500 lx. Confirme uma nova miniatura na amostra selecionada.
2. Fora da faixa, ajuste para 250 lx e depois para 800 lx. Em cada tentativa, confira o RGB vermelho por 3 s, o aviso **Captura recusada: luz fora da faixa ideal.** e a ausência de uma nova miniatura.
3. Nos limites, ajuste para 300 lx e depois para 700 lx. Confira uma nova captura em cada limite e o desaparecimento do aviso após uma sincronização. Use a leitura da estação para conferir o ajuste. Se a resolução do ADC impedir uma leitura exata no simulador, registre a leitura obtida. Os limites exatos também são verificados na compilação.

O aviso vem do campo opcional `motivo_recusa` no estado sincronizado. Corpos de clientes antigos continuam aceitos sem esse campo. [Contrato da API](docs/api.md#post-apiestacaosincronizacao)
