package br.com.petrilens;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServidorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServidorApplication.class, args);
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer tiposJsonEstritos() {
        // Jackson ainda converte números em texto com ALLOW_COERCION_OF_SCALARS desabilitado.
        return builder -> builder.postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
