#!/usr/bin/env bash
set +x
set -euo pipefail

# Execute com o servidor em primeiro plano em outro terminal.
: "${ESTACAO_API_KEY:?Defina ESTACAO_API_KEY no ambiente}"
: "${PAINEL_USUARIO:?Defina PAINEL_USUARIO no ambiente}"
: "${PAINEL_SENHA:?Defina PAINEL_SENHA no ambiente}"
: "${CAPTURA_CHAVE_PUBLICA_PEM:?Defina o caminho da chave pública PEM de teste}"
for valor in "$ESTACAO_API_KEY" "$PAINEL_USUARIO" "$PAINEL_SENHA"; do
  if [[ "$valor" == *$'\n'* || "$valor" == *$'\r'* ]]; then
    printf 'Credenciais não podem conter quebras de linha.\n' >&2
    exit 1
  fi
done
if [[ "$PAINEL_USUARIO" == *:* ]]; then
  printf 'O usuário Basic não pode conter dois-pontos.\n' >&2
  exit 1
fi
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
base=${PETRILENS_URL_BASE:-http://127.0.0.1:8080}
if [[ ! "$base" =~ ^http://(127\.0\.0\.1|localhost)(:[0-9]+)?$ ]]; then
  printf 'PETRILENS_URL_BASE deve apontar para HTTP em localhost.\n' >&2
  exit 1
fi
# Credenciais apenas em headers via stdin; cookie CSRF temporário com permissão restrita.
umask 077
temporario=$(mktemp -d)
trap 'rm -rf -- "$temporario"' EXIT
basic=$(printf '%s:%s' "$PAINEL_USUARIO" "$PAINEL_SENHA" | base64 | tr -d '\n')
operador() {
  printf 'Authorization: Basic %s\n' "$basic" |
    curl --fail-with-body --silent --show-error --header @- \
      --cookie "$temporario/cookies" --cookie-jar "$temporario/cookies" "$@"
}
operador "$base/api/painel/csrf" > "$temporario/csrf.json"
csrf=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' < "$temporario/csrf.json")
{
  printf 'Authorization: Basic %s\n' "$basic"
  printf 'X-XSRF-TOKEN: %s\n' "$csrf"
} | curl --fail-with-body --silent --show-error --header @- \
  --cookie "$temporario/cookies" -X PUT -H 'Content-Type: application/json' \
  --data-binary '{"amostra_id":"A5"}' "$base/api/painel/amostra-selecionada" > "$temporario/selecao.json"
python3 -c 'import json,sys; assert json.load(sys.stdin)=={"lote_id":"L2026-002","amostra_id":"A5"}' < "$temporario/selecao.json"
# Simula a estação recebendo a seleção e informando-a no POST seguinte.
for tentativa in 1 2; do
  if [[ "$tentativa" == 1 ]]; then
    cp "$raiz/exemplos/sincronizacao.json" "$temporario/estado.json"
  else
    python3 -c 'import json,sys; d=json.load(sys.stdin); d["amostra_id"]="A5"; print(json.dumps(d))' \
      < "$raiz/exemplos/sincronizacao.json" > "$temporario/estado.json"
  fi
  printf 'X-Api-Key: %s\n' "$ESTACAO_API_KEY" |
    curl --fail-with-body --silent --show-error --header @- \
      -H 'Content-Type: application/json' --data-binary "@$temporario/estado.json" \
      "$base/api/estacao/sincronizacao" > "$temporario/resposta.json"
  python3 -c 'import json,sys; d=json.load(sys.stdin); assert d["amostra_selecionada"]=={"lote_id":"L2026-002","amostra_id":"A5"}; assert d["comandos"]==[]' \
    < "$temporario/resposta.json"
done
operador "$base/api/painel/estado" > "$temporario/painel.json"
python3 -c 'import json,sys; d=json.load(sys.stdin); assert d["online"]; assert d["amostra_selecionada"]["amostra_id"]==d["estado"]["amostra_id"]=="A5"' < "$temporario/painel.json"
printf 'OK: seleção A5 salva, devolvida na sincronização e informada no POST seguinte.\n'
printf 'Sem novos POSTs, após mais de 15 s o painel autenticado deve mostrar offline.\n'

# Cifra a fixture com a chave pública de teste, sem imprimir chaves ou corpo.
java "$raiz/scripts/CifrarCapturaLocal.java" "$CAPTURA_CHAVE_PUBLICA_PEM" \
  "$raiz/exemplos/placa-ficticia.jpg" "$raiz/exemplos/captura.json" "$temporario/captura.json"
bytes=$(wc -c < "$temporario/captura.json")
test "$bytes" -le 65536
printf 'Captura cifrada: %d bytes de corpo HTTP (limite 65536).\n' "$bytes"
printf 'X-Api-Key: %s\n' "$ESTACAO_API_KEY" |
  curl --fail-with-body --silent --show-error --header @- \
    -H 'Content-Type: application/json' --data-binary "@$temporario/captura.json" \
    --output "$temporario/recebida.json" --write-out '%{http_code}' \
    "$base/api/estacao/capturas" > "$temporario/status"
test "$(cat "$temporario/status")" = 201
id=$(python3 -c 'import json,sys; d=json.load(sys.stdin); assert isinstance(d["id"],int) and d["id"]>0; assert d["recebida_em"]; print(d["id"])' < "$temporario/recebida.json")
operador "$base/api/amostras/A5/capturas" > "$temporario/lista.json"
python3 -c 'import json,sys; lista=json.load(sys.stdin); assert any(c["id"]==int(sys.argv[1]) and c["lux"]==542 for c in lista)' "$id" < "$temporario/lista.json"
operador "$base/api/capturas/$id/imagem" > "$temporario/imagem.jpg"
cmp "$raiz/exemplos/placa-ficticia.jpg" "$temporario/imagem.jpg"
printf 'OK: captura vinculada a A5, listada e JPEG recuperado byte a byte.\n'
