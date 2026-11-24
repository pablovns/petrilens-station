package br.com.petrilens.seguranca;

import br.com.petrilens.erro.TratamentoErro.Erro;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableConfigurationProperties(Credenciais.class)
public class Seguranca {
    private final ObjectMapper mapper;

    public Seguranca(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Bean
    UserDetailsService operador(Credenciais credenciais) {
        var codificadorSenha = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername(credenciais.usuario())
                .password(codificadorSenha.encode(credenciais.senha())).roles("OPERADOR").build());
    }

    @Bean
    @Order(1)
    SecurityFilterChain estacao(HttpSecurity http, Credenciais credenciais) throws Exception {
        byte[] chaveEsperada = credenciais.chaveEstacao().getBytes(StandardCharsets.UTF_8);
        return http.securityMatcher("/api/estacao/**")
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                // O ESP32 envia X-Api-Key explicitamente, sem credenciais que o navegador anexe sozinho.
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest requisicao,
                            HttpServletResponse resposta, FilterChain cadeiaFiltros)
                            throws IOException, ServletException {
                        String chave = requisicao.getHeader("X-Api-Key");
                        if (chave == null || !MessageDigest.isEqual(chaveEsperada,
                                chave.getBytes(StandardCharsets.UTF_8))) {
                            responderErro(resposta, 401, "Chave da estação ausente ou inválida.");
                            return;
                        }
                        cadeiaFiltros.doFilter(requisicao, resposta);
                    }
                }, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .build();
    }

    @Bean
    SecurityFilterChain painel(HttpSecurity http) throws Exception {
        return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                // O navegador pode reenviar o login HTTP Basic sozinho, então as alterações exigem CSRF.
                .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository())
                        // Basic reautentica a cada chamada; as consultas periódicas não devem apagar o token CSRF.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .httpBasic(b -> b.authenticationEntryPoint((requisicao, resposta, excecao) -> {
                    resposta.setHeader("WWW-Authenticate", "Basic realm=\"PetriLens\", charset=\"UTF-8\"");
                    responderErro(resposta, 401, "Login do operador ausente ou inválido.");
                }))
                .exceptionHandling(e -> e.accessDeniedHandler((requisicao, resposta, excecao) ->
                        responderErro(resposta, 403, "Acesso recusado: confira a autorização e o token CSRF.")))
                .build();
    }

    private void responderErro(HttpServletResponse resposta, int status, String mensagem) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/json");
        resposta.setCharacterEncoding("UTF-8");
        mapper.writeValue(resposta.getOutputStream(), new Erro(mensagem));
    }
}
