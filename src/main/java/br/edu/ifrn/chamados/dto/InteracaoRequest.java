package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InteracaoRequest(
        @NotNull(message = "autorId é obrigatório") Long autorId,
        @NotBlank(message = "texto é obrigatório") String texto
) {
}
