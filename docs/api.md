# API — PetriLens Station

Servidor local: `http://127.0.0.1:8080`. JSON em UTF-8, nomes `snake_case`. Uma estação fixa: `est-01`. Autenticação obrigatória: `X-Api-Key` para `/api/estacao/**`; HTTP Basic para painel, assets e todas as demais rotas. Apenas dados fictícios na demo. Na demo, a estação acessa o servidor pelo gateway privado em `http://host.wokwi.internal:8080`. O firmware aceita HTTP apenas para o prefixo `http://host.wokwi.internal:` e exige HTTPS com validação da CA para os demais destinos. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)

## POST /api/estacao/sincronizacao

Iniciado pela estação a cada 5 s, com `Content-Type: application/json`. O cliente envia `X-Api-Key` com a chave configurada em `ESTACAO_API_KEY` e mantém o cabeçalho `ngrok-skip-browser-warning: true` para o túnel opcional. Basic não autoriza esta API. A ausência de chave válida retorna **401** JSON sem `WWW-Authenticate`, antes de processar o corpo ou alterar o estado.

```json
{
  "estacao_id": "est-01",
  "presenca": false,
  "lux": 0,
  "brilho": 10,
  "amostra_id": null,
  "rssi": -55,
  "uptime_s": 5
}
```

| Campo | Regra |
|---|---|
| `estacao_id` | obrigatório, texto igual a `est-01` |
| `presenca` | obrigatório, booleano |
| `lux` | obrigatório, número finito maior ou igual a zero |
| `brilho` | obrigatório, inteiro de 0 a 10 |
| `amostra_id` | texto de 1 a 64 caracteres (`A-Z`, `a-z`, `0-9`, `_`, `-`), ou nulo/ausente |
| `rssi` | obrigatório, inteiro de -127 a 0, em dBm |
| `uptime_s` | obrigatório, inteiro de 0 a 9223372036854775807, em segundos |
| `motivo_recusa` | opcional, texto `lux_fora_da_faixa`, nulo ou ausente |

`motivo_recusa` registra a recusa da última tentativa que chegou à verificação de luz. A estação inclui o motivo nas sincronizações seguintes até uma tentativa passar nessa verificação. O servidor devolve o campo em `estado` no GET do painel enquanto constar no último estado recebido. Uma sincronização válida com o campo ausente ou nulo remove o motivo da consulta. A recusa fica apenas no estado da estação e não gera registro na lista de capturas. O campo `lux` continua sendo a leitura da sincronização.

