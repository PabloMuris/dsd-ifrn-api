package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Usuario;

public record UsuarioResumo(Long id, String nome) {

    public static UsuarioResumo from(Usuario usuario) {
        return usuario != null ? new UsuarioResumo(usuario.getId(), usuario.getNome()) : null;
    }
}
