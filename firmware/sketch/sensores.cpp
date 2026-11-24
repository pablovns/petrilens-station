#include "sensores.h"

#include <Arduino.h>
#include <math.h>

namespace {
constexpr int PINO_LUZ = 34;
constexpr int PINO_PRESENCA = 35;
constexpr int PINO_BOTAO = 36;
constexpr int RESOLUCAO_ADC_BITS = 12;
constexpr int ADC_MAXIMO = 4095;
constexpr float TENSAO_ALIMENTACAO = 3.3f;
constexpr float RESISTOR_DIVISOR_OHMS = 10000.0f;
constexpr float RESISTENCIA_LDR_A_10_LX_KOHMS = 50.0f;
constexpr float OHMS_POR_KILOOHM = 1000.0f;
constexpr float LUX_DE_REFERENCIA = 10.0f;
constexpr float GAMA_LDR = 0.7f;
constexpr unsigned long DEBOUNCE_MS = 30;

bool ultimaLeituraBrutaBotao = false;
bool botaoEstavel = false;
unsigned long instanteMudancaBotao = 0;
}  // namespace

void iniciarSensores() {
  pinMode(PINO_LUZ, INPUT);
  pinMode(PINO_PRESENCA, INPUT);
  pinMode(PINO_BOTAO, INPUT);
  analogReadResolution(RESOLUCAO_ADC_BITS);
  analogSetPinAttenuation(PINO_LUZ, ADC_11db);
}

LeituraSensores lerSensores() {
  bool botaoLido = digitalRead(PINO_BOTAO) == LOW;
  unsigned long agora = millis();
  if (botaoLido != ultimaLeituraBrutaBotao) {
    ultimaLeituraBrutaBotao = botaoLido;
    instanteMudancaBotao = agora;
  }
  if (agora - instanteMudancaBotao >= DEBOUNCE_MS) {
    botaoEstavel = botaoLido;
  }

  // Evita os extremos do ADC, que zerariam uma das divisões.
  int leituraAdc = constrain(analogRead(PINO_LUZ), 1, ADC_MAXIMO - 1);
  // A tensão do divisor dá a resistência do LDR. A curva nominal do Wokwi converte ohms em lux.
  float tensaoLdr = leituraAdc * TENSAO_ALIMENTACAO / ADC_MAXIMO;
  float resistenciaLdr = RESISTOR_DIVISOR_OHMS * tensaoLdr / (TENSAO_ALIMENTACAO - tensaoLdr);
  float lux = powf(RESISTENCIA_LDR_A_10_LX_KOHMS * OHMS_POR_KILOOHM
      * powf(LUX_DE_REFERENCIA, GAMA_LDR) / resistenciaLdr, 1.0f / GAMA_LDR);
  return {digitalRead(PINO_PRESENCA) == LOW, lux, botaoEstavel};
}
