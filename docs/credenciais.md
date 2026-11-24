# Credenciais da demonstração

## Inventário

| Item | Uso e origem | Onde fica e quem vê | Efeito da exposição |
| --- | --- | --- | --- |
| `ESTACAO_API_KEY` | Autentica a estação via `X-Api-Key`. O script gera 24 bytes aleatórios em hexadecimal com `openssl rand -hex 24`. | `.env` no notebook e `secrets.h` no firmware. Visível ao ngrok se o túnel for usado e a quem abrir o projeto Wokwi web se for incluída ali. | Permite se passar pela estação, enviar sincronizações e capturas e receber comandos pendentes. |
| `PAINEL_USUARIO` e `PAINEL_SENHA` | Login HTTP Basic do operador. Ambos são escolhidos na configuração e a senha é digitada sem eco. | Persistem apenas no `.env`. O operador os informa ao navegador em localhost. | O par permite consultar capturas e metadados, trocar a amostra selecionada e enviar comandos. |
| RSA 2048 | O script gera a privada com `openssl genpkey` e extrai a pública com `openssl pkey -pubout`. A pública cifra a chave AES de cada imagem. | Privada em PEM PKCS#8, `~/.config/petrilens/captura-privada.pem` (600, diretório 700), acessível à conta local do servidor. `CAPTURA_CHAVE_PRIVADA` contém o caminho absoluto no `.env`. A pública fica ao lado da privada e aparece como `CAPTURA_CHAVE_PUBLICA` no `secrets.h`. | A privada permite decifrar imagens de tráfego previamente coletado com esse par. A pública pode ser divulgada. |
| `SERVIDOR_CA_RAIZ` | Âncora pública da validação TLS. No domínio ngrok inspecionado, é a ISRG Root X1. Fica vazia no modo local. | `secrets.h`, visível no Wokwi. | A divulgação é esperada. Alterar a âncora pode romper a conexão ou fazê-la confiar na CA errada. |
| `SERVIDOR_URL` | `http://host.wokwi.internal:8080` na demo. Para um ESP32 real, URL HTTPS do servidor ou do túnel opcional. | `secrets.h`. O endereço da demo aponta para o gateway privado no notebook. | Revela onde o serviço pode ser alcançado enquanto o túnel está ativo. As rotas exigem autenticação. |
| Authtoken do ngrok | Usado apenas com o túnel opcional, para autenticar o agente no ngrok. | `ngrok config add-authtoken` grava o token na configuração do usuário, normalmente `~/.config/ngrok/ngrok.yml` no Linux, fora do Git. A conta e o agente local o usam. | Permite iniciar túneis em nome da conta dentro das permissões do token. Revogue-o no ngrok se for exposto. |

## Configuração no notebook

