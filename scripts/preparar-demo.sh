#!/usr/bin/env bash
# Gera o par RSA da demo (README) e o .env da raiz. Não imprime nenhum valor secreto.
set -euo pipefail
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
chaves="$HOME/.config/petrilens"
umask 077
set -o noclobber
mkdir -p "$chaves"
chmod 700 "$chaves"
if [[ ! -e "$chaves/captura-privada.pem" ]]; then
  if [[ -e "$chaves/captura-publica.pem" ]]; then
    echo 'Existe chave pública sem a privada correspondente. Restaure ou mova o par antes de continuar.' >&2
    exit 1
  fi
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$chaves/captura-privada.pem" 2>/dev/null
  openssl pkey -in "$chaves/captura-privada.pem" -pubout -out "$chaves/captura-publica.pem"
  echo 'Par RSA gerado.'
else
  echo 'Par RSA existente mantido.'
fi
if [[ -e "$raiz/.env" || -L "$raiz/.env" ]]; then
  echo '.env já existe: não sobrescrevi'
  exit 0
fi
read -rp 'Usuário do painel (sem ":", ex.: operador): ' usuario
read -rsp 'Senha do painel (alfanumérica): ' senha; printf '\n'
[[ $usuario =~ ^[A-Za-z0-9._-]+$ && $senha =~ ^[A-Za-z0-9]+$ ]] || { echo "usuário ou senha com caracteres não aceitos"; exit 1; }
chave_estacao=$(openssl rand -hex 24)
printf 'ESTACAO_API_KEY=%s\nPAINEL_USUARIO=%s\nPAINEL_SENHA=%s\nCAPTURA_CHAVE_PRIVADA=%s\n' \
  "$chave_estacao" "$usuario" "$senha" "$chaves/captura-privada.pem" > "$raiz/.env"
unset senha chave_estacao
echo ".env criado (chave da estação aleatória=[CONFIGURED], painel=[CONFIGURED], chave privada=[CONFIGURED])"
