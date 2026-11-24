#include "captura.h"

#include "iluminacao.h"
#include "imagem.h"
#include "rede.h"
#include "sensores.h"
#include "status.h"

#include <limits>

namespace {
constexpr float LUX_MINIMO = 300.0f;
constexpr float LUX_MAXIMO = 700.0f;

constexpr bool luxNaFaixa(float lux) {
  return lux >= LUX_MINIMO && lux <= LUX_MAXIMO;
}

static_assert(!luxNaFaixa(299.0f) && luxNaFaixa(300.0f));
static_assert(luxNaFaixa(700.0f) && !luxNaFaixa(701.0f));
static_assert(!luxNaFaixa(std::numeric_limits<float>::quiet_NaN()));
static_assert(!luxNaFaixa(std::numeric_limits<float>::infinity()));
static_assert(!luxNaFaixa(-std::numeric_limits<float>::infinity()));

bool recusadaPorLux = false;
}  // namespace

bool capturaRecusadaPorLux() {
  return recusadaPorLux;
}

bool capturar(bool clienteConfigurado, int brilho, const AmostraSelecionada& amostra) {
  LeituraSensores leitura = lerSensores();
  if (!leitura.presenca || !clienteConfigurado || !redeConectada() || amostra.amostraId.isEmpty()) {
    sinalizarErro();
    return false;
  }
  atualizarIluminacao(leitura.presenca, brilho);
  const float lux = leitura.lux;
  recusadaPorLux = !luxNaFaixa(lux);
  if (recusadaPorLux) {
    sinalizarErro();
    return false;
  }
  atualizarStatus(redeConectada(), leitura.presenca, true);
  fs::File imagem = abrirImagem();
  if (!imagem) {
    sinalizarErro();
    return false;
  }
  // A placa pode ter sido retirada enquanto o microSD abria a imagem.
  leitura = lerSensores();
  atualizarIluminacao(leitura.presenca, brilho);
  if (!leitura.presenca) {
    imagem.close();
    sinalizarErro();
    return false;
  }
  const bool enviada = enviarCaptura(amostra, lux, imagem);
  imagem.close();
  if (!enviada) sinalizarErro();
  return enviada;
}
