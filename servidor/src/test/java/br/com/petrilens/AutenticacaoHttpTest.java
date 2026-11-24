package br.com.petrilens;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "ESTACAO_API_KEY=chave-sintetica-auth", "PAINEL_USUARIO=operador-auth",
        "PAINEL_SENHA=senha-sintetica-auth", "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:AutenticacaoHttpTest"})
class AutenticacaoHttpTest {
    private static final String CHAVE = "chave-sintetica-auth";
    private static final String BASIC = basic("operador-auth:senha-sintetica-auth");
    private static final String ESTADO = """
            {"estacao_id":"est-01","presenca":true,"lux":123,"brilho":10,
             "amostra_id":null,"rssi":-55,"uptime_s":5}
            """;
    @LocalServerPort
    int port;
    @Autowired
    ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void estacaoExigeChaveMesmoComBasicENaoAlteraEstadoAoRecusar() throws Exception {
        var antes = mapper.readTree(enviar("GET", "/api/painel/estado", "Authorization", BASIC).body());
        for (String[] headers : List.of(new String[0],
                new String[]{"X-Api-Key", "errada"},
                new String[]{"Api-Key", CHAVE},
                new String[]{"Authorization", BASIC},
                new String[]{"X-Api-Key", "errada", "Authorization", BASIC})) {
            var resposta = enviar("POST", "/api/estacao/sincronizacao", headers);
            erro(resposta, 401, false);
            assertThat(mapper.readTree(enviar("GET", "/api/painel/estado", "Authorization", BASIC).body()))
                    .isEqualTo(antes);
        }
        var aceita = enviar("POST", "/api/estacao/sincronizacao", "X-Api-Key", CHAVE);
        assertThat(aceita.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(aceita.body()).path("comandos").isArray()).isTrue();
        var estado = mapper.readTree(enviar("GET", "/api/painel/estado", "Authorization", BASIC).body());
        assertThat(estado.path("estado")).isEqualTo(mapper.readTree(ESTADO));
        assertThat(estado.path("online").asBoolean()).isTrue();
        semSessao(aceita);
    }

    @Test
    void basicProtegePainelAssetsEApiSemAceitarChaveDaEstacao() throws Exception {
        for (String caminho : List.of("/", "/index.html", "/painel.js", "/api/painel/estado", "/api/lotes", "/api/painel/csrf", "/api/operador/inexistente")) {
            for (String[] headers : List.of(new String[0],
                    new String[]{"Authorization", basic("operador-auth:errada")},
                    new String[]{"Authorization", basic("errado:senha-sintetica-auth")},
                    new String[]{"X-Api-Key", CHAVE},
                    new String[]{"X-Api-Key", CHAVE, "Authorization", basic("operador-auth:errada")})) {
                erro(enviar("GET", caminho, headers), 401, true);
            }
            if (!caminho.contains("inexistente")) {
                var aceita = enviar("GET", caminho, "Authorization", BASIC);
                assertThat(aceita.statusCode()).isEqualTo(200);
                semSessao(aceita);
                // Uma requisição autenticada não autoriza a próxima sem credenciais.
                erro(enviar("GET", caminho), 401, true);
            }
        }
    }

    @Test
    void requisicaoMutavelDoOperadorMantemProtecaoCsrf() throws Exception {
        erro(enviar("POST", "/api/painel/estado", "Authorization", BASIC), 403, false);
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String... headers) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + caminho));
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        builder.method(metodo, metodo.equals("POST")
                ? HttpRequest.BodyPublishers.ofString(ESTADO) : HttpRequest.BodyPublishers.noBody());
        if (metodo.equals("POST")) {
            builder.header("Content-Type", "application/json");
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void erro(HttpResponse<String> resposta, int status, boolean challenge) throws Exception {
        assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(status);
        assertThat(resposta.headers().firstValue("content-type").orElse(""))
                .startsWith("application/json");
        var json = mapper.readTree(resposta.body());
        assertThat(json.size()).isEqualTo(1);
        assertThat(json.path("mensagem").asText()).isNotBlank();
        assertThat(resposta.headers().firstValue("www-authenticate").isPresent()).isEqualTo(challenge);
        if (challenge) {
            assertThat(resposta.headers().firstValue("www-authenticate").orElseThrow()).startsWith("Basic ");
        }
        semSessao(resposta);
    }

    private void semSessao(HttpResponse<String> resposta) {
        assertThat(resposta.headers().allValues("set-cookie"))
                .noneMatch(cookie -> cookie.contains("JSESSIONID"));
    }

    private static String basic(String credenciais) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
    }
}
