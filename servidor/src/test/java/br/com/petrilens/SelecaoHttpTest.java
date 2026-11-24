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
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

class SelecaoHttpTest {
    @TempDir
    Path dados;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private static final String BASIC = "Basic " + Base64.getEncoder().encodeToString(
            "operador-selecao:senha-sintetica".getBytes(StandardCharsets.UTF_8));
    private int port;
    private String header;
    private String token;
    private String cookie;

    @Test
    void selecionaPropagaValidaCsrfEPersisteAposReinicioReal() throws Exception {
        try (var contexto = iniciar()) {
            port = contexto.getWebServer().getPort();
            var lotes = enviar("GET", "/api/lotes", null, false, 200);
            assertThat(lotes.size()).isEqualTo(2);
            assertThat(lotes.get(0).path("lote_id").asText()).isEqualTo("L2026-001");
            assertThat(lotes.get(1).path("lote_id").asText()).isEqualTo("L2026-002");
            for (int lote = 0; lote < 2; lote++) {
                assertThat(lotes.get(lote).path("amostras").size()).isEqualTo(3);
                for (int amostra = 0; amostra < 3; amostra++) {
                    assertThat(lotes.get(lote).path("amostras").get(amostra).path("amostra_id").asText())
                            .isEqualTo("A" + (lote * 3 + amostra + 1));
                }
            }
            assertThat(estado().path("amostra_selecionada")).isEqualTo(amostra("L2026-001", "A1"));
            enviar("PUT", "/api/painel/amostra-selecionada", "{\"amostra_id\":\"A5\"}", false, 403);
            obterToken();
            enviar("PUT", "/api/painel/amostra-selecionada", "{\"amostra_id\":\"A5\"}", false, 403);
            assertThat(estado().path("amostra_selecionada")).isEqualTo(amostra("L2026-001", "A1"));
            String valido = token;
            token = "token-invalido";
            enviar("PUT", "/api/painel/amostra-selecionada", "{\"amostra_id\":\"A5\"}", true, 403);
            token = valido;
            var escolhida = amostra("L2026-002", "A5");
            assertThat(enviar("PUT", "/api/painel/amostra-selecionada", "{\"amostra_id\":\"A5\"}", true, 200))
                    .isEqualTo(escolhida);
            for (String invalida : new String[]{"{\"amostra_id\":\"inexistente\"}", "{}",
                    "{\"amostra_id\":null}", "{\"amostra_id\":1}", "{\"amostra_id\":\"\"}",
                    "{\"amostra_id\":\"A2\",\"extra\":1}", "null", "{"}) {
                assertThat(enviar("PUT", "/api/painel/amostra-selecionada", invalida, true, 400)
                        .path("mensagem").asText()).isNotBlank();
                assertThat(estado().path("amostra_selecionada")).isEqualTo(escolhida);
            }
            assertThat(sincronizar(null).path("amostra_selecionada")).isEqualTo(escolhida);
            assertThat(estado().path("estado").path("amostra_id").isNull()).isTrue();
            assertThat(sincronizar("A5").path("amostra_selecionada")).isEqualTo(escolhida);
            assertThat(estado().path("estado").path("amostra_id").asText()).isEqualTo("A5");
            assertThat(estado().path("amostra_selecionada")).isEqualTo(escolhida);
        }
        // Fecha o servidor e o pool H2 antes de abrir outro contexto no mesmo arquivo.
        try (var contexto = iniciar()) {
            port = contexto.getWebServer().getPort();
            assertThat(estado().path("amostra_selecionada")).isEqualTo(amostra("L2026-002", "A5"));
            assertThat(estado().path("estado").isNull()).isTrue();
            assertThat(sincronizar(null).path("amostra_selecionada")).isEqualTo(amostra("L2026-002", "A5"));
            assertThat(enviar("GET", "/api/lotes", null, false, 200).size()).isEqualTo(2);
            obterToken();
            assertThat(enviar("PUT", "/api/painel/amostra-selecionada", "{\"amostra_id\":\"A2\"}", true, 200))
                    .isEqualTo(amostra("L2026-001", "A2"));
        }
    }

    private ServletWebServerApplicationContext iniciar() {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(ServidorApplication.class)
                .run("--server.port=0", "--spring.config.import=", "--ESTACAO_API_KEY=chave-sintetica",
                        "--PAINEL_USUARIO=operador-selecao", "--PAINEL_SENHA=senha-sintetica",
                        "--spring.datasource.url=jdbc:h2:file:" + dados.resolve("petrilens"));
    }

    private void obterToken() throws Exception {
        var csrf = enviar("GET", "/api/painel/csrf", null, false, 200);
        header = csrf.path("header_name").asText();
        token = csrf.path("token").asText();
        assertThat(header).isEqualTo("X-XSRF-TOKEN");
        assertThat(token).isNotBlank();
    }

    private JsonNode amostra(String lote, String id) throws Exception {
        return mapper.readTree("{\"lote_id\":\"" + lote + "\",\"amostra_id\":\"" + id + "\"}");
    }

    private JsonNode estado() throws Exception {
        return enviar("GET", "/api/painel/estado", null, false, 200);
    }

    private JsonNode sincronizar(String id) throws Exception {
        return enviar("POST", "/api/estacao/sincronizacao", """
                {"estacao_id":"est-01","presenca":true,"lux":123,"brilho":10,
                "rssi":-55,"uptime_s":5,"amostra_id":%s}
                """.formatted(id == null ? "null" : "\"" + id + "\""), false, 200);
    }

    private JsonNode enviar(String metodo, String caminho, String corpo, boolean csrf, int status) throws Exception {
        var pedido = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + caminho));
        if (caminho.startsWith("/api/estacao/")) pedido.header("X-Api-Key", "chave-sintetica");
        else pedido.header("Authorization", BASIC);
        if (csrf) pedido.header(header, token);
        if (cookie != null) pedido.header("Cookie", cookie);
        if (corpo != null) pedido.header("Content-Type", "application/json");
        pedido.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        var resposta = http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
        for (String valor : resposta.headers().allValues("set-cookie")) {
            if (valor.startsWith("XSRF-TOKEN=")) cookie = valor.split(";", 2)[0];
        }
        assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(status);
        assertThat(resposta.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        assertThat(resposta.headers().allValues("set-cookie")).noneMatch(c -> c.contains("JSESSIONID"));
        return mapper.readTree(resposta.body());
    }
}
