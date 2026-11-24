package br.com.petrilens;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

class ComandoHttpTest {
    private static final String COMANDOS = "/api/painel/comandos";
    private static final String SINCRONIZACAO = "/api/estacao/sincronizacao";
    private static final String CHAVE = "chave-sintetica-comandos";
    private static final String BASIC = "Basic " + Base64.getEncoder().encodeToString(
            "operador-comandos:senha-sintetica".getBytes(StandardCharsets.UTF_8));
    private static final String ESTADO = """
            {"estacao_id":"est-01","presenca":true,"lux":123,"brilho":%d,
             "amostra_id":"A5","rssi":-55,"uptime_s":5}
            """;
    private static final String CAPTURAR = """
            {"tipo":"capturar"}
            """;

    @TempDir
    Path dados;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private int porta;
    private String cookie;
    private String csrfHeader;
    private String csrfToken;

    @Test
    void entregaUmaVezComASelecaoMesmoEmSincronizacoesConcorrentes() throws Exception {
        try (var contexto = iniciar()) {
            obterCsrf();
            var amostra = json(enviar(painel("PUT", "/api/painel/amostra-selecionada",
                    "{\"amostra_id\":\"A5\"}", true)), 200);
            var captura = post(CAPTURAR, 201);
            verificarComando(captura, "capturar", "null", "pendente");
            assertThat(listar()).containsExactly(captura);
            json(enviar(pedido("POST", SINCRONIZACAO, ESTADO.formatted(10))), 401);

            var primeira = http.sendAsync(sincronizacao(10).build(), HttpResponse.BodyHandlers.ofString());
            var segunda = http.sendAsync(sincronizacao(10).build(), HttpResponse.BodyHandlers.ofString());
            var id = captura.path("id");
            int entregas = 0;
            for (var futura : List.of(primeira, segunda)) {
                var resposta = json(futura.get(10, TimeUnit.SECONDS), 200);
                assertThat(resposta.path("amostra_selecionada")).isEqualTo(amostra);
                for (var comando : resposta.path("comandos")) {
                    verificarComando(comando, "capturar", "null", "entregue");
                    assertThat(comando.path("id")).isEqualTo(id);
                    entregas++;
                }
            }
            assertThat(entregas).isEqualTo(1);
            assertThat(sincronizar(10)).isEmpty();
            var entregue = listar().get(0);
            assertThat(entregue.path("id")).isEqualTo(id);
            verificarComando(entregue, "capturar", "null", "entregue");

            var status = post("{\"tipo\":\"status\",\"valor\":null}", 201);
            var brilho = post("{\"tipo\":\"ajustar_brilho\",\"valor\":0}", 201);
            verificarComando(status, "status", "null", "pendente");
            verificarComando(brilho, "ajustar_brilho", "0", "pendente");
            assertThat(listar()).containsExactly(entregue, status, brilho);
            var loteStatus = sincronizar(10);
            assertThat(loteStatus.size()).isEqualTo(1);
            verificarComando(loteStatus.get(0), "status", "null", "entregue");
            assertThat(loteStatus.get(0).path("id")).isEqualTo(status.path("id"));
            assertThat(listar().get(2)).isEqualTo(brilho);
            var loteBrilho = sincronizar(10);
            assertThat(loteBrilho.size()).isEqualTo(1);
            verificarComando(loteBrilho.get(0), "ajustar_brilho", "0", "entregue");
            assertThat(loteBrilho.get(0).path("id")).isEqualTo(brilho.path("id"));
            assertThat(sincronizar(0)).isEmpty();
            assertThat(listar()).containsExactly(entregue, loteStatus.get(0), loteBrilho.get(0));
            var estado = json(enviar(painel("GET", "/api/painel/estado", null, false)), 200);
            assertThat(estado.path("estado").path("brilho").asInt()).isZero();
        }
    }

