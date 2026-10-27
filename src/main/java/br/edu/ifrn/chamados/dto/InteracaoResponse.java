package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Interacao;

import java.time.LocalDateTime;

public record InteracaoResponse(Long id, String texto, LocalDateTime dataHora, UsuarioResumo autor) {

    public static InteracaoResponse from(Interacao interacao) {
        return new InteracaoResponse(interacao.getId(), interacao.getTexto(), interacao.getDataHora(),
                UsuarioResumo.from(interacao.getAutor()));
    }
}
