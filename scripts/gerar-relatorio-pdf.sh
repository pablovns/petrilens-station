#!/usr/bin/env bash
set -euo pipefail

LC_ALL=C
export LC_ALL

pandoc docs/relatorio.md \
  --pdf-engine=typst \
  --resource-path=docs \
  --lua-filter=scripts/links-relatorio.lua \
  -M lang=pt-BR \
  -V papersize=a4 \
  --metadata-file=<(printf 'margin: {x: 22mm, y: 20mm}\n') \
  -V mainfont='Noto Serif' \
  -V fontsize=11pt \
  -V page-numbering=1 \
  -o docs/relatorio.pdf

informacoes=$(pdfinfo docs/relatorio.pdf)
paginas=$(awk '/^Pages:/ {print $2}' <<< "$informacoes")
if [[ ! $paginas =~ ^[0-9]+$ ]]; then
  echo 'Não foi possível determinar a contagem de páginas.' >&2
  exit 1
fi

printf 'Páginas: %s\n' "$paginas"
if (( paginas < 8 || paginas > 15 )); then
  echo 'O relatório deve ter de 8 a 15 páginas.' >&2
  exit 1
fi
