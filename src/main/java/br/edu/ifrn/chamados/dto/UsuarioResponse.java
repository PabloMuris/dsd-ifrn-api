package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String matricula, TipoUsuario tipo,
                              SetorResponse setor) {

    public static UsuarioResponse from(Usuario usuario) {
        SetorResponse setor = usuario.getSetor() != null ? SetorResponse.from(usuario.getSetor()) : null;
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getMatricula(),
                usuario.getTipo(), setor);
    }
}
