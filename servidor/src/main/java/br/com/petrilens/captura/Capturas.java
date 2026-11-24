package br.com.petrilens.captura;

import br.com.petrilens.amostra.Amostras;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class Capturas {
    private final JdbcTemplate jdbc;
    private final Amostras amostras;
    private final ImagemCifrada imagemCifrada;
    private final Path diretorio;

    public Capturas(JdbcTemplate jdbc, Amostras amostras, ImagemCifrada imagemCifrada,
            @Value("${petrilens.capturas.diretorio:dados/capturas}") String diretorio) {
        this.jdbc = jdbc;
        this.amostras = amostras;
        this.imagemCifrada = imagemCifrada;
        this.diretorio = Path.of(diretorio);
    }

    public record Recebida(long id, Instant recebidaEm) {}
    public record Resumo(long id, Instant recebidaEm, double lux) {}

    public Recebida salvar(CapturaInformada captura, Instant recebidaEm) throws IOException {
        if (amostras.buscar(captura.amostraId())
                .filter(amostra -> amostra.loteId().equals(captura.loteId())).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        byte[] jpeg = imagemCifrada.decifrar(captura);
        validarJpeg(jpeg);
        long id = jdbc.queryForObject("SELECT NEXT VALUE FOR captura_id", Long.class);
        Files.createDirectories(diretorio);
        Path arquivo = caminhoImagem(id);
        boolean arquivoCriado = false;
        try {
            try (var saida = Files.newOutputStream(arquivo, StandardOpenOption.CREATE_NEW)) {
                arquivoCriado = true;
                saida.write(jpeg);
            }
            // Grava o registro no H2 só depois de fechar o arquivo, para evitar consultas sem imagem.
            registrarCaptura(id, captura, recebidaEm);
        } catch (IOException | RuntimeException erro) {
            // Remove o arquivo desta tentativa se a gravação ou o registro falhar.
            if (arquivoCriado) {
                removerArquivoIncompleto(arquivo, erro);
            }
            throw erro;
        }
        return new Recebida(id, recebidaEm);
    }

    private void registrarCaptura(long id, CapturaInformada captura, Instant recebidaEm) {
        jdbc.update("""
                INSERT INTO captura (id, estacao_id, lote_id, amostra_id, lux,
                    resolucao, simulacao, recebida_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, captura.estacaoId(), captura.loteId(), captura.amostraId(),
                captura.lux(), captura.resolucao(), captura.simulacao(), recebidaEm);
    }

    private void removerArquivoIncompleto(Path arquivo, Exception erroOriginal) {
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException erroLimpeza) {
            erroOriginal.addSuppressed(erroLimpeza);
        }
    }

    public List<Resumo> listar(String amostraId) {
        if (amostras.buscar(amostraId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return jdbc.query("""
                SELECT id, recebida_em, lux FROM captura WHERE amostra_id = ?
                ORDER BY recebida_em DESC, id DESC
                """, (resultado, numeroLinha) -> new Resumo(resultado.getLong(1),
                        resultado.getObject(2, OffsetDateTime.class).toInstant(), resultado.getDouble(3)), amostraId);
    }

    public byte[] imagem(long id) throws IOException {
        if (id <= 0 || jdbc.queryForObject("SELECT COUNT(*) FROM captura WHERE id = ?",
                Integer.class, id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Files.readAllBytes(caminhoImagem(id));
    }

    private Path caminhoImagem(long id) {
        return diretorio.resolve(id + ".jpg");
    }

    private void validarJpeg(byte[] jpeg) {
        try {
            if (jpeg.length < 4 || jpeg[0] != (byte) 0xff || jpeg[1] != (byte) 0xd8
                    || jpeg[jpeg.length - 2] != (byte) 0xff || jpeg[jpeg.length - 1] != (byte) 0xd9) {
                throw new IllegalArgumentException();
            }
            try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(jpeg))) {
                var leitores = ImageIO.getImageReaders(entrada);
                if (!leitores.hasNext()) {
                    throw new IllegalArgumentException();
                }
                var leitor = leitores.next();
                try {
                    leitor.setInput(entrada);
                    // Exige JPEG de 800 × 600 e confere o tamanho antes de decodificar os pixels.
                    if (!leitor.getFormatName().equalsIgnoreCase("JPEG")
                            || leitor.getWidth(0) != 800 || leitor.getHeight(0) != 600
                            || leitor.read(0) == null) {
                        throw new IllegalArgumentException();
                    }
                } finally {
                    leitor.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
