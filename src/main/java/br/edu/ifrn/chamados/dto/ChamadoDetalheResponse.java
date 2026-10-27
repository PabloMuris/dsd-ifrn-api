package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Chamado;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

public record ChamadoDetalheResponse(@JsonUnwrapped ChamadoResponse chamado, List<InteracaoResponse> interacoes) {

    public static ChamadoDetalheResponse from(Chamado chamado) {
        return new ChamadoDetalheResponse(ChamadoResponse.from(chamado),
                chamado.getInteracoes().stream().map(InteracaoResponse::from).toList());
    }
}
