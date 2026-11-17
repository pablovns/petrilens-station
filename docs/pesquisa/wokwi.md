# Pesquisa: limites do Wokwi para este projeto

Consultado em 30/10/2026 (análise da proposta) e 17/11/2026 (planos e execução local). Planos e preços mudam: conferir nos links antes de pagar qualquer coisa.

## Câmera

- O Wokwi não simula câmera. A lista oficial de placas não tem ESP32-CAM e o pedido de módulo de câmera foi fechado sem implementação: https://docs.wokwi.com/guides/esp32, https://github.com/wokwi/wokwi-features/issues/337.
- A placa `esp32-cam` existe em `wokwi/wokwi-boards` e compila, mas não expõe os pinos do sensor (5, 18, 19, 21, 22, 23, 25, 26, 27, 34, 35, 36, 39). Decisão: ADR 0004.

## Rede

- O Wi-Fi simulado (`Wokwi-GUEST`, sem senha) tem internet completa: HTTP, HTTPS, MQTT e WebSocket funcionam. Sem ICMP. https://docs.wokwi.com/guides/esp32-wifi
- **Gateway público** (gratuito): dá acesso à internet, mas não à rede local nem ao `localhost`. "All the traffic is monitored for security purposes."
- **Gateway privado**: o simulador alcança a máquina local pelo nome `host.wokwi.internal` (ex.: `http://host.wokwi.internal:8080/`). "The Private Gateway is only available for paying users." Funciona no Wokwi web pelo F1, "Enable Private Wokwi IoT Gateway"; não funciona no Safari.

## Executar localmente

| Opção | Roda local? | Plano necessário |
|---|---|---|
| Wokwi for VS Code | Simulador local, com gateway privado embutido; mesmo assim exige internet ("requires an internet connection to run") | Hobby+ (€8,1/mês no anual). Offline só no Pro (€20/mês por usuário) |
| Wokwi CLI | Não: "The simulation runs in the cloud." | Gratuito com 50 min a cada 30 dias; feito para CI |
| Wokwi web + gateway privado | O ESP32 alcança o notebook por `host.wokwi.internal` | Hobby (€5,6/mês no anual) |
| QEMU da Espressif | Sim | Gratuito; a documentação não cobre Wi-Fi e não há componentes visuais. Não serve para a demo |

Fontes: https://wokwi.com/pricing, https://docs.wokwi.com/vscode/getting-started, https://docs.wokwi.com/vscode/offline-mode, https://docs.wokwi.com/vscode/project-config, https://docs.wokwi.com/wokwi-ci/getting-started, https://github.com/espressif/esp-toolchain-docs/blob/main/qemu/esp32/README.md.

Sem gateway privado, a alternativa gratuita é um túnel do notebook para a internet, pelo ngrok (https://ngrok.com/docs/universal-gateway/domains/, https://ngrok.com/docs/pricing-limits/free-plan-limits/):

- O domínio gratuito é fixo: "it's static: it doesn't change when you restart the agent". `ngrok http 8080` usa esse domínio automaticamente (`*.ngrok-free.app`).
- A página de aviso só aparece para navegadores: "This does not impact users serving APIs or accessing ngrok endpoints programmatically". O `HTTPClient` do ESP32 usa o User-Agent `ESP32HTTPClient`; enviar também o cabeçalho `ngrok-skip-browser-warning` por segurança.
- Limites do plano Free: 20.000 requisições HTTP por mês, 1 GB de saída por mês, 4.000 requisições por minuto. Uma requisição a cada 3 s gasta 1.200 por hora, ou seja, a cota acaba em cerca de 17 horas de estação ligada. O painel deve ser aberto por `localhost`, não pelo túnel.
- O gateway público do Wokwi aceita conexões de saída para qualquer host; não há lista documentada de domínios ou portas bloqueados. HTTPS funciona ("Query web services over HTTP, HTTPS, and web sockets"); exemplos da comunidade usam `setInsecure()`, e não há orientação oficial sobre validação de certificado.

## Cartão microSD

https://docs.wokwi.com/parts/wokwi-microsd-card

- "By default, Wokwi copies all your project files into the microSD card." O cartão é FAT16 com até 8 MB no Wokwi web.
- Enviar arquivos binários (como um JPEG) para o cartão é recurso pago: "Paying users can upload custom binary files". No plano gratuito, o cartão só recebe os arquivos de texto do projeto.

## Editor web

- Aceita vários arquivos `.cpp`/`.h` junto do sketch ("New file" no menu das abas; https://github.com/wokwi/wokwi-features/issues/655).
- Bibliotecas vão em `libraries.txt`, uma por linha, com versão opcional (`Nome@1.2.3`); https://docs.wokwi.com/guides/libraries.
- Não há documentação sobre nomear o arquivo principal de outra forma que `sketch.ino`.

## Link da proposta

O projeto `https://wokwi.com/projects/305569599398609473`, citado na proposta, é o exemplo oficial "WiFi Scanning" do Wokwi (autor urish, listado em https://docs.wokwi.com/guides/esp32), e não uma cópia do grupo. A entrega precisa de um projeto salvo na conta do grupo.

## Sensor de luz

`wokwi-photoresistor-sensor` (https://docs.wokwi.com/parts/wokwi-photoresistor-sensor): a saída AO é lida com `analogRead()`; a DO fica alta no escuro (limiar padrão de cerca de 100 lux). O lux inicial é o atributo `lux` (padrão 500). A fórmula documentada assume 5 V e ADC de 10 bits; para o ESP32 (3,3 V, 12 bits) ela precisa ser adaptada e validada na simulação. O resultado pode dar `inf`: conferir com `isfinite()`.

## Peças disponíveis

Confirmadas em `docs.wokwi.com/parts/<id>`: `wokwi-photoresistor-sensor`, `wokwi-slide-switch`, `wokwi-pushbutton`, `wokwi-led-bar-graph`, `wokwi-rgb-led`, `wokwi-microsd-card`, além de `wokwi-dht22`, `wokwi-ds18b20`, `wokwi-lcd1602`, `wokwi-buzzer`, `wokwi-relay-module`, `wokwi-servo`, `wokwi-pir-motion-sensor`, `wokwi-ds1307`. Não existe `wokwi-reed-switch` (substituído pelo slide switch na proposta).
