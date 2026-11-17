# Simulação no Wokwi web, com o servidor rodando no notebook

A proposta aprovada fixa a montagem "apenas no Wokwi". Não existe forma gratuita de rodar o Wokwi localmente (ver `docs/pesquisa/wokwi.md`), então a simulação roda no Wokwi web, plano gratuito, e o servidor roda no notebook levado à apresentação. A sala tem internet, que o Wokwi web exige de qualquer forma.

## Consequências

- No plano gratuito, o Wi-Fi simulado sai pelo gateway público do Wokwi na nuvem, que não alcança o `localhost` do notebook. O caminho escolhido é um túnel ngrok gratuito, com URL fixa e HTTPS; o painel é aberto por `localhost` para não gastar a cota do túnel.
- **Validar antes de construir:** o caminho Wokwi → ngrok → servidor local é o maior risco técnico e é o primeiro trabalho a ser feito, antes do resto do fluxo. Se não funcionar, as saídas, em ordem, são: ajustar o fluxo ou a configuração do túnel; usar o gateway privado do plano Hobby (`host.wokwi.internal`); por último, relatar ao professor, explicar a limitação e pedir a troca de ferramenta.
- Para que essa troca seja barata, só um módulo do firmware conhece a URL e o protocolo do servidor, e a URL fica na configuração. O servidor não sabe se há túnel na frente dele.
- O tráfego do gateway público é monitorado pelo Wokwi: só dados e imagens fictícios.
- O projeto Wokwi compartilhado é público: qualquer token colocado nele fica exposto. Usar um token exclusivo da demo e registrar o risco no checklist OWASP.
- O link do projeto Wokwi é item de entrega e precisa estar com compartilhamento ativo.
