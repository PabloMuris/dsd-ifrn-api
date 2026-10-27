package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolverChamadoRequest(
        @NotNull(message = "tecnicoId é obrigatório") Long tecnicoId,
        @NotBlank(message = "solução é obrigatória") String solucao
) {
}
