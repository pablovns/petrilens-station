#pragma once

#include <Arduino.h>
#include <FS.h>

struct EstadoEstacao {
  bool presenca;
  float lux;
  int brilho;
  const char* amostraId;
  int rssi;
  unsigned long uptimeS;
  bool capturaRecusadaPorLux;
};

struct AmostraSelecionada {
  String loteId;
  String amostraId;
};

enum class TipoComando { Nenhum, Capturar, Status, AjustarBrilho };

struct ComandoRecebido {
  TipoComando tipo = TipoComando::Nenhum;
  int brilho = 0;
};

bool configurarClienteServidor();
bool sincronizar(const EstadoEstacao& estado, AmostraSelecionada& amostraSelecionada,
                 ComandoRecebido& comando);

bool enviarCaptura(const AmostraSelecionada& amostra, float lux, fs::File& imagem);
