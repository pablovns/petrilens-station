#pragma once

#include <FS.h>
#include <memory>

fs::File abrirImagem();

struct ImagemCifrada {
  uint8_t chave[256];
  uint8_t iv[12];
  std::unique_ptr<uint8_t[]> bytes;
  size_t tamanho = 0;
};

bool cifrarImagem(fs::File& imagem, const char* chavePublica, ImagemCifrada& imagemCifrada);
