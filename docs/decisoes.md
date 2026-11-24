# Decisões do grill (17/11/2026)

Decisões de comportamento e implementação tomadas na sessão de grill, antes da spec. As decisões difíceis de reverter estão em `docs/adr/`; os termos, em `GLOSSARY.md`. Este arquivo registra o resto, para não depender do contexto da sessão.

## Captura e amostra

- O operador sempre tem uma amostra selecionada no painel; toda captura nova é vinculada a ela, e uma amostra pode ter várias capturas.
- A amostra selecionada fica no servidor, como estado da estação, e chega à estação em toda sincronização. A estação carimba cada captura com a amostra no momento da captura.
- A captura só é disparada pelo botão físico ou pelo comando `capturar` do painel, e exige placa presente. Não há disparo automático nem modos automático/manual (ADR 0006).
- A placa presente acende o LED bar e a placa ausente apaga (R1).
- Antes de capturar pelo botão, a estação sincroniza (ADR 0005).
- O servidor marca o horário da captura quando ela chega; a estação não usa NTP.
- O LDR é lido e o lux vai no estado e na captura (ADR 0006).

### Recusa por lux (#9)

- A verificação local acontece na leitura de captura, com placa presente e amostra da sincronização. O firmware aceita de **300 a 700 lx**, com os dois limites inclusivos, e recusa leituras não finitas.
- A faixa serve para demonstrar o controle do LDR simulado em torno do padrão de 500 lx do Wokwi. A calibração de laboratório fica fora desta demonstração.
- Uma recusa por lux acende o RGB vermelho por 3 s e é informada nas sincronizações seguintes, até uma tentativa passar pela verificação de luz. O painel mostra o aviso da estação. A captura recusada não lê nem envia a imagem.

## Comunicação

- Sincronização a cada 5 s: a estação envia o estado (presença, lux, brilho, amostra em uso, RSSI, uptime) e recebe os comandos pendentes e a amostra selecionada (ADR 0005).
- Estação sem sincronizar por mais de 15 s aparece como offline no painel.
- Comandos: `capturar`, `status` e `ajustar_brilho`.
  - `status` faz a estação sincronizar na hora, sem esperar o intervalo.
  - `ajustar_brilho` recebe um inteiro de 0 a 10, o número de segmentos do LED bar acesos com a placa presente. Padrão: 10. Fora da faixa, o servidor recusa com erro de validação, e a estação também recusa.
- Envio da captura: até 3 tentativas. Se todas falharem, o LED mostra erro e a captura é descartada. Não há fila offline.
- A imagem vai como texto Base64 dentro do JSON da captura; o servidor decodifica e salva o JPEG (ADR 0004).
- Limite da captura: 64 KB. Um JPEG 800x600 de teste com qualidade 75 tem 26,5 KB (35,3 KB em Base64), então a captura fica em torno de 36 a 40 KB. A estação envia o Base64 em partes, sem montar uma String única na memória.
- A demo usa o gateway privado do Wokwi para VS Code e abre o painel em `localhost`. O túnel ngrok previsto na ADR 0003 fica como alternativa HTTPS para um ESP32 real. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)

## Imagem cifrada (ADR 0007)

- A estação cifra a imagem com AES-GCM (chave aleatória por captura, IV de 12 bytes) e cifra a chave AES com a chave pública RSA 2048 do servidor (OAEP com SHA-256 também no MGF1).
- A captura leva, em Base64: a chave cifrada, o IV e a imagem cifrada (com a tag do GCM). O tamanho fica praticamente o mesmo e cabe nos 64 KB.
- A chave pública fica no firmware (pode ser pública). A chave privada fica num arquivo do servidor indicado no `.env`, fora do repositório; o README explica como gerar o par.
- No modo HTTPS, a estação valida o certificado do servidor com a CA configurada. O modo HTTP fica restrito ao gateway privado local. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)
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
- A chave da estação e o login do painel autenticam as requisições. Na demo, o tráfego HTTP fica no notebook.
- O checklist OWASP IoT Top 10 registra os riscos do projeto; a correção implementada é a imagem cifrada para a API (ADR 0007).

## Usuário

- Um único ator, o operador (técnico de laboratório na bancada).

## Decisões a revisar (orquestração final)

Decisões tomadas sem definição na SPEC durante a orquestração final, para revisão do grupo.

| Decisão | Motivo | Alternativa descartada | Arquivos ou commits |
|---|---|---|---|
| Versionar o PDF gerado em `docs/relatorio.pdf`. | O artefato conferido fica disponível no repositório e é regenerado após a #11. | Guardar só o Markdown. | `docs/relatorio.pdf`, `40373f7` |
| Gerar o PDF com Pandoc e Typst (`scripts/gerar-relatorio-pdf.sh`), Noto Serif 11 pt, A4, quebras de página em blocos Typst no Markdown. | Typst é um binário único, sem instalar LaTeX; o script falha fora de 8 a 15 páginas. | LaTeX (texlive-xetex) ou template Typst próprio. Os blocos ```` ```{=typst} ```` aparecem como código no GitHub. | `scripts/gerar-relatorio-pdf.sh`, `docs/relatorio.md`, `40373f7` |
| Diagramas como PNG (escala 3) gerados pelo Mermaid CLI, com o `.mmd` versionado como fonte. | O Typst perde o texto de SVG Mermaid com `foreignObject`. | SVG ou renderizar a cada geração do PDF. | `docs/diagramas/`, `40373f7` |
| No PDF, links relativos viram URLs `github.com/pablovns/petrilens-station/blob/integracao/...` por filtro Lua. | Links relativos não funcionam num PDF solto; o Markdown fica igual. | Manter só o texto do link. | `scripts/links-relatorio.lua`, `40373f7` |
| Não mover sincronização e envio para uma tarefa FreeRTOS separada, apesar de bloquearem o `loop()` durante o TLS. | Simples e funcionando; só muda se o ensaio mostrar atraso relevante na luz ou no botão. | Tarefa de trabalho dedicada ao fluxo bloqueante. | `firmware/sketch/sketch.ino` (sem mudança) |
| Scripts `preparar-demo.sh` e `gerar-secrets.sh` versionados para gerar par RSA, `.env` e `secrets.h` sem exibir valores; CA ISRG Root X1 do repositório de confiança do sistema para o túnel ngrok. | Passos manuais longos eram fáceis de errar e expunham valores no terminal; a raiz sobrevive à renovação da folha Let's Encrypt. | Preencher arquivos à mão; fixar o certificado folha ou a intermediária YE2. | `scripts/`, `docs/credenciais.md`, `README.md` |
| Projeto Wokwi salvo como **público** (`wokwi.com/projects/480987947564554241`). | O modo Unlisted exige plano pago; a ADR 0003 já aceita o projeto público com token exclusivo da demo. | Unlisted (pago). | `docs/entrega.md` (link) |
| Plano pago do Wokwi (Hobby, gateway privado) descartado pelo grupo em 17/11/2026. A saída inicial foi aguardar a recuperação do gateway público (ADR 0003). | Custo fora de cogitação. | Assinar o Wokwi Hobby. | — |
| Usar Wokwi para VS Code com gateway privado local e HTTP restrito a `host.wokwi.internal`. | Gateway público instável e handshake TLS com a borda do ngrok interrompido no ESP32 simulado. | Hobby pago no web, âncora na YE2 e desativar a validação TLS. | [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md), `ebdb136` |
