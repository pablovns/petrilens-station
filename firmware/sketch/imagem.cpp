#include "imagem.h"

#include <SD.h>
#include <SPI.h>
#include <algorithm>
#include <esp_random.h>
#include <mbedtls/base64.h>
#include <mbedtls/gcm.h>
#include <mbedtls/pk.h>
#include <mbedtls/platform_util.h>
#include <mbedtls/rsa.h>
#include <new>

#include "rede.h"

namespace {
constexpr int PINO_SPI_CLOCK = 18;
constexpr int PINO_SPI_MISO = 19;
constexpr int PINO_SPI_MOSI = 23;
constexpr int PINO_SPI_SELECAO_SD = 33;
constexpr size_t TAMANHO_MINIMO_BASE64_SD = 8;
constexpr size_t LIMITE_BASE64_SD = 40000;
constexpr size_t TAMANHO_TAG = 16;
constexpr size_t TAMANHO_CHAVE_AES_BYTES = 16;
constexpr size_t TAMANHO_CHAVE_RSA_BITS = 2048;
constexpr size_t TAMANHO_BLOCO_BASE64 = 256;
constexpr size_t TAMANHO_BLOCO_JPEG = 192;
constexpr size_t TAMANHO_MINIMO_JPEG = 4;

int gerarBytesAleatorios(void*, unsigned char* dados, size_t tamanho) {
  esp_fill_random(dados, tamanho);
  return 0;
}

bool cifrarChaveAesComRsa(const char* pem, const uint8_t* chaveAes, size_t tamanhoChave,
                          ImagemCifrada& imagemCifrada) {
  mbedtls_pk_context contextoChavePublica;
  mbedtls_pk_init(&contextoChavePublica);
  bool sucesso = false;
  if (mbedtls_pk_parse_public_key(&contextoChavePublica, reinterpret_cast<const uint8_t*>(pem),
          strlen(pem) + 1) == 0
      && mbedtls_pk_get_type(&contextoChavePublica) == MBEDTLS_PK_RSA
      && mbedtls_pk_get_bitlen(&contextoChavePublica) == TAMANHO_CHAVE_RSA_BITS) {
    auto* rsa = mbedtls_pk_rsa(contextoChavePublica);
    // Só o servidor pode abrir a chave AES cifrada com RSA-OAEP. O SHA-256 vale também para MGF1.
    sucesso = mbedtls_rsa_set_padding(rsa, MBEDTLS_RSA_PKCS_V21, MBEDTLS_MD_SHA256) == 0
        && mbedtls_rsa_rsaes_oaep_encrypt(rsa, gerarBytesAleatorios, nullptr, nullptr, 0,
               tamanhoChave, chaveAes, imagemCifrada.chave) == 0;
  }
  mbedtls_pk_free(&contextoChavePublica);
  return sucesso;
}

bool finalizarImagemCifrada(mbedtls_gcm_context& gcm, ImagemCifrada& imagemCifrada,
                            size_t limiteJpegBytes, size_t bytesEscritos) {
  // O servidor recebe a tag de autenticação logo após os bytes cifrados.
  uint8_t tag[TAMANHO_TAG];
  size_t bytesFinais = 0;
  if (mbedtls_gcm_finish(&gcm, imagemCifrada.bytes.get() + bytesEscritos,
          limiteJpegBytes + TAMANHO_TAG - bytesEscritos, &bytesFinais, tag, sizeof(tag)) != 0
      || bytesEscritos + bytesFinais < TAMANHO_MINIMO_JPEG
      || bytesEscritos + bytesFinais > limiteJpegBytes) {
    return false;
  }
  memcpy(imagemCifrada.bytes.get() + bytesEscritos + bytesFinais, tag, sizeof(tag));
  imagemCifrada.tamanho = bytesEscritos + bytesFinais + sizeof(tag);
  return true;
}

bool decodificarECifrarJpeg(mbedtls_gcm_context& gcm, fs::File& imagem,
                            ImagemCifrada& imagemCifrada, size_t limiteJpegBytes) {
  // Decodifica o Base64 do microSD em blocos para cifrar sem guardar o JPEG inteiro na RAM.
  // Cada bloco completo rende 192 bytes, múltiplo do bloco AES.
  uint8_t blocoBase64[TAMANHO_BLOCO_BASE64];
  uint8_t blocoJpeg[TAMANHO_BLOCO_JPEG];
  size_t bytesEscritos = 0;
  size_t caracteresRestantes = imagem.size();
  bool sucesso = true;
  while (caracteresRestantes > 0) {
    size_t caracteresDoBloco = min(caracteresRestantes, sizeof(blocoBase64));
    size_t decodificados = 0;
    size_t cifrados = 0;
    if (imagem.read(blocoBase64, caracteresDoBloco) != caracteresDoBloco
        || std::any_of(blocoBase64, blocoBase64 + caracteresDoBloco, [](uint8_t c) { return c <= ' '; })
        || mbedtls_base64_decode(blocoJpeg, sizeof(blocoJpeg), &decodificados,
               blocoBase64, caracteresDoBloco) != 0
        || (caracteresRestantes > caracteresDoBloco && decodificados != sizeof(blocoJpeg))
        || mbedtls_gcm_update(&gcm, blocoJpeg, decodificados, imagemCifrada.bytes.get() + bytesEscritos,
               limiteJpegBytes + TAMANHO_TAG - bytesEscritos, &cifrados) != 0) {
      sucesso = false;
      break;
    }
    bytesEscritos += cifrados;
    caracteresRestantes -= caracteresDoBloco;
  }
  mbedtls_platform_zeroize(blocoJpeg, sizeof(blocoJpeg));
  mbedtls_platform_zeroize(blocoBase64, sizeof(blocoBase64));
  if (!sucesso) return false;

  return finalizarImagemCifrada(gcm, imagemCifrada, limiteJpegBytes, bytesEscritos);
}
}  // namespace

