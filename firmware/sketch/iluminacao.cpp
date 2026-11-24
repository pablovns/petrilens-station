#include "iluminacao.h"

#include <Arduino.h>

namespace {
// O GPIO 1 (TX0) não acende LED no Wokwi. O primeiro segmento usa o GPIO 5.
constexpr int PINOS_BARRA[] = {5, 3, 4, 13, 14, 16, 17, 21, 22, 27};
constexpr int TOTAL_SEGMENTOS = sizeof(PINOS_BARRA) / sizeof(PINOS_BARRA[0]);
}  // namespace

void iniciarIluminacao() {
  for (int pino : PINOS_BARRA) {
    pinMode(pino, OUTPUT);
    digitalWrite(pino, LOW);
  }
}

void atualizarIluminacao(bool presenca, int brilho) {
  // A presença liga a barra no brilho configurado. Sem placa, todos os segmentos apagam.
  int segmentosAcesos = presenca ? constrain(brilho, 0, TOTAL_SEGMENTOS) : 0;
  for (int segmento = 0; segmento < TOTAL_SEGMENTOS; ++segmento) {
    digitalWrite(PINOS_BARRA[segmento], segmento < segmentosAcesos ? HIGH : LOW);
  }
}