1. Na raiz, execute `./scripts/preparar-demo.sh` e escolha o usuário e a senha do painel. O script preserva o `.env` e o par RSA existentes. O `.env` usa Java properties (`NOME=valor`), com senha alfanumérica e usuário que também aceita ponto, hífen e sublinhado. Inicie o servidor conforme o README. [Script](../scripts/preparar-demo.sh) · [README](../README.md#servidor-e-prova-local)
2. Gere o cabeçalho com `./scripts/gerar-secrets.sh http://host.wokwi.internal:8080`. O modo local dispensa CA. Compile e inicie o Wokwi para VS Code conforme o README. [Script](../scripts/gerar-secrets.sh) · [Simulação](../README.md#simular-no-wokwi-para-vs-code)

## Modo HTTPS opcional

Para um ESP32 real, configure a URL HTTPS e a CA do servidor. Se for usar ngrok como túnel:

1. Obtenha o authtoken no painel da conta ngrok e registre-o com `ngrok config add-authtoken <authtoken>`, omitindo o valor de gravações e do histórico do shell.
2. Em outro terminal, execute `ngrok http 8080 --url https://<dominio-dev>` com o domínio da conta, em primeiro plano.
3. Confira a cadeia do domínio conforme a seção abaixo. Use o PEM da CA do sistema.
4. Gere o cabeçalho com `./scripts/gerar-secrets.sh 'https://<dominio-dev>' /etc/ssl/certs/ISRG_Root_X1.pem`, substituindo o domínio. O segundo argumento é obrigatório. Os PEMs são incorporados inteiros em literais C++ `R"PEM(...)PEM"`. [Script](../scripts/gerar-secrets.sh)

## Identificação da CA

Na inspeção do domínio ngrok com OpenSSL, `-showcerts` mostrou a folha e as intermediárias da cadeia `*.ngrok-free.dev` ← Let's Encrypt YE2 ← ISRG Root YE ← ISRG Root X2 (assinada de forma cruzada) ← ISRG Root X1. A X1 vem do repositório de confiança do sistema em `/etc/ssl/certs/ISRG_Root_X1.pem`. Sua impressão digital SHA-256 começa por `96:BC:EC:06` e o certificado vale até 2035.

```sh
# Defina HOST com o domínio da conta, sem https://.
openssl s_client -connect "$HOST:443" -servername "$HOST" -showcerts </dev/null
openssl s_client -connect "$HOST:443" -servername "$HOST" -verify_hostname "$HOST" \
  -CAfile /etc/ssl/certs/ISRG_Root_X1.pem -no-CApath -no-CAstore -verify_return_error </dev/null
openssl x509 -in /etc/ssl/certs/ISRG_Root_X1.pem -noout -fingerprint -sha256 -enddate
```

A validação OpenSSL com apenas a X1 retornou `Verify return code: 0`. Com DigiCert Global Root G2, falhou com `unable to get local issuer certificate`. Essa recusa fundamenta o caso P3, aplicável ao modo HTTPS. A folha da Let's Encrypt troca em cerca de 90 dias. A raiz é mais duradoura e permite validar folhas renovadas da mesma cadeia. O firmware usa `setCACert()` e mantém a validação ativa, sem `setInsecure()`. [Cliente TLS](../firmware/sketch/cliente_servidor.cpp) · [Caso P3](roteiro-wokwi.md)

## Proteções e limites

- O Git ignora `.env`, `secrets.h` e `*.pem`. Os exemplos versionados têm campos de credenciais vazios. Os scripts ocultam os valores e usam `umask 077`. A privada fica fora do repositório. [Regras do Git](../.gitignore) · [Exemplo do servidor](../.env.example) · [Exemplo do firmware](../firmware/sketch/secrets.example.h)
- O servidor recusa a partida com chave da estação, usuário ou senha ausentes ou vazios. Exige Basic nas rotas do operador e CSRF nas alterações do painel. O operador usa HTTP apenas em localhost. Não há sessão de autenticação nem logout no servidor, e o navegador pode manter o Basic em cache. [Credenciais.java](../servidor/src/main/java/br/com/petrilens/seguranca/Credenciais.java) · [Seguranca.java](../servidor/src/main/java/br/com/petrilens/seguranca/Seguranca.java) · [Contrato da API](api.md)
- Quando usado, o ngrok termina o TLS e vê cabeçalhos, JSON e metadados. O JPEG no pedido segue cifrado em AES-GCM. A chave AES é cifrada com RSA-OAEP SHA-256 (também no MGF1). A API decifra e verifica a tag. O JPEG original da demo é fictício e acompanha os arquivos do projeto público. [ADR 0007](adr/0007-imagem-cifrada-para-a-api.md) · [imagem.cpp](../firmware/sketch/imagem.cpp) · [ImagemCifrada.java](../servidor/src/main/java/br/com/petrilens/captura/ImagemCifrada.java)
- Configurações incluídas no projeto Wokwi web ficam visíveis a quem o abrir. Use um token exclusivo da demo e troque-o após o ensaio. Para trocar o token e o login, pare o servidor, retire o `.env` antigo da raiz e rode `preparar-demo.sh` de novo. Se for trocar o RSA, retire também os dois PEMs antigos da pasta de chaves antes de rodar o script. Reinicie o servidor, rode `gerar-secrets.sh` e recompile o sketch local. [ADR 0003](adr/0003-simulacao-no-wokwi-web.md) · [Checklist OWASP](owasp-iot-top-10.md)

## Perguntas prováveis na defesa

1. **Como a estação e o operador se autenticam?** A estação envia `X-Api-Key` e o operador usa HTTP Basic. Cada credencial autoriza o conjunto de rotas correspondente.
2. **Por que a chave da estação pode aparecer no Wokwi?** O compartilhamento expõe o código da demo. Enquanto a chave estiver ativa, outra pessoa pode se passar pela estação. Ela é descartável e os dados são fictícios.
3. **O ngrok consegue ler o que passa pelo túnel?** Ele abre o TLS e lê o JSON, inclusive os metadados. Para abrir o JPEG cifrado no pedido precisaria da chave RSA privada.
4. **Por que usar AES e RSA juntos?** O AES-GCM cifra o JPEG e autentica seu conteúdo. O RSA-OAEP protege a chave AES aleatória daquela captura.
5. **Por que confiar na raiz e o que ocorre com uma CA errada?** A raiz continua válida quando a folha é renovada na mesma cadeia. Com uma CA alheia à cadeia, a negociação TLS deve falhar antes do primeiro POST.
6. **Por que há CSRF com HTTP Basic?** O navegador pode reenviar Basic automaticamente. Para alterar estado, a aplicação também exige o cookie e o cabeçalho CSRF.
7. **O que acontece se a chave RSA privada faltar?** O servidor responde 503 ao envio de captura válido. A sincronização e o painel continuam funcionando.
8. **Por que HTTP no modo local?** O TLS com o ngrok falhou no ESP32 simulado. O gateway privado permite HTTP dentro do notebook, com `X-Api-Key` e a imagem cifrada até a API. [ADR 0008](adr/0008-wokwi-vscode-com-gateway-local.md)
