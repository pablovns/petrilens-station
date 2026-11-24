package br.com.petrilens.estacao;

import br.com.petrilens.amostra.Amostras;
import br.com.petrilens.amostra.Amostras.Amostra;
import br.com.petrilens.comando.Comandos;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstacaoController {
    private final Clock clock;
    private final Amostras amostras;
    private final Comandos comandos;
    // Cada requisição lê o estado e o horário juntos, mesmo com sincronizações simultâneas.
    private volatile UltimaSincronizacao ultima;

    public EstacaoController(Clock clock, Amostras amostras, Comandos comandos) {
        this.clock = clock;
        this.amostras = amostras;
        this.comandos = comandos;
    }

    @PostMapping("/api/estacao/sincronizacao")
    public RespostaSincronizacao sincronizar(@Valid @RequestBody EstadoInformado estado) {
        ultima = new UltimaSincronizacao(estado, clock.instant());
        var entrega = comandos.entregar();
        return new RespostaSincronizacao(entrega.amostraSelecionada(), entrega.comandos());
    }

    @GetMapping("/api/painel/estado")
    public EstadoPainel consultar() {
        UltimaSincronizacao sincronizacao = ultima;
        if (sincronizacao == null) {
            return new EstadoPainel("est-01", false, null, null, amostras.selecionada());
        }
        // A estação fica online até 15 s após a última sincronização, inclusive.
        boolean online = !clock.instant().isAfter(sincronizacao.recebidaEm().plusSeconds(15));
        return new EstadoPainel("est-01", online, sincronizacao.recebidaEm(), sincronizacao.estado(), amostras.selecionada());
    }

    private record UltimaSincronizacao(EstadoInformado estado, Instant recebidaEm) {}

    public record RespostaSincronizacao(Amostra amostraSelecionada, List<Comandos.Comando> comandos) {}

    public record EstadoPainel(String estacaoId, boolean online, Instant ultimaSincronizacao,
            EstadoInformado estado, Amostra amostraSelecionada) {}
}
