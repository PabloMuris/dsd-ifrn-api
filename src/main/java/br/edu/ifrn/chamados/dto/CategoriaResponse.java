package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Categoria;

public record CategoriaResponse(Long id, String nome, String descricao, SetorResponse setor) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao(),
                SetorResponse.from(categoria.getSetor()));
    }
}
