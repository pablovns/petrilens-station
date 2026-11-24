package br.com.petrilens.estacao;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EstadoInformado(
        @NotNull @Pattern(regexp = "est-01") String estacaoId,
        @NotNull Boolean presenca,
        @NotNull @DecimalMin("0") BigDecimal lux,
        @NotNull @Min(0) @Max(10) Integer brilho,
        @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String amostraId,
        @NotNull @Min(-127) @Max(0) Integer rssi,
        @NotNull @Min(0) Long uptimeS,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Pattern(regexp = "lux_fora_da_faixa") String motivoRecusa) {}
