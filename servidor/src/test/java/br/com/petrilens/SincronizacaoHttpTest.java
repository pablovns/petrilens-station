package br.com.petrilens;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "ESTACAO_API_KEY=chave-sintetica-teste", "PAINEL_USUARIO=operador-teste",
        "PAINEL_SENHA=senha-sintetica-teste", "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:SincronizacaoHttpTest"})
@Import(SincronizacaoHttpTest.Configuracao.class)
class SincronizacaoHttpTest {
    private static final String ESTADO = """
            {"estacao_id":"est-01","presenca":true,"lux":542.5,"brilho":10,
             "amostra_id":null,"rssi":-55,"uptime_s":5}
            """;
    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private RelogioTeste clock;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void sincronizaConsultaExpiraEValidaPelaApiHttp() throws Exception {
        JsonNode inicial = json(enviar("GET", "/api/painel/estado", null), 200);
        assertThat(inicial.path("online").asBoolean()).isFalse();
        assertThat(inicial.path("estado").isNull()).isTrue();
        assertThat(inicial.path("ultima_sincronizacao").isNull()).isTrue();

        JsonNode resposta = json(enviar("POST", "/api/estacao/sincronizacao", ESTADO), 200);
        assertThat(resposta.path("amostra_selecionada").path("amostra_id").asText()).isEqualTo("A1");
        assertThat(resposta.path("comandos").isArray()).isTrue();
        assertThat(resposta.path("comandos")).isEmpty();
        JsonNode recebido = consultar();
        assertThat(recebido.path("online").asBoolean()).isTrue();
        assertThat(recebido.path("estado")).isEqualTo(mapper.readTree(ESTADO));
        assertThat(recebido.path("ultima_sincronizacao").asText())
                .isEqualTo("2026-11-17T18:00:00Z");

        clock.agora = clock.agora.plusSeconds(15);
        assertThat(consultar().path("online").asBoolean()).isTrue();
        clock.agora = clock.agora.plusMillis(1);
        assertThat(consultar().path("online").asBoolean()).isFalse();
        assertThat(consultar().path("estado")).isEqualTo(mapper.readTree(ESTADO));

        JsonNode offline = consultar();
        List<String> invalidos = List.of(
                ESTADO.replace("est-01", "est-02"),
                ESTADO.replace("\"presenca\":true", "\"presenca\":null"),
                ESTADO.replace("\"presenca\":true", "\"presenca\":\"true\""),
                ESTADO.replace("542.5", "-1"),
                ESTADO.replace("542.5", "\"NaN\""),
                ESTADO.replace("\"brilho\":10", "\"brilho\":11"),
                ESTADO.replace("\"brilho\":10", "\"brilho\":-1"),
                ESTADO.replace("\"brilho\":10", "\"brilho\":1.5"),
                ESTADO.replace("\"amostra_id\":null", "\"amostra_id\":\"\""),
                ESTADO.replace("\"amostra_id\":null", "\"amostra_id\":1"),
                ESTADO.replace("-55", "-128"),
                ESTADO.replace("-55", "1"),
                ESTADO.replace("\"uptime_s\":5", "\"uptime_s\":-1"),
                ESTADO.replace("\"uptime_s\":5", "\"uptime_s\":null"),
                ESTADO.replace("\"uptime_s\":5", "\"uptime_s\":9223372036854775808"),
                ESTADO.replace("\"lux\":542.5,", ""),
                ESTADO.replace("\"lux\":542.5", "\"lux\":\"542.5\""),
                ESTADO.replace("\"rssi\":-55", "\"rssi\":\"-55\""),
                ESTADO.replace("\"estacao_id\":\"est-01\"", "\"estacao_id\":null"),
                ESTADO.replace("\"brilho\":10", "\"brilho\":null"),
                ESTADO.replace("\"amostra_id\":null", "\"amostra_id\":\"" + "A".repeat(65) + "\""),
                ESTADO.replace("\"lux\":542.5", "\"extra\":1,\"lux\":542.5"),
                "{}", "{", "null");
        for (String invalido : invalidos) {
            JsonNode erro = json(enviar("POST", "/api/estacao/sincronizacao", invalido), 400);
            assertThat(erro.path("mensagem").asText()).isNotBlank();
            assertThat(erro.size()).isEqualTo(1);
            assertThat(consultar()).isEqualTo(offline);
        }

        String limites = ESTADO.replace("true", "false").replace("542.5", "0")
                .replace("\"brilho\":10", "\"brilho\":0").replace("-55", "-127")
                .replace("\"uptime_s\":5", "\"uptime_s\":0")
                .replace("\"amostra_id\":null", "\"amostra_id\":\"A1\"");
        json(enviar("POST", "/api/estacao/sincronizacao", limites), 200);
        assertThat(consultar().path("estado")).isEqualTo(mapper.readTree(limites));
        assertThat(consultar().path("online").asBoolean()).isTrue();
        assertThat(enviar("GET", "/", null).body()).contains("Estação PetriLens");
        assertThat(enviar("GET", "/painel.js", null).statusCode()).isEqualTo(200);

        String recusa = ESTADO.replace("\"uptime_s\":5",
                "\"uptime_s\":5,\"motivo_recusa\":\"lux_fora_da_faixa\"");
        json(enviar("POST", "/api/estacao/sincronizacao", recusa), 200);
        assertThat(consultar().path("estado")).isEqualTo(mapper.readTree(recusa));
        assertThat(json(enviar("GET", "/api/amostras/A1/capturas", null), 200)).isEmpty();

        for (String invalido : List.of(recusa.replace("lux_fora_da_faixa", "desconhecido"),
                recusa.replace("\"lux_fora_da_faixa\"", "42"))) {
            json(enviar("POST", "/api/estacao/sincronizacao", invalido), 400);
            assertThat(consultar().path("estado")).isEqualTo(mapper.readTree(recusa));
        }

        json(enviar("POST", "/api/estacao/sincronizacao", ESTADO.replace("-55", "0")), 200);
        assertThat(consultar().path("estado").has("motivo_recusa")).isFalse();
    }

    private JsonNode consultar() throws Exception {
        return json(enviar("GET", "/api/painel/estado", null), 200);
    }

    private JsonNode json(HttpResponse<String> resposta, int status) throws Exception {
        assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(status);
        assertThat(resposta.headers().firstValue("content-type").orElse(""))
                .startsWith("application/json");
        return mapper.readTree(resposta.body());
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + caminho));
        if (caminho.startsWith("/api/estacao/")) {
            builder.header("X-Api-Key", "chave-sintetica-teste");
        } else {
            builder.header("Authorization", "Basic " + java.util.Base64.getEncoder()
                    .encodeToString("operador-teste:senha-sintetica-teste".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        if (corpo == null) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(corpo));
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration
    static class Configuracao {
        @Bean
        @Primary
        RelogioTeste relogioTeste() {
            return new RelogioTeste();
        }
    }

    static class RelogioTeste extends Clock {
        volatile Instant agora = Instant.parse("2026-11-17T18:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(agora, zone);
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
