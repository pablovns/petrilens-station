package br.com.petrilens.captura;

import br.com.petrilens.erro.TratamentoErro.Erro;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CapturaController {
    private static final int LIMITE_CORPO_BYTES = 65536;
    private final Capturas capturas;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final Clock clock;

    public CapturaController(Capturas capturas, ObjectMapper mapper, Validator validator, Clock clock) {
        this.capturas = capturas;
        this.mapper = mapper;
        this.validator = validator;
        this.clock = clock;
    }

    @PostMapping(value = "/api/estacao/capturas", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Capturas.Recebida> receber(HttpServletRequest requisicao) throws IOException {
        var recebidaEm = clock.instant();
        byte[] corpo = lerCorpoLimitado(requisicao);
        CapturaInformada captura = interpretarCaptura(corpo);
        return ResponseEntity.status(HttpStatus.CREATED).body(capturas.salvar(captura, recebidaEm));
    }

    private byte[] lerCorpoLimitado(HttpServletRequest requisicao) throws IOException {
        if (requisicao.getContentLengthLong() > LIMITE_CORPO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        // O byte extra detecta excesso mesmo sem Content-Length, inclusive em chunked.
        byte[] corpo = requisicao.getInputStream().readNBytes(LIMITE_CORPO_BYTES + 1);
        if (corpo.length > LIMITE_CORPO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        return corpo;
    }

    private CapturaInformada interpretarCaptura(byte[] corpo) throws IOException {
        CapturaInformada captura;
        try {
            captura = mapper.readerFor(CapturaInformada.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(corpo);
        } catch (JsonProcessingException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (captura == null || !validator.validate(captura).isEmpty()
                || !Double.isFinite(captura.lux())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return captura;
    }

    @ExceptionHandler(ImagemCifrada.ChaveIndisponivel.class)
    ResponseEntity<Erro> chaveIndisponivel() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new Erro("Chave privada de captura indisponível no servidor."));
    }

    @GetMapping("/api/amostras/{amostraId}/capturas")
    public List<Capturas.Resumo> listar(@PathVariable String amostraId) {
        return capturas.listar(amostraId);
    }

    @GetMapping(value = "/api/capturas/{id}/imagem", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] imagem(@PathVariable long id) throws IOException {
        return capturas.imagem(id);
    }
}
