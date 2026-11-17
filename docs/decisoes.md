# Decisões do grill (17/11/2026)

Decisões de comportamento e implementação tomadas na sessão de grill, antes da spec. As decisões difíceis de reverter estão em `docs/adr/`; os termos, em `GLOSSARY.md`. Este arquivo registra o resto, para não depender do contexto da sessão.

## Captura e amostra

- O operador sempre tem uma amostra selecionada no painel; toda captura nova é vinculada a ela, e uma amostra pode ter várias capturas.
- A amostra selecionada fica no servidor, como estado da estação, e chega à estação em toda sincronização. A estação carimba cada captura com a amostra no momento da captura.
- A captura só é disparada pelo botão físico ou pelo comando `capturar` do painel, e exige placa presente. Não há disparo automático nem modos automático/manual (ADR 0006).
- A placa presente acende o LED bar e a placa ausente apaga (R1).
- Antes de capturar pelo botão, a estação sincroniza (ADR 0005).
- O servidor marca o horário da captura quando ela chega; a estação não usa NTP.
- O LDR é lido e o lux vai no estado e na captura. Bloquear a captura por lux fora da faixa é desejável, mas de menor prioridade (ADR 0006).

## Comunicação

- Sincronização a cada 5 s: a estação envia o estado (presença, lux, brilho, amostra em uso, RSSI, uptime) e recebe os comandos pendentes e a amostra selecionada (ADR 0005).
- Estação sem sincronizar por mais de 15 s aparece como offline no painel.
- Comandos: `capturar`, `status` e `ajustar_brilho`.
  - `status` faz a estação sincronizar na hora, sem esperar o intervalo.
  - `ajustar_brilho` recebe um inteiro de 0 a 10, o número de segmentos do LED bar acesos com a placa presente. Padrão: 10. Fora da faixa, o servidor recusa com erro de validação, e a estação também recusa.
- Envio da captura: até 3 tentativas. Se todas falharem, o LED mostra erro e a captura é descartada. Não há fila offline.
- A imagem vai como texto Base64 dentro do JSON da captura; o servidor decodifica e salva o JPEG (ADR 0004).
- Limite da captura: 64 KB. Um JPEG 800x600 de teste com qualidade 75 tem 26,5 KB (35,3 KB em Base64), então a captura fica em torno de 36 a 40 KB. A estação envia o Base64 em partes, sem montar uma String única na memória.
- Túnel ngrok gratuito, validado antes do resto do fluxo; o painel é aberto por `localhost` (ADR 0003).

## Imagem cifrada (ADR 0007)

- A estação cifra a imagem com AES-GCM (chave aleatória por captura, IV de 12 bytes) e cifra a chave AES com a chave pública RSA 2048 do servidor (OAEP com SHA-256 também no MGF1).
- A captura leva, em Base64: a chave cifrada, o IV e a imagem cifrada (com a tag do GCM). O tamanho fica praticamente o mesmo e cabe nos 64 KB.
- A chave pública fica no firmware (pode ser pública). A chave privada fica num arquivo do servidor indicado no `.env`, fora do repositório; o README explica como gerar o par.
- A estação valida o certificado HTTPS do servidor (autoridade certificadora identificada na validação do caminho Wokwi → ngrok → servidor), sem `setInsecure()`.
- Imagem que não decifra (chave errada, dado adulterado) é recusada com erro 400.
- Esta é a correção derivada do checklist OWASP; o checklist ainda é escrito, e o risco de transferência insegura de dados aparece nele com esta correção.

## Estados do LED RGB (R5)

| Cor | Estado |
|---|---|
| Amarelo | Conectada, aguardando a placa |
| Verde | Pronta: conectada e com a placa no berço |
| Azul | Capturando e enviando |
| Vermelho | Erro (sem rede, envio falhou, captura recusada). Fica 3 s e volta ao estado anterior; sem rede, fica fixo |

## Painel

- Aplicação web separada da estação, servida pelo próprio servidor e aberta no navegador (`localhost`).
- HTML/JS estático, sem build de frontend, consultando a API a cada 2 s.
- Organizado em torno da amostra selecionada: seletor da amostra, estado da estação (incluindo qual amostra a estação informou), botões de comando e uma tabela com as capturas da amostra (horário, lux, miniatura). Sem gráfico e sem histórico geral.
- Login com um único usuário (o operador), HTTP Basic, com usuário e senha definidos no `.env`. Pode melhorar depois.

## Servidor

- Java/Spring com Maven; versões decididas na implementação (ADR 0002).
- Banco H2 em arquivo. Na inicialização, carrega 2 lotes com 3 amostras cada (`L2026-001`, `A1`, ...); a amostra selecionada inicial é a primeira.
- Pacotes por funcionalidade (por exemplo `captura`, `amostra`, `estacao`), com os transversais (segurança, erros) separados.
- Uma única estação, `est-01`, configurada no servidor, sem tela de cadastro.
- A estação se autentica com um token no cabeçalho; o token fica no `.env` do servidor e no `secrets.h` do firmware, nenhum dos dois versionado.

## Firmware

- Sketch Arduino em pasta plana: `firmware/sketch/` com `sketch.ino`, módulos `.cpp`/`.h`, `diagram.json`, `libraries.txt` e o arquivo Base64 da imagem. Copiado 1:1 para o Wokwi web e compilado localmente com `arduino-cli`.
- Só um módulo conhece a URL e o protocolo do servidor; a URL fica na configuração.

## Segurança

- Credenciais fora do repositório, com arquivos de exemplo (`.env.example`, `secrets.example.h`).
- Token da estação e login do painel contam como autenticação na comunicação; o ngrok fornece HTTPS.
- O checklist OWASP IoT Top 10 registra os riscos do projeto; a correção implementada é a imagem cifrada para a API (ADR 0007).

## Usuário

- Um único ator, o operador (técnico de laboratório na bancada).
