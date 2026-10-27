package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // "nome" chega como "" quando não informado, o que casa com qualquer valor
    @EntityGraph(attributePaths = "setor")
    @Query("""
            select c from Categoria c
            where lower(c.nome) like lower(concat('%', :nome, '%'))
              and (:setorId is null or c.setor.id = :setorId)
            """)
    Page<Categoria> filtrar(@Param("nome") String nome,
                            @Param("setorId") Long setorId,
                            Pageable pageable);

    List<Categoria> findBySetorId(Long setorId);
}
