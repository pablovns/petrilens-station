package br.com.petrilens.comando;

import br.com.petrilens.erro.TratamentoErro.Erro;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ComandoController {
    private final Comandos comandos;

    public ComandoController(Comandos comandos) {
        this.comandos = comandos;
    }

    @GetMapping("/api/painel/comandos")
    public List<Comandos.Comando> listar() {
        return comandos.listar();
    }

    @PostMapping("/api/painel/comandos")
    public ResponseEntity<?> criar(@RequestBody JsonNode pedido) {
        JsonNode campoTipo = pedido.path("tipo");
        String tipo = campoTipo.isTextual() ? campoTipo.textValue() : "";
        JsonNode valor = pedido.path("valor");
        if (!temApenasCamposPermitidos(pedido) || !valorValidoPara(tipo, valor)) {
            return ResponseEntity.badRequest().body(new Erro(
                    "Comando inválido: tipo capturar/status sem valor ou ajustar_brilho com inteiro de 0 a 10."));
        }
        Integer valorComando = tipo.equals("ajustar_brilho") ? valor.intValue() : null;
        return ResponseEntity.status(201).body(comandos.criar(tipo, valorComando));
    }

    private boolean temApenasCamposPermitidos(JsonNode pedido) {
        if (!pedido.isObject()) {
            return false;
        }
        var campos = pedido.fieldNames();
        while (campos.hasNext()) {
            if (!Set.of("tipo", "valor").contains(campos.next())) {
                return false;
            }
        }
        return true;
    }

    private boolean valorValidoPara(String tipo, JsonNode valor) {
        if (tipo.equals("ajustar_brilho")) {
            return valor.isIntegralNumber() && valor.canConvertToInt()
                    && valor.intValue() >= 0 && valor.intValue() <= 10;
        }
        return Set.of("capturar", "status").contains(tipo) && (valor.isMissingNode() || valor.isNull());
    }
}
