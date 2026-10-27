package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.CategoriaRequest;
import br.edu.ifrn.chamados.dto.CategoriaResponse;
import br.edu.ifrn.chamados.dto.PageResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ChamadoRepository chamadoRepository;
    private final SetorService setorService;

    @Transactional(readOnly = true)
    public PageResponse<CategoriaResponse> listar(String nome, Long setorId, Pageable pageable) {
        String filtroNome = nome == null ? "" : nome.trim();
        return PageResponse.from(categoriaRepository.filtrar(filtroNome, setorId, pageable)
                .map(CategoriaResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoriaResponse> listarPorSetor(Long setorId, String nome, Pageable pageable) {
        setorService.buscarEntidade(setorId);
        return listar(nome, setorId, pageable);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.from(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        preencher(categoria, request);
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidade(id);
        preencher(categoria, request);
        return CategoriaResponse.from(categoria);
    }

    // Regra 8: não é permitido excluir categoria que tenha chamados vinculados
    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidade(id);
        if (chamadoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Não é possível excluir a categoria " + categoria.getNome()
                    + ": existem chamados vinculados a ela");
        }
        categoriaRepository.delete(categoria);
    }

    Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " não encontrada"));
    }

    private void preencher(Categoria categoria, CategoriaRequest request) {
        categoria.setNome(request.nome().trim());
        categoria.setDescricao(request.descricao());
        categoria.setSetor(setorService.buscarEntidade(request.setorId()));
    }
}
