package br.com.petrilens.seguranca;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    public record Token(String headerName, String token) {}

    @GetMapping("/api/painel/csrf")
    public Token consultar(CsrfToken csrf) {
        return new Token(csrf.getHeaderName(), csrf.getToken());
    }
}