fs::File abrirImagem() {
  static bool cartaoIniciado = false;
  if (!cartaoIniciado) {
    SPI.begin(PINO_SPI_CLOCK, PINO_SPI_MISO, PINO_SPI_MOSI, PINO_SPI_SELECAO_SD);
    cartaoIniciado = SD.begin(PINO_SPI_SELECAO_SD);
  }
  if (!cartaoIniciado) return {};
  fs::File imagem = SD.open("/placa-base64.txt", FILE_READ);
  if (imagem && (imagem.isDirectory() || imagem.size() < TAMANHO_MINIMO_BASE64_SD
                    || imagem.size() > LIMITE_BASE64_SD || imagem.size() % 4 != 0)) {
    imagem.close();
  }
  return imagem;
}

bool cifrarImagem(fs::File& imagem, const char* chavePublica, ImagemCifrada& imagemCifrada) {
  size_t tamanho = imagem.size();
  if (tamanho < TAMANHO_MINIMO_BASE64_SD || tamanho > LIMITE_BASE64_SD || tamanho % 4 != 0
      || !imagem.seek(0) || !redeConectada()) return false;

  // Cada captura gera uma chave AES-GCM e um IV aleatórios.
  // O Wi-Fi ativo fornece entropia ao gerador do ESP32.
  uint8_t chave[TAMANHO_CHAVE_AES_BYTES];
  esp_fill_random(chave, sizeof(chave));
  esp_fill_random(imagemCifrada.iv, sizeof(imagemCifrada.iv));
  mbedtls_gcm_context gcm;
  mbedtls_gcm_init(&gcm);
  bool pronta = cifrarChaveAesComRsa(chavePublica, chave, sizeof(chave), imagemCifrada)
      && mbedtls_gcm_setkey(&gcm, MBEDTLS_CIPHER_ID_AES, chave, sizeof(chave) * 8) == 0;
  mbedtls_platform_zeroize(chave, sizeof(chave));

  size_t limiteJpegBytes = tamanho / 4 * 3;
  if (pronta) imagemCifrada.bytes.reset(new (std::nothrow) uint8_t[limiteJpegBytes + TAMANHO_TAG]);
  bool sucesso = pronta && imagemCifrada.bytes
      && mbedtls_gcm_starts(&gcm, MBEDTLS_GCM_ENCRYPT, imagemCifrada.iv, sizeof(imagemCifrada.iv)) == 0
      && decodificarECifrarJpeg(gcm, imagem, imagemCifrada, limiteJpegBytes);
  mbedtls_gcm_free(&gcm);
  return sucesso;
}