    @Test
    void exigeBasicECsrfERecusaComandosInvalidosSemCoercao() throws Exception {
        try (var contexto = iniciar()) {
            json(enviar(pedido("GET", COMANDOS, null).header("X-Api-Key", CHAVE)), 401);
            json(enviar(painel("POST", COMANDOS, CAPTURAR, false)), 403);
            obterCsrf();
            json(enviar(pedido("POST", COMANDOS, CAPTURAR)
                    .header("Cookie", cookie).header(csrfHeader, csrfToken)), 401);
            json(enviar(painel("POST", COMANDOS, CAPTURAR, false)
                    .header(csrfHeader, "token-invalido")), 403);

            for (String corpo : List.of("{}", "null", "[]", "{", "{\"tipo\":false}",
                    "{\"tipo\":\"desconhecido\"}", "{\"tipo\":\"capturar\",\"valor\":0}",
                    "{\"tipo\":\"status\",\"valor\":true}", "{\"tipo\":\"ajustar_brilho\"}")) {
                assertThat(post(corpo, 400).path("mensagem").asText()).isNotBlank();
            }
            for (String valor : List.of("-1", "11", "4294967296", "4.0", "1e0", "\"4\"", "true", "null", "[]", "{}")) {
                assertThat(post("{\"tipo\":\"ajustar_brilho\",\"valor\":" + valor + "}", 400)
                        .path("mensagem").asText()).isNotBlank();
            }
            assertThat(listar()).isEmpty();
            verificarComando(post("{\"tipo\":\"ajustar_brilho\",\"valor\":10}", 201),
                    "ajustar_brilho", "10", "pendente");
        }
    }

    private ServletWebServerApplicationContext iniciar() {
        var contexto = (ServletWebServerApplicationContext) new SpringApplicationBuilder(ServidorApplication.class)
                .run("--server.port=0", "--server.address=127.0.0.1", "--spring.config.import=",
                        "--ESTACAO_API_KEY=" + CHAVE, "--PAINEL_USUARIO=operador-comandos",
                        "--PAINEL_SENHA=senha-sintetica",
                        "--spring.datasource.url=jdbc:h2:file:" + dados.resolve("petrilens"));
        porta = contexto.getWebServer().getPort();
        return contexto;
    }

    private void obterCsrf() throws Exception {
        var resposta = enviar(painel("GET", "/api/painel/csrf", null, false));
        var csrf = json(resposta, 200);
        csrfHeader = csrf.path("header_name").asText();
        csrfToken = csrf.path("token").asText();
        cookie = resposta.headers().allValues("set-cookie").stream()
                .filter(valor -> valor.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow().split(";", 2)[0];
    }

    private JsonNode listar() throws Exception {
        return json(enviar(painel("GET", COMANDOS, null, false)), 200);
    }

    private JsonNode post(String corpo, int status) throws Exception {
        return json(enviar(painel("POST", COMANDOS, corpo, true)), status);
    }

    private JsonNode sincronizar(int brilho) throws Exception {
        return json(enviar(sincronizacao(brilho)), 200).path("comandos");
    }

    private HttpRequest.Builder sincronizacao(int brilho) {
        return pedido("POST", SINCRONIZACAO, ESTADO.formatted(brilho)).header("X-Api-Key", CHAVE);
    }

    private HttpRequest.Builder painel(String metodo, String caminho, String corpo, boolean csrf) {
        var builder = pedido(metodo, caminho, corpo).header("Authorization", BASIC);
        if (cookie != null) builder.header("Cookie", cookie);
        if (csrf) builder.header(csrfHeader, csrfToken);
        return builder;
    }

    private HttpRequest.Builder pedido(String metodo, String caminho, String corpo) {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .timeout(Duration.ofSeconds(10));
        if (corpo != null) builder.header("Content-Type", "application/json");
        return builder.method(metodo, corpo == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
    }

    private HttpResponse<String> enviar(HttpRequest.Builder pedido) throws Exception {
        return http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> resposta, int status) throws Exception {
        assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(status);
        assertThat(resposta.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        return mapper.readTree(resposta.body());
    }

    private void verificarComando(JsonNode comando, String tipo, String valor, String estado) throws Exception {
        assertThat(comando.path("id").isIntegralNumber()).isTrue();
        assertThat(comando.path("tipo").asText()).isEqualTo(tipo);
        assertThat(comando.path("valor")).isEqualTo(mapper.readTree(valor));
        assertThat(comando.path("estado").asText()).isEqualTo(estado);
    }
}