O firmware permite a captura com placa presente e leitura entre **300 e 700 lx**, inclusive. Uma leitura abaixo, acima ou não finita recusa a captura. A API aceita o corpo antigo sem `motivo_recusa`. Para conferir no simulador, dispare uma captura em 500 lx, depois em 250 e 800 lx, e por fim em cada limite, 300 e 700 lx. Após cada tentativa, aguarde a sincronização e consulte `GET /api/painel/estado`. Nos casos fora da faixa, confira `estado.motivo_recusa` igual a `lux_fora_da_faixa` e a lista de capturas sem novo registro. Em 500, 300 e 700 lx, confira a captura registrada e o campo ausente. [Roteiro de demonstração](../README.md#recusa-por-lux-9)

Campos desconhecidos, números em texto, frações em campos inteiros, campos obrigatórios ausentes ou nulos e JSON malformado são recusados. O estado informado aceita identificador sintaticamente válido sem validar sua existência: ele registra o que a estação informou, independentemente da seleção. No primeiro POST após ligar, o firmware informa `amostra_id: null`; depois informa a seleção recebida no POST anterior.

Resposta **200**:

```json
{"amostra_selecionada":{"lote_id":"L2026-001","amostra_id":"A1"},"comandos":[]}
```

A seleção sempre é um objeto com `lote_id` e `amostra_id`, lido do H2 em cada sincronização. O firmware mantém cópias próprias das strings e informa a amostra em uso na sincronização seguinte. O array `comandos` contém até um comando, entregue por ordem de id. Quando há um pendente, a seleção e a entrega são obtidas na mesma transação H2. A marcação como `entregue` corresponde ao envio na resposta, sem confirmação de execução nem reentrega se a resposta se perder. A captura usa requisição própria e não tem fila offline. O servidor registra atomicamente o último estado aceito com seu horário UTC de chegada. Esse último estado é volátil; a seleção persiste ao reiniciar.

## GET /api/painel/estado

Exige `Authorization: Basic <base64(usuario:senha)>`, com `PAINEL_USUARIO` e `PAINEL_SENHA`. Sem login válido: **401** JSON com `WWW-Authenticate: Basic realm="PetriLens", charset="UTF-8"`, inclusive para `/`, `/index.html` e `/painel.js`. `X-Api-Key` não autoriza nenhuma dessas rotas.

Resposta **200**, antes da primeira sincronização:

```json
{"estacao_id":"est-01","online":false,"ultima_sincronizacao":null,"estado":null,"amostra_selecionada":{"lote_id":"L2026-001","amostra_id":"A1"}}
```

Após receber o exemplo:

```json
{
  "estacao_id": "est-01",
  "online": true,
  "ultima_sincronizacao": "2026-11-17T18:00:00Z",
  "estado": {
    "estacao_id": "est-01",
    "presenca": false,
    "lux": 0,
    "brilho": 10,
    "amostra_id": null,
    "rssi": -55,
    "uptime_s": 5
  },
  "amostra_selecionada": {"lote_id":"L2026-001","amostra_id":"A1"}
}
```

`online` é calculado em cada consulta: verdadeiro enquanto o horário da consulta for menor ou igual ao horário da última sincronização mais 15 s; falso após esse limite. Offline preserva os últimos dados e o horário. Um POST inválido não muda o estado nem renova esse horário. O relógio é do servidor; a estação não fornece a data.

## GET /api/lotes

Exige Basic. Retorna **200**, catálogo fixo de dois lotes e seis identificadores globalmente únicos:

```json
[
  {"lote_id":"L2026-001","amostras":[{"lote_id":"L2026-001","amostra_id":"A1"},{"lote_id":"L2026-001","amostra_id":"A2"},{"lote_id":"L2026-001","amostra_id":"A3"}]},
  {"lote_id":"L2026-002","amostras":[{"lote_id":"L2026-002","amostra_id":"A4"},{"lote_id":"L2026-002","amostra_id":"A5"},{"lote_id":"L2026-002","amostra_id":"A6"}]}
]
```

## PUT /api/painel/amostra-selecionada

Exige Basic, cookie e header CSRF, e `Content-Type: application/json`:

```json
{"amostra_id":"A5"}
```

Resposta **200**: `{"lote_id":"L2026-002","amostra_id":"A5"}`. Identificador desconhecido, vazio, nulo, tipo incorreto, campos extras ou JSON inválido retornam **400** sem alterar a seleção. O campo é obrigatório e tem no máximo 64 caracteres. O banco garante uma seleção não nula referindo uma amostra existente. Na primeira inicialização ela é A1; a carga idempotente do catálogo não sobrescreve a escolha em reinícios. Não há cadastro de lotes/amostras.

## POST /api/painel/comandos

Exige Basic, cookie e header CSRF e `Content-Type: application/json`. Tipos aceitos:

| Corpo | Ação na estação |
|---|---|
| `{"tipo":"capturar"}` | Captura com a amostra devolvida na sincronização que trouxe o comando, sujeita à mesma regra local do botão. |
| `{"tipo":"status"}` | Solicita outra sincronização no próximo giro do laço, sem esperar o intervalo de 5 s. |
| `{"tipo":"ajustar_brilho","valor":4}` | Ajusta de 0 a 10 segmentos quando há placa. A estação informa o brilho na próxima sincronização. |

Para `capturar` e `status`, `valor` pode ser omitido ou nulo. Para brilho, o valor deve ser inteiro JSON entre 0 e 10. Tipo desconhecido, valor inválido ou campos extras retornam **400**.

Resposta **201**: `{"id":1,"tipo":"ajustar_brilho","valor":4,"estado":"pendente"}`.

## GET /api/painel/comandos

Exige Basic. Resposta **200** com a lista em ordem crescente de id. Cada item tem `id`, `tipo`, `valor` e `estado`. O estado muda de `pendente` para `entregue` quando o servidor inclui o comando na resposta da sincronização. Para comandos sem valor, `valor` é nulo. Sem registros, retorna `[]`.

Quando um comando é entregue, a sincronização retorna, por exemplo:

```json
{"amostra_selecionada":{"lote_id":"L2026-001","amostra_id":"A1"},"comandos":[{"id":1,"tipo":"ajustar_brilho","valor":4,"estado":"entregue"}]}
```

## Erros

Validação ou JSON inválido: **400**, por exemplo:

```json
{"mensagem":"Estado inválido: confira os campos e limites em docs/api.md."}
```

Erros HTTP tratados centralmente também retornam JSON com `mensagem`. Falhas inesperadas retornam **500** com mensagem genérica, sem stack trace, nomes de classes ou detalhes internos.

## Autenticação e CSRF

Credenciais vêm do `.env` no diretório de execução ou do ambiente, sem fallback; veja o formato no README. Não há login por formulário, sessão de autenticação, cookie `JSESSIONID` ou endpoint de logout. O navegador reenvia Basic a cada chamada; fechar a janela pode não limpar o cache Basic do navegador. Não coloque credenciais em URLs, logs ou argumentos de comandos.

Somente a cadeia `/api/estacao/**`, autenticada pelo cabeçalho da estação, é isenta de CSRF. No lado do operador a proteção permanece ativa, com repositório de token em cookie `XSRF-TOKEN` (HttpOnly) e header `X-XSRF-TOKEN`. O token mascarado pelo tratamento XOR padrão do Spring vem de `GET /api/painel/csrf`, autenticado por Basic:

```json
{"header_name":"X-XSRF-TOKEN","token":"<token-mascarado>"}
```

A resposta também estabelece o cookie HttpOnly `XSRF-TOKEN`. O cliente deve conservar o cookie e enviar o valor JSON do token no header indicado em cada PUT/POST do operador. O cookie sozinho não basta; não use seu valor bruto no header. Token ausente/incorreto retorna **403** JSON e impede a operação. O polling com Basic não invalida o token; a estratégia de rotação por login foi dispensada porque Basic reautentica em cada chamada e não há sessão nem login customizado. A máscara XOR e a verificação do token continuam padrão. O painel usa as credenciais Basic do navegador, sem armazená-las em JavaScript.


Erros de segurança seguem `{"mensagem":"..."}` e não expõem credenciais. O desafio Basic só aparece no lado do operador. A chave da demo fica visível no projeto Wokwi público (ADR 0003); deve ser exclusiva e descartável. No modo HTTPS, a CA deve corresponder à cadeia do destino. Para o domínio ngrok inspecionado, use a ISRG Root X1. [Credenciais](credenciais.md#identificação-da-ca)

## POST /api/estacao/capturas

Exige `X-Api-Key` da estação e `Content-Type: application/json`. Basic não substitui a chave. O [exemplo estrutural](../exemplos/captura.json) usa marcadores e precisa de valores gerados com a chave pública do servidor.

```json
{"estacao_id":"est-01","lote_id":"L2026-001","amostra_id":"A1","lux":542,"resolucao":"800x600","simulacao":true,"chave_cifrada":"<Base64 RSA-OAEP>","iv":"<Base64 IV>","imagem_cifrada":"<Base64 AES-GCM>"}
```

Todos os campos são obrigatórios e não nulos. Não há coerção de tipos nem campos extras.

| Campo | Regra |
|---|---|
| `estacao_id` | texto igual a `est-01` |
| `lote_id`, `amostra_id` | textos de 1 a 64 caracteres, amostra existente e pertencente ao lote informado |
| `lux` | número finito maior ou igual a zero |
| `resolucao` | texto igual a `800x600` |
| `simulacao` | booleano `true`, somente imagem fictícia |
| `chave_cifrada` | 256 bytes em Base64 (344 caracteres). Chave AES de 16 bytes cifrada com RSA 2048, OAEP SHA-256, MGF1 SHA-256 e rótulo vazio |
| `iv` | 12 bytes aleatórios em Base64 (16 caracteres) |
| `imagem_cifrada` | JPEG 800×600 cifrado com AES-128-GCM, sem dados associados, seguido da tag de 16 bytes. Codifique ciphertext e tag juntos em Base64 |

Use Base64 padrão com padding quando necessário, sem espaços ou quebras de linha. Gere uma chave AES e um IV novos a cada captura. O servidor autentica e decifra a imagem antes de validá-la e armazenar o JPEG original.

O **corpo HTTP inteiro** é limitado a **65.536 bytes**, inclusive JSON, espaços e Base64. Excesso retorna **413** antes de parsing/decodificação, mesmo sem Content-Length e em chunked. Exatamente 65.536 bytes são aceitos se o JSON/imagem forem válidos. Não há upload multipart. Campo ausente, tipo incorreto, JSON/Base64/JPEG inválido, falha de decifragem (incluindo dados adulterados ou cifrados para outra chave pública), resolução divergente, amostra inexistente ou vínculo lote/amostra incorreto retornam **400** genérico, sem gravar arquivo ou registro. Sem `X-Api-Key` válida, retorna **401**. Se o servidor estiver sem a chave privada de captura, a submissão válida retorna **503** com mensagem explícita. Falha de persistência retorna **500** genérico, sem caminho interno.

Resposta **201**:

```json
{"id":1,"recebida_em":"2026-11-17T18:00:00Z"}
```

`id` é numérico, positivo e gerado pelo servidor. `recebida_em` é o horário UTC do servidor ao começar a atender a captura; a estação não envia data. A associação usa a **amostra informada neste pedido**, mesmo se a seleção mudou durante o envio. JPEG original em disco e metadados no H2 sobrevivem reinício. A sincronização continua independente: POST de captura não renova o online. Até três tentativas do firmware reutilizam os mesmos metadados, chave cifrada, IV e bytes cifrados; não existe fila offline ou deduplicação. Se uma resposta se perder depois de salvar, o retry pode registrar outra captura.

A imagem segue cifrada até a API também no HTTP local. A chave da estação, os metadados e a sincronização trafegam sem TLS dentro do notebook. Quando há um túnel HTTPS, esses dados ficam visíveis ao terminador TLS. [ADR 0007](adr/0007-imagem-cifrada-para-a-api.md) · [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)

## GET /api/amostras/{amostra_id}/capturas

Exige HTTP Basic do operador. Resposta **200**, da mais recente para a mais antiga (id decrescente em empate):

```json
[{"id":1,"recebida_em":"2026-11-17T18:00:00Z","lux":542.0}]
```

Amostra existente sem capturas: `[]`. Amostra inexistente: **404**. Consulta a amostra do caminho, independente da seleção. A chave da estação não autoriza acesso (**401**).

## GET /api/capturas/{id}/imagem

Exige HTTP Basic. Resposta **200** `Content-Type: image/jpeg`, com bytes idênticos aos do JPEG antes de cifrar. O caminho no disco é derivado exclusivamente do id numérico gerado pelo servidor; não se aceita caminho do cliente. Id inexistente/não positivo: **404**; id sem formato inteiro: **400**. Sem Basic válido: **401**, inclusive com chave da estação.

O painel consulta capturas da amostra selecionada a cada ciclo de 2 s, com horário, lux e miniatura que abre a imagem completa. Respostas de uma seleção anterior são descartadas.
