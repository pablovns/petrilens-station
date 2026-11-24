package br.com.petrilens.amostra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Amostras {
    private final JdbcTemplate jdbc;

    public Amostras(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Amostra(String loteId, String amostraId) {}
    public record Lote(String loteId, List<Amostra> amostras) {}

    public List<Lote> listarLotes() {
        var lotes = new LinkedHashMap<String, List<Amostra>>();
        jdbc.query("SELECT lote_id, amostra_id FROM amostra ORDER BY lote_id, amostra_id",
                this::lerAmostra)
                .forEach(amostra -> lotes.computeIfAbsent(amostra.loteId(), loteId -> new ArrayList<>()).add(amostra));
        return lotes.entrySet().stream().map(lote -> new Lote(lote.getKey(), lote.getValue())).toList();
    }

    public Amostra selecionada() {
        return jdbc.queryForObject("""
                SELECT a.lote_id, a.amostra_id FROM amostra a
                JOIN selecao_estacao s ON s.amostra_id = a.amostra_id WHERE s.id = 1
                """, this::lerAmostra);
    }

    public Optional<Amostra> buscar(String amostraId) {
        return jdbc.query("SELECT lote_id, amostra_id FROM amostra WHERE amostra_id = ?",
                this::lerAmostra, amostraId).stream().findFirst();
    }

    public Optional<Amostra> selecionar(String amostraId) {
        Optional<Amostra> amostra = buscar(amostraId);
        amostra.ifPresent(encontrada -> jdbc.update(
                "UPDATE selecao_estacao SET amostra_id = ? WHERE id = 1", amostraId));
        return amostra;
    }

    private Amostra lerAmostra(ResultSet resultado, int numeroLinha) throws SQLException {
        return new Amostra(resultado.getString(1), resultado.getString(2));
    }
}
