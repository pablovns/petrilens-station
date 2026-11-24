package br.com.petrilens.amostra;

import br.com.petrilens.amostra.Amostras.Lote;
import br.com.petrilens.erro.TratamentoErro.Erro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AmostraController {
    private final Amostras amostras;

    public AmostraController(Amostras amostras) {
        this.amostras = amostras;
    }

    @GetMapping("/api/lotes")
    public List<Lote> listar() {
        return amostras.listarLotes();
    }

    public record Selecao(@NotBlank @Size(max = 64) String amostraId) {}

    @PutMapping("/api/painel/amostra-selecionada")
    public ResponseEntity<?> selecionar(@Valid @RequestBody Selecao selecao) {
        return amostras.selecionar(selecao.amostraId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body(new Erro("Amostra inexistente.")));
    }
}
