package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    @EntityGraph(attributePaths = {"solicitante", "tecnico", "categoria", "categoria.setor"})
    @Query("""
            select c from Chamado c
            where (:status is null or c.status = :status)
              and (:prioridade is null or c.prioridade = :prioridade)
              and (:setorId is null or c.categoria.setor.id = :setorId)
              and (:categoriaId is null or c.categoria.id = :categoriaId)
              and (:solicitanteId is null or c.solicitante.id = :solicitanteId)
              and (:tecnicoId is null or c.tecnico.id = :tecnicoId)
              and (cast(:dataInicio as LocalDateTime) is null or c.dataAbertura >= :dataInicio)
              and (cast(:dataFim as LocalDateTime) is null or c.dataAbertura < :dataFim)
            """)
    Page<Chamado> filtrar(@Param("status") StatusChamado status,
                          @Param("prioridade") Prioridade prioridade,
                          @Param("setorId") Long setorId,
                          @Param("categoriaId") Long categoriaId,
                          @Param("solicitanteId") Long solicitanteId,
                          @Param("tecnicoId") Long tecnicoId,
                          @Param("dataInicio") LocalDateTime dataInicio,
                          @Param("dataFim") LocalDateTime dataFim,
                          Pageable pageable);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCategoriaSetorId(Long setorId);
}
