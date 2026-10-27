package br.edu.ifrn.chamados.dto;

import java.time.LocalDateTime;

public record ErroResponse(LocalDateTime timestamp, int status, String mensagem, String caminho) {
}
