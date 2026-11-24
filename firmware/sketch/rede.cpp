#include "rede.h"

#include <WiFi.h>

void iniciarRede() {
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin("Wokwi-GUEST", "");
}

bool redeConectada() {
  return WiFi.status() == WL_CONNECTED;
}

int intensidadeRede() {
  return WiFi.RSSI();
}
