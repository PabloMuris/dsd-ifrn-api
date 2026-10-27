package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.PageResponse;
import br.edu.ifrn.chamados.dto.SetorRequest;
import br.edu.ifrn.chamados.dto.SetorResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetorService {

    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;

    @Transactional(readOnly = true)
    public PageResponse<SetorResponse> listar(String nome, Pageable pageable) {
        String filtroNome = nome == null ? "" : nome.trim();
        return PageResponse.from(setorRepository.findByNomeContainingIgnoreCase(filtroNome, pageable)
                .map(SetorResponse::from));
    }

    @Transactional(readOnly = true)
    public SetorResponse buscar(Long id) {
        return SetorResponse.from(buscarEntidade(id));
    }

    @Transactional
    public SetorResponse criar(SetorRequest request) {
        String sigla = normalizarSigla(request.sigla());
        if (setorRepository.existsBySiglaIgnoreCase(sigla)) {
            throw new RegraNegocioException("Já existe um setor com a sigla " + sigla);
        }
        Setor setor = new Setor();
        setor.setNome(request.nome().trim());
        setor.setSigla(sigla);
        return SetorResponse.from(setorRepository.save(setor));
    }

    @Transactional
    public SetorResponse atualizar(Long id, SetorRequest request) {
        Setor setor = buscarEntidade(id);
        String sigla = normalizarSigla(request.sigla());
        if (setorRepository.existsBySiglaIgnoreCaseAndIdNot(sigla, id)) {
            throw new RegraNegocioException("Já existe um setor com a sigla " + sigla);
        }
        setor.setNome(request.nome().trim());
        setor.setSigla(sigla);
        return SetorResponse.from(setor);
    }

    // Regra 8: não é permitido excluir setor que tenha chamados vinculados
    @Transactional
    public void excluir(Long id) {
        Setor setor = buscarEntidade(id);
        if (chamadoRepository.existsByCategoriaSetorId(id)) {
            throw new RegraNegocioException("Não é possível excluir o setor " + setor.getSigla()
                    + ": existem chamados vinculados às suas categorias");
        }
        if (usuarioRepository.existsBySetorId(id)) {
            throw new RegraNegocioException("Não é possível excluir o setor " + setor.getSigla()
                    + ": existem técnicos vinculados a ele");
        }
        // Sem chamados, as categorias do setor podem ser removidas junto com ele
        categoriaRepository.deleteAll(categoriaRepository.findBySetorId(id));
        setorRepository.delete(setor);
    }

    Setor buscarEntidade(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor " + id + " não encontrado"));
    }

    private String normalizarSigla(String sigla) {
        return sigla.trim().toUpperCase();
    }
}
