#include "cliente_servidor.h"

#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <NetworkClient.h>
#include <NetworkClientSecure.h>
#include <mbedtls/base64.h>

#include "imagem.h"

#if __has_include("secrets.h")
#include "secrets.h"
#else
#include "secrets.example.h"
#endif

namespace {
constexpr int TEMPO_LIMITE_HTTP_MS = 4000;
constexpr unsigned long TEMPO_LIMITE_HANDSHAKE_S = 4;
constexpr int MAX_TENTATIVAS_CAPTURA = 3;
constexpr size_t LIMITE_CORPO_CAPTURA_BYTES = 65536;
constexpr int BRILHO_MINIMO = 0;
constexpr int BRILHO_MAXIMO = 10;

// A ADR 0008 permite HTTP só no gateway privado, que encaminha ao próprio notebook.
bool urlDoGatewayLocalWokwi() {
  return String(SERVIDOR_URL).startsWith("http://host.wokwi.internal:");
}
}  // namespace

bool configurarClienteServidor() {
  if (strlen(ESTACAO_API_KEY) == 0) {
    Serial.println("Configuração pendente: preencha ESTACAO_API_KEY em secrets.h.");
    return false;
  }
  if (urlDoGatewayLocalWokwi()) return true;
  if (strlen(SERVIDOR_CA_RAIZ) == 0) {
    Serial.println("Configuração pendente: preencha SERVIDOR_CA_RAIZ em secrets.h; HTTPS bloqueado sem CA.");
    return false;
  }
  if (!String(SERVIDOR_URL).startsWith("https://")) {
    Serial.println("Configuração pendente: SERVIDOR_URL deve ser HTTPS ou o gateway local do Wokwi.");
    return false;
  }
  return true;
}

namespace {
bool iniciarConexaoHttp(HTTPClient& http, NetworkClient& clienteSemTls,
                        NetworkClientSecure& clienteTls, const char* rota) {
  http.setConnectTimeout(TEMPO_LIMITE_HTTP_MS);
  http.setTimeout(TEMPO_LIMITE_HTTP_MS);
  String url = String(SERVIDOR_URL) + rota;
  if (urlDoGatewayLocalWokwi()) return http.begin(clienteSemTls, url);
  // HTTPS valida o certificado pela CA configurada para autenticar o servidor.
  clienteTls.setCACert(SERVIDOR_CA_RAIZ);
  clienteTls.setHandshakeTimeout(TEMPO_LIMITE_HANDSHAKE_S);
  return http.begin(clienteTls, url);
}

void interpretarComando(JsonObject recebido, ComandoRecebido& comando) {
  const char* tipo = recebido["tipo"] | "";
  JsonVariant valor = recebido["valor"];
  if (strcmp(tipo, "capturar") == 0 && valor.isNull()) {
    comando.tipo = TipoComando::Capturar;
  } else if (strcmp(tipo, "status") == 0 && valor.isNull()) {
    comando.tipo = TipoComando::Status;
  } else if (strcmp(tipo, "ajustar_brilho") == 0 && valor.is<int>()) {
    int brilho = valor.as<int>();
    if (brilho >= BRILHO_MINIMO && brilho <= BRILHO_MAXIMO) {
      comando = {TipoComando::AjustarBrilho, brilho};
    }
  }
}
}  // namespace

bool sincronizar(const EstadoEstacao& estado, AmostraSelecionada& amostraSelecionada,
                 ComandoRecebido& comando) {
  comando = {};
  NetworkClient clienteSemTls;
  NetworkClientSecure clienteTls;
  HTTPClient http;
  if (!iniciarConexaoHttp(http, clienteSemTls, clienteTls, "/api/estacao/sincronizacao")) {
    Serial.println("Não foi possível iniciar a conexão com o servidor.");
    return false;
  }
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Api-Key", ESTACAO_API_KEY);
  http.addHeader("ngrok-skip-browser-warning", "true");

  JsonDocument corpo;
  corpo["estacao_id"] = "est-01";
  corpo["presenca"] = estado.presenca;
  corpo["lux"] = estado.lux;
  corpo["brilho"] = estado.brilho;
  corpo["amostra_id"] = estado.amostraId;
  corpo["rssi"] = estado.rssi;
  corpo["uptime_s"] = estado.uptimeS;
  if (estado.capturaRecusadaPorLux) corpo["motivo_recusa"] = "lux_fora_da_faixa";
  String json;
  serializeJson(corpo, json);
  int codigoHttp = http.POST(json);
  bool sucesso = false;
  if (codigoHttp == HTTP_CODE_OK) {
    JsonDocument resposta;
    DeserializationError erro = deserializeJson(resposta, http.getString());
    JsonObject amostra = resposta["amostra_selecionada"].as<JsonObject>();
    JsonArray comandos = resposta["comandos"].as<JsonArray>();
    sucesso = !erro && !comandos.isNull() && comandos.size() <= 1
        && amostra["lote_id"].is<const char*>() && amostra["amostra_id"].is<const char*>()
        && strlen(amostra["lote_id"].as<const char*>()) > 0
        && strlen(amostra["amostra_id"].as<const char*>()) > 0;
    if (sucesso) {
      amostraSelecionada.loteId = amostra["lote_id"].as<String>();
      amostraSelecionada.amostraId = amostra["amostra_id"].as<String>();
      if (comandos.size() == 1) {
        interpretarComando(comandos[0].as<JsonObject>(), comando);
      }
    }
    if (!sucesso) {
      Serial.println("Resposta de sincronização inválida.");
    }
  } else {
    Serial.printf("Sincronização falhou (HTTP/cliente: %d); confira rede, URL e CA.\n", codigoHttp);
  }
  http.end();
  return sucesso;
}

