package br.com.petrilens.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class TratamentoErro extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TratamentoErro.class);

    public record Erro(String mensagem) {}

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException excecao, HttpHeaders cabecalhos,
            HttpStatusCode status, WebRequest requisicao) {
        return ResponseEntity.badRequest().body(new Erro(
                "Estado inválido: confira os campos e limites em docs/api.md."));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException excecao, HttpHeaders cabecalhos,
            HttpStatusCode status, WebRequest requisicao) {
        return ResponseEntity.badRequest().body(new Erro(
                "JSON inválido: confira os campos e tipos em docs/api.md."));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception excecao, Object corpo,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        return new ResponseEntity<>(new Erro("Requisição não atendida: " + status.value() + "."),
                cabecalhos, status);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Erro> inesperado(Exception excecao) {
        LOG.error("Falha ao atender requisição", excecao);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Erro("Não foi possível atender a requisição."));
    }
}
