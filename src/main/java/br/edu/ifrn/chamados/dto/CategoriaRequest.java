package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 100) String nome,
        @Size(max = 255) String descricao,
        @NotNull(message = "setorId é obrigatório") Long setorId
) {
}
