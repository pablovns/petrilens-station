#!/usr/bin/env bash
# Monta firmware/sketch/secrets.h a partir do .env, da chave pública e da CA raiz. Não imprime valores.
# Uso: gerar-secrets.sh https://SEU-DOMINIO.ngrok-free.dev caminho-da-ca-raiz.pem
set -euo pipefail
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
# Wokwi para VS Code (ADR 0008): gerar-secrets.sh http://host.wokwi.internal:8080 (sem CA).
if [[ $# -lt 1 ]]; then
  echo 'Uso: gerar-secrets.sh https://DOMINIO caminho-da-ca-raiz.pem | http://host.wokwi.internal:8080' >&2
  exit 1
fi
url=${1%/}
ca=${2:-}
pub="$HOME/.config/petrilens/captura-publica.pem"
if [[ $url =~ ^http://host\.wokwi\.internal:[0-9]+$ ]]; then
  local_wokwi=1
elif [[ $url =~ ^https://[A-Za-z0-9.-]+$ ]]; then
  local_wokwi=0
  [[ -n $ca ]] && grep -q -- 'BEGIN CERTIFICATE' "$ca" || { echo "CA raiz ausente em ${ca:-<não informada>}"; exit 1; }
else
  echo "URL deve ser https://dominio ou http://host.wokwi.internal:PORTA, sem caminho"; exit 1
fi
grep -q -- 'BEGIN PUBLIC KEY' "$pub" || { echo "chave pública ausente em $pub"; exit 1; }
if ! linha=$(grep -m1 '^ESTACAO_API_KEY=' "$raiz/.env") ||
   [[ ! ${linha#*=} =~ ^[A-Za-z0-9]+$ ]]; then
  echo 'ESTACAO_API_KEY ausente ou não alfanumérica no .env' >&2
  exit 1
fi
chave=${linha#*=}
unset linha
umask 077
{
  printf '#pragma once\n\n// Gerado localmente; nunca versione.\n'
  printf 'static constexpr char SERVIDOR_URL[] = "%s";\n' "$url"
  printf 'static constexpr char SERVIDOR_CA_RAIZ[] = R"PEM('
  (( local_wokwi )) || cat -- "$ca"
  printf ')PEM";\n'
  printf 'static constexpr char ESTACAO_API_KEY[] = "%s";\n' "$chave"
  printf 'static constexpr char CAPTURA_CHAVE_PUBLICA[] = R"PEM('
  cat -- "$pub"
  printf ')PEM";\n'
} > "$raiz/firmware/sketch/secrets.h"
unset chave
echo 'secrets.h gerado: URL=[CONFIGURED], CA=[CONFIGURED], chave da estação=[CONFIGURED], chave pública=[CONFIGURED]'
