#include "captura.h"
#include "cliente_servidor.h"
#include "iluminacao.h"
#include "rede.h"
#include "sensores.h"
#include "status.h"

namespace {
constexpr unsigned long INTERVALO_SINCRONIZACAO_MS = 5000;
constexpr unsigned long PAUSA_LOOP_MS = 10;
constexpr unsigned long MS_POR_SEGUNDO = 1000;
constexpr int BRILHO_INICIAL = 10;

bool clienteConfigurado = false;
unsigned long ultimaSincronizacaoTentada = 0;
bool sincronizacaoImediata = true;
int brilho = BRILHO_INICIAL;
AmostraSelecionada amostraSelecionada;
bool botaoEstavaPressionado = false;

void executarComando(const ComandoRecebido& comando) {
  switch (comando.tipo) {
    case TipoComando::Capturar:
      capturar(clienteConfigurado, brilho, amostraSelecionada);
      break;
    case TipoComando::Status:
      // Responde ao status com outra sincronização na próxima volta do loop.
      sincronizacaoImediata = true;
      break;
    case TipoComando::AjustarBrilho:
      brilho = comando.brilho;
      atualizarIluminacao(lerSensores().presenca, brilho);
      break;
    case TipoComando::Nenhum:
      break;
  }
}
}  // namespace

void setup() {
  // Serial.begin ocuparia o GPIO 3 (RX0), usado pela barra de LEDs.
  iniciarSensores();
  iniciarIluminacao();
  iniciarStatus();
  clienteConfigurado = configurarClienteServidor();
  iniciarRede();
}

void loop() {
  LeituraSensores leitura = lerSensores();
  bool wifiConectado = redeConectada();
  atualizarIluminacao(leitura.presenca, brilho);
  atualizarStatus(wifiConectado, leitura.presenca);

  bool novoAperto = leitura.botaoPressionado && !botaoEstavaPressionado;
  botaoEstavaPressionado = leitura.botaoPressionado;
  bool capturaPeloBotao = novoAperto && leitura.presenca && clienteConfigurado && wifiConectado;
  if (novoAperto && !capturaPeloBotao) sinalizarErro();

  unsigned long agora = millis();
  // Sincroniza a cada 5 s e no primeiro contato.
  // Pelo botão, sincroniza antes da captura para atualizar a amostra selecionada.
  if (clienteConfigurado && wifiConectado
      && (capturaPeloBotao || sincronizacaoImediata
          || agora - ultimaSincronizacaoTentada >= INTERVALO_SINCRONIZACAO_MS)) {
    sincronizacaoImediata = false;
    ultimaSincronizacaoTentada = agora;
    EstadoEstacao estado{leitura.presenca, leitura.lux, brilho,
        amostraSelecionada.amostraId.isEmpty() ? nullptr : amostraSelecionada.amostraId.c_str(),
        intensidadeRede(), agora / MS_POR_SEGUNDO, capturaRecusadaPorLux()};
    ComandoRecebido comando;
    if (sincronizar(estado, amostraSelecionada, comando)) {
      executarComando(comando);
      if (capturaPeloBotao) capturar(clienteConfigurado, brilho, amostraSelecionada);
    } else if (capturaPeloBotao) {
      sinalizarErro();
    }

    leitura = lerSensores();
    atualizarIluminacao(leitura.presenca, brilho);
    atualizarStatus(redeConectada(), leitura.presenca);
  }
  delay(PAUSA_LOOP_MS);
}
