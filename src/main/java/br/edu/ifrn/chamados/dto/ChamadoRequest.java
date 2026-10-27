package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
        @NotBlank(message = "título é obrigatório") @Size(max = 150) String titulo,
        @NotBlank(message = "descrição é obrigatória") String descricao,
        @NotBlank(message = "local é obrigatório") @Size(max = 150) String local,
        Prioridade prioridade,
        @NotNull(message = "categoriaId é obrigatório") Long categoriaId,
        @NotNull(message = "solicitanteId é obrigatório") Long solicitanteId
) {
}
