package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetorRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 100) String nome,
        @NotBlank(message = "sigla é obrigatória") @Size(max = 20) String sigla
) {
}
