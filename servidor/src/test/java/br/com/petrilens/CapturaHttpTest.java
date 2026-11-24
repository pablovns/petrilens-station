package br.com.petrilens;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.petrilens.captura.DelimitadorPemPkcs8;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

class CapturaHttpTest {
    @TempDir
    Path dados;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private static final String CHAVE = "chave-sintetica-captura";
    private static final String BASIC = "Basic " + Base64.getEncoder().encodeToString(
            "operador-captura:senha-sintetica".getBytes(StandardCharsets.UTF_8));
    private static final Path JPEG = Path.of("../exemplos/placa-ficticia.jpg");
    private static final List<String> CAMPOS_CIFRADOS = List.of("chave_cifrada", "iv", "imagem_cifrada");
    private final SecureRandom aleatorio = new SecureRandom();
    private KeyPair par;
    private int port;

    @BeforeEach
    void gerarChaveEfemera() throws Exception {
        par = gerarPar();
        Files.writeString(dados.resolve("privada.pem"), DelimitadorPemPkcs8.BEGIN.texto() + "\n"
                + Base64.getMimeEncoder().encodeToString(par.getPrivate().getEncoded())
                + "\n" + DelimitadorPemPkcs8.END.texto() + "\n");
    }

    private KeyPair gerarPar() throws Exception {
        var gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048, aleatorio);
        return gerador.generateKeyPair();
    }

    @Test
    void recebeNaAmostraInformadaProtegeConsultasEPersisteBytesAposReinicio() throws Exception {
        long id;
        Instant antes = Instant.now();
        byte[] jpeg = Files.readAllBytes(JPEG);
        try (var contexto = iniciar(dados.resolve("imagens"))) {
            port = contexto.getWebServer().getPort();
            var csrf = enviar("GET", "/api/painel/csrf", null, "Authorization", BASIC);
            var token = json(csrf, 200);
            String cookie = csrf.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
            json(enviar("PUT", "/api/painel/amostra-selecionada", "{\"amostra_id\":\"A5\"}",
                    "Authorization", BASIC, "Cookie", cookie,
                    token.path("header_name").asText(), token.path("token").asText()), 200);
            var recebida = json(capturar(valida().toString()), 201);
            id = recebida.path("id").asLong();
            assertThat(id).isPositive();
            assertThat(recebida.size()).isEqualTo(2);
            assertThat(Instant.parse(recebida.path("recebida_em").asText())).isBetween(antes, Instant.now());
            var lista = lista("A1");
            assertThat(lista.size()).isEqualTo(1);
            assertThat(lista.get(0).size()).isEqualTo(3);
            assertThat(lista.get(0).path("id").asLong()).isEqualTo(id);
            assertThat(lista.get(0).path("lux").asDouble()).isEqualTo(542);
            assertThat(lista.get(0).path("recebida_em")).isEqualTo(recebida.path("recebida_em"));
            assertThat(lista("A5")).isEmpty();
            conferirImagem(id, jpeg);
            for (String rota : List.of("/api/amostras/A1/capturas", "/api/capturas/" + id + "/imagem")) {
                json(enviar("GET", rota, null), 401);
                json(enviar("GET", rota, null, "X-Api-Key", CHAVE), 401);
                json(enviar("GET", rota, null, "Authorization", "Basic aW52YWxpZG86aW52YWxpZG8="), 401);
            }
            json(enviar("POST", "/api/estacao/capturas", valida().toString(), "Authorization", BASIC), 401);
            json(enviar("POST", "/api/estacao/capturas", valida().toString(), "X-Api-Key", "invalida"), 401);
            json(enviar("GET", "/api/amostras/inexistente/capturas", null, "Authorization", BASIC), 404);
            json(enviar("GET", "/api/capturas/999999/imagem", null, "Authorization", BASIC), 404);
            json(enviar("GET", "/api/capturas/abc/imagem", null, "Authorization", BASIC), 400);
            json(enviar("GET", "/api/capturas/-1/imagem", null, "Authorization", BASIC), 404);
        }
        try (var contexto = iniciar(dados.resolve("imagens"))) {
            port = contexto.getWebServer().getPort();
            assertThat(lista("A1").get(0).path("id").asLong()).isEqualTo(id);
            assertThat(lista("A5")).isEmpty();
            conferirImagem(id, jpeg);
            var estado = json(enviar("GET", "/api/painel/estado", null, "Authorization", BASIC), 200);
            assertThat(estado.path("amostra_selecionada").path("amostra_id").asText()).isEqualTo("A5");
            long proximoId = json(capturar(valida().toString()), 201).path("id").asLong();
            assertThat(proximoId).isGreaterThan(id);
            conferirImagem(proximoId, jpeg);
        }
    }

    @Test
    void recusaCamposTiposBase64JpegEAmostraSemRegistrarCapturas() throws Exception {
        try (var contexto = iniciar(dados.resolve("imagens"))) {
            port = contexto.getWebServer().getPort();
            ObjectNode valida = valida();
            var campos = new java.util.ArrayList<>(List.of(
                    "estacao_id", "lote_id", "amostra_id", "lux", "resolucao", "simulacao"));
            campos.addAll(CAMPOS_CIFRADOS);
            for (String campo : campos) {
                ObjectNode ausente = valida.deepCopy();
                ausente.remove(campo);
                json(capturar(ausente.toString()), 400);
                ObjectNode nulo = valida.deepCopy();
                nulo.putNull(campo);
                json(capturar(nulo.toString()), 400);
                ObjectNode tipoErrado = valida.deepCopy();
                tipoErrado.putArray(campo).add(1);
                json(capturar(tipoErrado.toString()), 400);
            }
            for (String[] invalido : new String[][]{
                    {"estacao_id", "est-02"}, {"lote_id", "L2026-002"}, {"lote_id", "inexistente"},
                    {"amostra_id", "inexistente"}, {"amostra_id", ""}, {"resolucao", "640x480"}}) {
                json(capturar(valida.deepCopy().put(invalido[0], invalido[1]).toString()), 400);
            }
            for (String campo : CAMPOS_CIFRADOS) {
                String valor = valida.path(campo).asText();
                for (String invalido : List.of("", "%" + valor.substring(1), valor + "\n")) {
                    json(capturar(valida.deepCopy().put(campo, invalido).toString()), 400);
                }
                json(capturar(valida.deepCopy().put(campo, 123).toString()), 400);
                json(capturar(valida.deepCopy().put(campo, true).toString()), 400);
                var objeto = valida.deepCopy();
                objeto.putObject(campo);
                json(capturar(objeto.toString()), 400);
            }
            json(capturar(valida.deepCopy().put("chave_cifrada", base64(new byte[257])).toString()), 400);
            json(capturar(valida.deepCopy().put("iv", base64(new byte[11])).toString()), 400);
            json(capturar(valida.deepCopy().put("imagem_cifrada", base64(new byte[16])).toString()), 400);
            ObjectNode antigo = valida.deepCopy();
            antigo.remove(CAMPOS_CIFRADOS);
            antigo.put("imagem_base64", base64(Files.readAllBytes(JPEG)));
            json(capturar(antigo.toString()), 400);
            for (String texto : List.of("{}", "null", "{", "[]", valida + "{}",
                    valida.deepCopy().put("lux", -1).toString(),
                    valida.deepCopy().put("lux", "542").toString(),
                    valida.deepCopy().put("simulacao", false).toString(),
                    valida.deepCopy().put("simulacao", "true").toString(),
                    valida.deepCopy().put("amostra_id", 1).toString(),
                    valida.deepCopy().put("extra", 1).toString(),
                    valida.toString().replace("\"lux\":542", "\"lux\":1e999"))) {
                json(capturar(texto), 400);
            }
            var imagem = new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (String formato : List.of("png", "jpeg")) {
                var bytes = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(imagem, formato, bytes);
                json(capturar(cifrar(bytes.toByteArray(), par.getPublic()).toString()), 400);
            }
            byte[] jpeg = Files.readAllBytes(JPEG);
            json(capturar(cifrar(Arrays.copyOf(jpeg, jpeg.length / 2), par.getPublic()).toString()), 400);
            assertThat(lista("A1")).isEmpty();
            json(capturar(valida.toString()), 201);
            assertThat(lista("A1").size()).isEqualTo(1);
        }
    }

    @Test
    void limitaCorpoInteiroComContentLengthEChunkedAntesDeDecodificar() throws Exception {
        try (var contexto = iniciar(dados.resolve("imagens"))) {
            port = contexto.getWebServer().getPort();
            String valida = valida().toString();
            assertThat(valida.getBytes(StandardCharsets.UTF_8).length).isLessThan(65536);
            String limite = valida + " ".repeat(65536 - valida.getBytes(StandardCharsets.UTF_8).length);
            json(capturar(limite + " "), 413);
            byte[] excesso = (limite + " ").getBytes(StandardCharsets.UTF_8);
            var pedido = HttpRequest.newBuilder(uri("/api/estacao/capturas"))
                    .header("X-Api-Key", CHAVE).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(excesso))).build();
            assertThat(pedido.bodyPublisher().orElseThrow().contentLength()).isEqualTo(-1);
            json(http.send(pedido, HttpResponse.BodyHandlers.ofByteArray()), 413);
            assertThat(lista("A1")).isEmpty();
            json(capturar(limite), 201);
            assertThat(lista("A1").size()).isEqualTo(1);
        }
    }

    @Test
    void falhaDeDiscoNaoRegistraCapturaNemExpoeCaminho() throws Exception {
        Path arquivo = dados.resolve("nao-e-diretorio");
        Files.writeString(arquivo, "fixture sintetica");
        try (var contexto = iniciar(arquivo)) {
            port = contexto.getWebServer().getPort();
            var erro = json(capturar(valida().toString()), 500);
            assertThat(erro.size()).isEqualTo(1);
            assertThat(erro.path("mensagem").asText()).doesNotContain(dados.toString(), "Exception");
            assertThat(lista("A1")).isEmpty();
        }
    }

    @Test
    void recusaAdulteracaoEChaveErradaComMesmoErroESemRegistro() throws Exception {
        try (var contexto = iniciar(dados.resolve("imagens"))) {
            port = contexto.getWebServer().getPort();
            ObjectNode valida = valida();
            JsonNode erro = null;
            for (String campo : CAMPOS_CIFRADOS) {
                byte[] bytes = Base64.getDecoder().decode(valida.path(campo).asText());
                bytes[bytes.length - 1] ^= 1;
                var atual = json(capturar(valida.deepCopy().put(campo, base64(bytes)).toString()), 400);
                assertThat(atual.path("mensagem").isTextual()).isTrue();
                if (erro != null) assertThat(atual).isEqualTo(erro);
                erro = atual;
            }
            var errada = cifrar(Files.readAllBytes(JPEG), gerarPar().getPublic());
            assertThat(json(capturar(errada.toString()), 400)).isEqualTo(erro);
            assertThat(lista("A1")).isEmpty();
            json(enviar("GET", "/api/capturas/1/imagem", null, "Authorization", BASIC), 404);
        }
    }

    @Test
    void chavePrivadaAusenteBloqueiaSomenteUpload() throws Exception {
        try (var contexto = iniciar(dados.resolve("imagens"), "")) {
            port = contexto.getWebServer().getPort();
            var erro = json(capturar(valida().toString()), 503);
            assertThat(erro.path("mensagem").asText()).contains("Chave privada de captura");
            assertThat(lista("A1")).isEmpty();
            json(enviar("GET", "/api/painel/estado", null, "Authorization", BASIC), 200);
            var estado = Files.readString(Path.of("../exemplos/sincronizacao.json"));
            json(enviar("POST", "/api/estacao/sincronizacao", estado, "X-Api-Key", CHAVE), 200);
        }
    }

    private ServletWebServerApplicationContext iniciar(Path imagens) {
        return iniciar(imagens, dados.resolve("privada.pem").toString());
    }

    private ServletWebServerApplicationContext iniciar(Path imagens, String caminhoChave) {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(ServidorApplication.class)
                .run("--server.port=0", "--spring.config.import=", "--ESTACAO_API_KEY=" + CHAVE,
                        "--PAINEL_USUARIO=operador-captura", "--PAINEL_SENHA=senha-sintetica",
                        "--spring.datasource.url=jdbc:h2:file:" + dados.resolve("petrilens"),
                        "--CAPTURA_CHAVE_PRIVADA=" + caminhoChave, "--petrilens.capturas.diretorio=" + imagens);
    }

    private ObjectNode valida() throws Exception {
        return cifrar(Files.readAllBytes(JPEG), par.getPublic());
    }

    private ObjectNode cifrar(byte[] jpeg, PublicKey publica) throws Exception {
        byte[] chave = new byte[16];
        byte[] iv = new byte[12];
        aleatorio.nextBytes(chave);
        aleatorio.nextBytes(iv);
        var rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, publica, new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT), aleatorio);
        var gcm = Cipher.getInstance("AES/GCM/NoPadding");
        gcm.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chave, "AES"), new GCMParameterSpec(128, iv));
        return ((ObjectNode) mapper.readTree(Files.readString(Path.of("../exemplos/captura.json"))))
                .put("chave_cifrada", base64(rsa.doFinal(chave)))
                .put("iv", base64(iv))
                .put("imagem_cifrada", base64(gcm.doFinal(jpeg)));
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private JsonNode lista(String amostra) throws Exception {
        return json(enviar("GET", "/api/amostras/" + amostra + "/capturas", null, "Authorization", BASIC), 200);
    }

    private void conferirImagem(long id, byte[] jpeg) throws Exception {
        var imagem = enviar("GET", "/api/capturas/" + id + "/imagem", null, "Authorization", BASIC);
        assertThat(imagem.statusCode()).isEqualTo(200);
        assertThat(imagem.headers().firstValue("content-type").orElseThrow()).isEqualTo("image/jpeg");
        assertThat(imagem.body()).isEqualTo(jpeg);
    }

    private HttpResponse<byte[]> capturar(String corpo) throws Exception {
        return enviar("POST", "/api/estacao/capturas", corpo, "X-Api-Key", CHAVE);
    }

    private URI uri(String caminho) {
        return URI.create("http://127.0.0.1:" + port + caminho);
    }

    private HttpResponse<byte[]> enviar(String metodo, String caminho, String corpo, String... headers) throws Exception {
        var pedido = HttpRequest.newBuilder(uri(caminho));
        if (headers.length > 0) pedido.headers(headers);
        if (corpo != null) pedido.header("Content-Type", "application/json");
        pedido.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        return http.send(pedido.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private JsonNode json(HttpResponse<byte[]> resposta, int status) throws Exception {
        assertThat(resposta.statusCode()).as(new String(resposta.body(), StandardCharsets.UTF_8)).isEqualTo(status);
        assertThat(resposta.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        return mapper.readTree(resposta.body());
    }
}
