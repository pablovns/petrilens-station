package br.com.petrilens.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("petrilens.seguranca")
public class Credenciais {
    private final String chaveEstacao;
    private final String usuario;
    private final String senha;

    public Credenciais(String chaveEstacao, String usuario, String senha) {
        // Não incluir valores em erros de binding/validação de credenciais.
        if (chaveEstacao == null || chaveEstacao.isBlank()
                || usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Configure ESTACAO_API_KEY, PAINEL_USUARIO e PAINEL_SENHA não vazios.");
        }
        this.chaveEstacao = chaveEstacao;
        this.usuario = usuario;
        this.senha = senha;
    }

    public String chaveEstacao() {
        return chaveEstacao;
    }

    public String usuario() {
        return usuario;
    }

    public String senha() {
        return senha;
    }
}
