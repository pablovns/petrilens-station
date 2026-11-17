# Captura só por botão ou painel; a presença da placa acende a luz

A proposta descreve a captura disparada pela colocação da placa no berço. Para simplificar, a captura só é disparada pelo botão físico ou pelo comando `capturar` do painel, e exige placa presente. A colocação da placa continua acionando a luz automaticamente (placa presente acende o LED bar, placa ausente apaga), o que atende ao R1: o atuador reage a uma leitura de sensor, e não somente a um comando manual.

## Sensor de luz

No Wokwi, o sensor de luz (LDR) é um controle deslizante movido à mão na simulação; ele não mede a luz do LED da estação. Por isso não há malha fechada (o LED se ajustando até a luz entrar na faixa): ela nunca convergiria na simulação. O nível do LED bar é definido pelo comando `ajustar_brilho`. Validar a luminosidade medida pelo LDR antes de capturar é desejável, mas não obrigatório, e fica como trabalho de menor prioridade.

Com hardware físico, o LDR dentro da câmara escura mediria a luz do próprio LED, e a malha fechada passaria a fazer sentido.
