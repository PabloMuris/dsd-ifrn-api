package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;

import java.time.LocalDateTime;

public record ChamadoResponse(
        Long id,
        String titulo,
        String descricao,
        String local,
        Prioridade prioridade,
        StatusChamado status,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        String solucao,
        CategoriaResponse categoria,
        UsuarioResumo solicitante,
        UsuarioResumo tecnico
) {
    public static ChamadoResponse from(Chamado chamado) {
        return new ChamadoResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(), chamado.getLocal(),
                chamado.getPrioridade(), chamado.getStatus(), chamado.getDataAbertura(), chamado.getDataFechamento(),
                chamado.getSolucao(), CategoriaResponse.from(chamado.getCategoria()),
                UsuarioResumo.from(chamado.getSolicitante()), UsuarioResumo.from(chamado.getTecnico()));
    }
}
