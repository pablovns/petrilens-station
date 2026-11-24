#include "status.h"

#include <Arduino.h>

namespace {
constexpr int PINO_VERMELHO = 25;
constexpr int PINO_VERDE = 26;
constexpr int PINO_AZUL = 32;
constexpr unsigned long DURACAO_ERRO_MS = 3000;
bool erroAtivo = false;
unsigned long inicioErro = 0;
}  // namespace

void iniciarStatus() {
  pinMode(PINO_VERMELHO, OUTPUT);
  pinMode(PINO_VERDE, OUTPUT);
  pinMode(PINO_AZUL, OUTPUT);
  atualizarStatus(false, false);
}

void sinalizarErro() {
  erroAtivo = true;
  inicioErro = millis();
  digitalWrite(PINO_VERMELHO, HIGH);
  digitalWrite(PINO_VERDE, LOW);
  digitalWrite(PINO_AZUL, LOW);
}

void atualizarStatus(bool conectada, bool presenca, bool capturando) {
  if (erroAtivo && millis() - inicioErro >= DURACAO_ERRO_MS) erroAtivo = false;
  // Vermelho sem Wi-Fi ou por erro, azul ao capturar/enviar, verde com placa e amarelo sem placa.
  bool mostrarVermelhoDeErro = !conectada || (erroAtivo && !capturando);
  digitalWrite(PINO_VERMELHO, mostrarVermelhoDeErro || (!capturando && !presenca) ? HIGH : LOW);
  digitalWrite(PINO_VERDE, !mostrarVermelhoDeErro && !capturando ? HIGH : LOW);
  digitalWrite(PINO_AZUL, !mostrarVermelhoDeErro && capturando ? HIGH : LOW);
}
