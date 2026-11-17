# Estação de captura PetriLens

Estação que fotografa placas de Petri em condições padronizadas e registra cada foto associada à amostra de origem, para análise posterior no PetriLens.

## Language

**Estação**:
Equipamento na bancada que mantém a placa em posição fixa, ilumina e fotografa. Neste trabalho existe uma só, simulada no Wokwi.
_Avoid_: dispositivo, scanner

**Operador**:
Técnico de laboratório que usa a estação e o painel; é o único usuário do sistema.
_Avoid_: usuário, analista

**Painel**:
Aplicação web, separada da estação, onde o operador escolhe a amostra selecionada, dispara capturas e consulta as capturas da amostra.
_Avoid_: dashboard, app

**Captura**:
Uma foto da placa feita pela estação, com os dados do momento (luz medida, amostra, horário). É a entrada para uma análise futura.
_Avoid_: análise, foto, leitura

**Análise**:
Processamento de uma captura pelo PetriLens para contar colônias e identificar, por exemplo, *Salmonella*. Fica fora deste trabalho: aqui nenhuma captura é analisada.

**Lote**:
Conjunto de amostras recebidas juntas pelo laboratório, já existente no sistema antes de qualquer captura.

**Amostra**:
Material de um lote cultivado em placa, já existente no sistema antes da captura. Uma amostra pode ter várias capturas, e cada captura é analisada separadamente.

**Sincronização**:
Troca periódica iniciada pela estação, em que ela informa seu estado e recebe os comandos pendentes e a amostra selecionada.

**Amostra selecionada**:
A amostra à qual a estação vincula toda captura nova; é escolhida pelo operador no painel e sempre existe uma.
_Avoid_: amostra atual, amostra ativa
