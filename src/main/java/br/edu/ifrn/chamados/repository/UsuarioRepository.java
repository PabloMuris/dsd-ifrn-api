package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "setor")
    @Query("""
            select u from Usuario u
            where lower(u.nome) like lower(concat('%', :nome, '%'))
              and (:tipo is null or u.tipo = :tipo)
              and (:setorId is null or u.setor.id = :setorId)
            """)
    Page<Usuario> filtrar(@Param("nome") String nome,
                          @Param("tipo") TipoUsuario tipo,
                          @Param("setorId") Long setorId,
                          Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsBySetorId(Long setorId);
}
