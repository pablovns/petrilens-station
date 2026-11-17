> Cópia da proposta (Atividade 1) enviada e aprovada pelo professor. Não editar: é o compromisso de escopo do trabalho. Mudanças de rumo vão para `docs/adr/`.

# Estação IoT de captura de placas de Petri

Atividade 1: proposta de projeto. Outubro de 2026.

## 1. Identificação

Integrantes: Pablo Vinicius de Campos Souza, Douglas Bruno Campos de Souza e Entony Gabriel De Lima Wittmann.

## 2. Aplicação

A análise de colônias de *Salmonella* spp. no projeto PetriLens depende de fotos padronizadas das placas de Petri. Hoje, a captura manual com celular varia em distância, ângulo e iluminação, o que prejudica a contagem automática. A estação resolve isso em laboratórios de microbiologia de alimentos.

No dia a dia, técnicos de laboratório, analistas de alimentos e estudantes ou pesquisadores usam o sistema para contar colônias.

A estação mantém a placa em uma posição fixa e a iluminação controlada. Quando o usuário coloca a placa no berço, um sensor de presença detecta a placa e acende a luz branca difusa. Um sensor de luz confirma se a iluminação está na faixa ideal. O ESP32 captura a imagem (no Wokwi, um JPEG de exemplo) e a envia por Wi-Fi ao sistema PetriLens, junto com os dados da captura. O sistema registra a análise e o usuário vê o resultado no aplicativo ou painel. Um botão físico e comandos pela rede também podem iniciar a captura. LEDs de status mostram se a estação está pronta, capturando, enviando ou com erro, sem depender do Monitor Serial.

## 3. Componentes

Placa: ESP32 DevKit V1. Sensores: LDR (`wokwi-photoresistor-sensor`), slide switch de presença (`wokwi-slide-switch`, no lugar da chave reed que detecta a placa no berço) e pushbutton (`wokwi-pushbutton`) para acionamento manual. A câmera OV2640 (2 MP) não é simulada no Wokwi; a imagem é um JPEG de exemplo guardado no cartão microSD (`wokwi-microsd-card`). Atuadores: LED bar graph (`wokwi-led-bar-graph`), no lugar do anel de LED branco da captura, e LED RGB (`wokwi-rgb-led`) para o status. Custo total: R$ 0, pois o projeto roda todo no Wokwi.

## 4. Comunicação e dados

A estação usa Wi-Fi 2,4 GHz e HTTP. Fica fixa, perto do roteador, e envia imagens; Wi-Fi dá conta da taxa e do alcance. Bluetooth fica curto demais para imagens, e LoRa tem taxa muito baixa.

Envia a imagem JPEG e um JSON com os dados da captura, por exemplo `{"estacao_id":"est-01","lote_id":"L2026-001","amostra_id":"A1","lux":542,"resolucao":"800x600","simulacao":true}`. Recebe os comandos `capturar`, `status` e `ajustar_brilho`. O usuário vê o resultado no aplicativo ou painel do PetriLens, e o LED de status mostra o estado na própria estação.

## 5. Montagem

O projeto será desenvolvido apenas no Wokwi. A base atual tem o scanner de Wi-Fi; os outros componentes entram na simulação. A câmera é a única parte que o Wokwi não simula. Para representar a captura, o firmware lê um JPEG de exemplo (uma imagem fictícia de placa) do cartão SD, e o resto do fluxo (sensores, iluminação, Wi-Fi, HTTP, JSON e status) roda de verdade.

O projeto está em https://wokwi.com/projects/305569599398609473, ainda como um circuito simples criado a partir do template "Wifi Scanner".

## Arquitetura em 4 camadas

Percepção: LDR, slide switch (presença), pushbutton e câmera (JPEG de exemplo). Rede: Wi-Fi 2,4 GHz e HTTP. Processamento: ESP32, com a regra local (placa presente e lux na faixa ideal), controle da luz, leitura da imagem, envio com retry, JSON e status. Aplicação: aplicativo ou painel do PetriLens, com o LED de status na estação.
