package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotNull;

public record AssumirChamadoRequest(@NotNull(message = "tecnicoId é obrigatório") Long tecnicoId) {
}
