package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotNull;

public record CancelarChamadoRequest(@NotNull(message = "solicitanteId é obrigatório") Long solicitanteId) {
}
