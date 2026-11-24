# Wokwi para VS Code com gateway privado local

Em 17/11/2026, entre 22:30 e 23:20 (-03), o gateway público do Wokwi web (`netwi.wokwi.com`) estava instável. O TLS com o gateway fechava, mas o serviço não respondia. Na simulação, o Wi-Fi ficou sem conexão e o RGB permaneceu vermelho. [ADR 0003](0003-simulacao-no-wokwi-web.md)

Com o gateway privado, o Wi-Fi associou (IP `10.13.37.2`) e o DNS resolveu o domínio do ngrok. A borda do ngrok encerrou o handshake TLS com erro mbedTLS `-0x0050` (`MBEDTLS_ERR_NET_CONN_RESET`). Com a âncora ISRG Root X1, o handshake durou cerca de 23 s simulados. A cadeia de `*.ngrok-free.dev` exige três verificações ECDSA P-384 (folha ← Let's Encrypt YE2 ← ISRG Root YE ← ISRG Root X2) e uma RSA (X2 ← X1). Com a âncora na YE2, o tempo caiu para cerca de 11 s, com aproximadamente 6 s por verificação P-384 no ESP32 simulado, e a conexão ainda falhou. A borda do ngrok já desconectou um cliente TLS de teste no notebook que atrasou a resposta em 3 s. O TLS validado entre o ESP32 simulado e o ngrok não concluiu. O mesmo limite vale para o Wokwi web, que simula a mesma CPU. [Registro do ensaio](../roteiro-wokwi.md#registro-de-falha-e-reteste)

## Decisão

O grupo usa o Wokwi para VS Code (extensão 4.1.0), com o gateway privado embutido rodando no notebook. A demo usa o mesmo sketch e o mesmo `diagram.json` do projeto web. O `wokwi.toml` aponta para `build/sketch.ino.merged.bin`, gerado com `arduino-cli` e `--build-path firmware/sketch/build`. A cada início, o simulador copia `sdcard/` para o microSD. Nessa pasta, `placa-base64.txt` é um link simbólico para o arquivo do sketch. [Configuração do Wokwi](../../firmware/sketch/wokwi.toml) · [README](../../README.md#simular-no-wokwi-para-vs-code)

No commit `ebdb136`, o cliente do servidor passou a aceitar HTTP somente para URLs iniciadas por `http://host.wokwi.internal:`. O gateway encaminha a conexão ao servidor no próprio notebook. Para os demais destinos, o firmware exige HTTPS com a CA configurada e validação ativa. O script aceita `./scripts/gerar-secrets.sh http://host.wokwi.internal:8080` sem CA. No reteste, a sincronização retornou HTTP 200 cerca de 10 s após o boot simulado, com o registro `connected to host.wokwi.internal:8080`. [Cliente do servidor](../../firmware/sketch/cliente_servidor.cpp) · [Script de configuração](../../scripts/gerar-secrets.sh)

## Considered Options

- **Wokwi Hobby no web:** o grupo descartou a assinatura paga para usar o gateway privado.
- **Ancorar na intermediária YE2:** reduziu o tempo do handshake, mas a borda do ngrok ainda encerrou a conexão.
- **Desativar a validação com `setInsecure()`:** descartado porque elimina a autenticação do destino HTTPS.

## Consequências

- A demo dispensa o ngrok. Simulador, gateway e servidor em localhost ficam no mesmo notebook. O grupo aceita a ausência de TLS nesse trecho local. A API continua exigindo `X-Api-Key`, e a imagem segue cifrada até a API com AES-GCM e RSA-OAEP SHA-256. A correção do checklist OWASP continua aplicada. [ADR 0007](0007-imagem-cifrada-para-a-api.md)
- Para um ESP32 real em rede de laboratório, use HTTPS com a CA configurada, de preferência com uma cadeia curta de certificados, pelo custo das verificações ECDSA P-384.
- A extensão usa uma licença pessoal gratuita de 30 dias. O link do Wokwi web continua como item de entrega. [Entrega](../entrega.md)
