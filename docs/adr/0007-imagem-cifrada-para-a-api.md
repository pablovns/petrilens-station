# Imagem da captura cifrada de ponta a ponta para a API

A imagem da captura só pode ser lida pela API, nem por quem está na mesma rede nem por intermediários. No plano gratuito, o ngrok abre o HTTPS na sua borda e repassa o conteúdo aberto ao notebook; criptografia de ponta a ponta pelo próprio ngrok (TLS endpoints) é paga. Por isso a estação cifra a imagem antes de enviar: gera uma chave AES aleatória por captura, cifra a imagem com AES-GCM e cifra essa chave com a chave pública RSA do servidor (OAEP com SHA-256). Só a chave privada, guardada no servidor fora do repositório, abre a imagem. Além disso, a estação valida o certificado HTTPS do servidor em vez de aceitar qualquer certificado.

## Considered Options

- **Só HTTPS com validação de certificado:** protege contra a rede local e o gateway do Wokwi, mas o ngrok continua vendo a imagem.
- **TLS de ponta a ponta pelo ngrok:** exige plano pago.
- **Chave simétrica compartilhada:** ficaria exposta no projeto Wokwi, que é público; com chave pública, a parte que fica no firmware pode ser pública sem risco.

## Consequências

- Só a imagem é cifrada; os demais dados da captura e a sincronização seguem apenas com HTTPS e continuam visíveis para o ngrok.
- Na demo, a imagem original está no próprio projeto Wokwi público: a proteção demonstrada é a do transporte, que passa a valer de fato com uma câmera real.
- O GCM também detecta adulteração: imagem alterada no caminho é recusada pelo servidor.
- Esta é a correção derivada do checklist OWASP IoT Top 10 (transferência insegura de dados) e reforça o item de criptografia na comunicação do adicional de segurança.
