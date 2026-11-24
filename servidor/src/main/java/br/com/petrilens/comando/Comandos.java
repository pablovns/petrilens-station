package br.com.petrilens.comando;

import br.com.petrilens.amostra.Amostras;
import br.com.petrilens.amostra.Amostras.Amostra;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Comandos {
    private final JdbcTemplate jdbc;
    private final Amostras amostras;

    public Comandos(JdbcTemplate jdbc, Amostras amostras) {
        this.jdbc = jdbc;
        this.amostras = amostras;
    }

    public record Comando(long id, String tipo, Integer valor, String estado) {}
    public record Entrega(Amostra amostraSelecionada, List<Comando> comandos) {}

    public Comando criar(String tipo, Integer valor) {
        long id = jdbc.queryForObject("SELECT NEXT VALUE FOR comando_id", Long.class);
        jdbc.update("INSERT INTO comando (id, tipo, valor, entregue) VALUES (?, ?, ?, FALSE)", id, tipo, valor);
        return new Comando(id, tipo, valor, "pendente");
    }

    public List<Comando> listar() {
        return jdbc.query("SELECT id, tipo, valor, entregue FROM comando ORDER BY id", (resultado, numeroLinha) ->
                new Comando(resultado.getLong(1), resultado.getString(2), resultado.getObject(3, Integer.class),
                        resultado.getBoolean(4) ? "entregue" : "pendente"));
    }

    @Transactional
    public Entrega entregar() {
        // O bloqueio também protege a seleção e impede que duas sincronizações entreguem o mesmo comando.
        jdbc.queryForObject("SELECT id FROM selecao_estacao WHERE id = 1 FOR UPDATE", Integer.class);
        Amostra amostraSelecionada = amostras.selecionada();
        // A resposta leva no máximo um comando para limitar a memória usada no ESP32.
        List<Comando> comandosEntregues = jdbc.query("SELECT id, tipo, valor FROM comando WHERE entregue = FALSE ORDER BY id LIMIT 1",
                (resultado, numeroLinha) -> new Comando(resultado.getLong(1), resultado.getString(2), resultado.getObject(3, Integer.class), "entregue"));
        // Entregue significa incluído na resposta, sem confirmação de execução.
        for (Comando comando : comandosEntregues) {
            jdbc.update("UPDATE comando SET entregue = TRUE WHERE id = ?", comando.id());
        }
        return new Entrega(amostraSelecionada, comandosEntregues);
    }
}
