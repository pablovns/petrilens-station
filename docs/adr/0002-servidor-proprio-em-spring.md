# Servidor próprio e mínimo em Java/Spring, com painel web

A proposta fala em enviar os dados "ao sistema PetriLens", mas o backend do TCC (FastAPI + YOLOv8 + PostgreSQL) não existe ainda. Decidimos construir um servidor próprio e mínimo para a estação, em Java/Spring, que recebe as capturas, guarda o histórico, expõe os comandos e serve um painel web. Spring porque os três integrantes dominam Java/Spring e todos precisam explicar o código na defesa individual; nenhum documento da disciplina exige uma stack.

## Considered Options

- **FastAPI (stack do TCC):** rejeitado; o grupo não domina e a defesa cobra entendimento de todo o código.
- **Node-RED / ThingSpeak:** rejeitado; o critério Código (20%) avalia organização e tratamento de erros, que aparecem melhor em código próprio.
- **App Flutter (stack do TCC):** rejeitado; a proposta aceita "aplicativo ou painel", e um painel web servido pelo próprio servidor é muito mais simples.

## Consequências

- Sem inferência de IA (YOLO): o servidor registra a captura e a imagem, não conta colônias.
- Versões de Java, Spring e dependências são decididas na implementação.
