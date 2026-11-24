#pragma once

struct LeituraSensores {
  bool presenca;
  float lux;
  bool botaoPressionado;
};

void iniciarSensores();
LeituraSensores lerSensores();