namespace {
// O corpo termina com as aspas do campo da imagem e a chave de fechamento do JSON.
constexpr size_t TAMANHO_SUFIXO_JSON = 2;

constexpr size_t tamanhoBase64(size_t bytes) {
  return ((bytes + 2) / 3) * 4;
}

// Envia prefixo, Base64 em blocos e sufixo, evitando duas cópias da imagem na RAM.
// O tamanho total é calculado antes do envio para gerar o Content-Length exato.
class CorpoCaptura : public Stream {
 public:
  CorpoCaptura(const String& prefixo, const ImagemCifrada& imagem)
      : prefixo_(prefixo), imagem_(imagem),
        tamanho_(prefixo.length() + tamanhoBase64(imagem.tamanho) + TAMANHO_SUFIXO_JSON) {}

  int available() override { return falhou_ ? -1 : tamanho_ - posicao_; }
  int read() override {
    int valor = peek();
    if (valor >= 0) posicao_++;
    return valor;
  }
  int peek() override {
    if (falhou_ || posicao_ >= tamanho_) return -1;
    if (posicao_ < prefixo_.length()) return prefixo_[posicao_];
    size_t inicioSufixo = tamanho_ - TAMANHO_SUFIXO_JSON;
    if (posicao_ >= inicioSufixo) return posicao_ == inicioSufixo ? '"' : '}';

    size_t posicaoBase64 = posicao_ - prefixo_.length();
    size_t bloco = posicaoBase64 / BASE64_POR_BLOCO;
    if (bloco != blocoAtual_) {
      size_t inicio = bloco * BYTES_POR_BLOCO;
      size_t bytes = min(BYTES_POR_BLOCO, imagem_.tamanho - inicio);
      size_t escritos = 0;
      if (mbedtls_base64_encode(bloco_, sizeof(bloco_), &escritos,
              imagem_.bytes.get() + inicio, bytes) != 0 || escritos != tamanhoBase64(bytes)) {
        falhou_ = true;
        return -1;
      }
      blocoAtual_ = bloco;
    }
    return bloco_[posicaoBase64 % BASE64_POR_BLOCO];
  }
  size_t write(uint8_t) override { return 0; }
  size_t tamanho() const { return tamanho_; }

 private:
  static constexpr size_t BYTES_POR_BLOCO = 192;
  static constexpr size_t BASE64_POR_BLOCO = tamanhoBase64(BYTES_POR_BLOCO);
  const String& prefixo_;
  const ImagemCifrada& imagem_;
  const size_t tamanho_;
  size_t posicao_ = 0;
  uint8_t bloco_[BASE64_POR_BLOCO + 1];
  size_t blocoAtual_ = SIZE_MAX;
  bool falhou_ = false;
};

bool tentarEnviarCaptura(CorpoCaptura& corpo) {
  NetworkClient clienteSemTls;
  NetworkClientSecure clienteTls;
  HTTPClient http;
  if (!iniciarConexaoHttp(http, clienteSemTls, clienteTls, "/api/estacao/capturas")) return false;
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Api-Key", ESTACAO_API_KEY);
  http.addHeader("ngrok-skip-browser-warning", "true");
  int codigoHttp = http.sendRequest("POST", &corpo, corpo.tamanho());
  http.end();
  return codigoHttp == HTTP_CODE_CREATED;
}
}  // namespace

bool enviarCaptura(const AmostraSelecionada& amostra, float lux, fs::File& imagem) {
  if (CAPTURA_CHAVE_PUBLICA[0] == '\0') {
    Serial.println("Configuração pendente: preencha CAPTURA_CHAVE_PUBLICA em secrets.h.");
    return false;
  }
  ImagemCifrada cifrada;
  if (!cifrarImagem(imagem, CAPTURA_CHAVE_PUBLICA, cifrada)) {
    Serial.println("Não foi possível preparar a imagem cifrada.");
    return false;
  }

  uint8_t chaveBase64[tamanhoBase64(sizeof(cifrada.chave)) + 1];
  uint8_t ivBase64[tamanhoBase64(sizeof(cifrada.iv)) + 1];
  size_t escritos = 0;
  if (mbedtls_base64_encode(chaveBase64, sizeof(chaveBase64), &escritos,
          cifrada.chave, sizeof(cifrada.chave)) != 0
      || mbedtls_base64_encode(ivBase64, sizeof(ivBase64), &escritos,
          cifrada.iv, sizeof(cifrada.iv)) != 0) return false;

  JsonDocument metadados;
  metadados["estacao_id"] = "est-01";
  metadados["lote_id"] = amostra.loteId;
  metadados["amostra_id"] = amostra.amostraId;
  metadados["lux"] = lux;
  metadados["resolucao"] = "800x600";
  metadados["simulacao"] = true;
  metadados["chave_cifrada"] = reinterpret_cast<const char*>(chaveBase64);
  metadados["iv"] = reinterpret_cast<const char*>(ivBase64);
  metadados["imagem_cifrada"] = "";
  String prefixo;
  if (metadados.overflowed() || serializeJson(metadados, prefixo) != measureJson(metadados)) return false;
  prefixo.remove(prefixo.length() - TAMANHO_SUFIXO_JSON);  // Fecha imagem_cifrada somente depois do Base64.

  for (int tentativa = 0; tentativa < MAX_TENTATIVAS_CAPTURA; tentativa++) {
    CorpoCaptura corpo(prefixo, cifrada);
    if (corpo.tamanho() > LIMITE_CORPO_CAPTURA_BYTES) return false;
    if (tentarEnviarCaptura(corpo)) return true;
  }
  return false;
}
