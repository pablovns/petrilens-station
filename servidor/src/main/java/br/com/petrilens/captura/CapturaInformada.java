package br.com.petrilens.captura;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CapturaInformada(
        @NotNull @Pattern(regexp = "est-01") String estacaoId,
        @NotBlank @Size(max = 64) String loteId,
        @NotBlank @Size(max = 64) String amostraId,
        @NotNull @DecimalMin("0") Double lux,
        @NotNull @Pattern(regexp = "800x600") String resolucao,
        @NotNull @AssertTrue Boolean simulacao,
        @NotBlank @Size(min = 344, max = 344) String chaveCifrada,
        @NotBlank @Size(min = 16, max = 16) String iv,
        @NotBlank @Size(min = 28, max = 65536) String imagemCifrada) {}
