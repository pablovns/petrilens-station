# A estação inicia toda a comunicação, por sincronização periódica

No plano gratuito do Wokwi, o Wi-Fi simulado sai pelo gateway público, que só permite conexões de saída: o servidor não consegue chamar a estação (ver `docs/pesquisa/wokwi.md`). Por isso a estação inicia toda a comunicação. A cada 5 s ela faz uma **sincronização**: envia seu estado e recebe na resposta os comandos pendentes e a amostra selecionada. As capturas são enviadas numa requisição própria. O painel nunca fala com a estação: ele lê e escreve no servidor, que guarda os comandos até a próxima sincronização.

## Consequências

- Um comando dado no painel leva até cerca de 5 s para chegar à estação, mais o intervalo de atualização do painel.
- O intervalo de 5 s também respeita a cota de requisições do plano gratuito do ngrok (ADR 0003).
- Para que a captura pelo botão nunca saia com uma amostra desatualizada, a estação sincroniza imediatamente antes de capturar. O comando `capturar` do painel já chega numa sincronização, junto com a amostra selecionada atual. Reduzir o intervalo só encolheria a janela de risco e gastaria a cota mais rápido.

## Fora do Wokwi

A abordagem é consequência da simulação, não uma exigência do sistema. Com a estação física na rede do laboratório, ou com outro mecanismo de mensagens (por exemplo MQTT, em que a estação assina um tópico de comandos), o servidor poderia entregar comandos na hora, sem esperar a próxima sincronização.
