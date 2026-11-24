#pragma once

// Copie para secrets.h e preencha localmente; nunca versione secrets.h.
// URL base HTTPS, sem / final. Exemplo de formato: https://seu-dominio.ngrok-free.app
static constexpr char SERVIDOR_URL[] = "";
// PEM completo da CA raiz do domínio (veja docs/credenciais.md).
static constexpr char SERVIDOR_CA_RAIZ[] = "";
// Mesma chave exclusiva da demo configurada no servidor; vazia bloqueia o cliente.
static constexpr char ESTACAO_API_KEY[] = "";
// Chave pública RSA 2048 em PEM (BEGIN PUBLIC KEY); vazia bloqueia somente a captura.
static constexpr char CAPTURA_CHAVE_PUBLICA[] = "";
