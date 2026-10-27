package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.PageResponse;
import br.edu.ifrn.chamados.dto.UsuarioRequest;
import br.edu.ifrn.chamados.dto.UsuarioResponse;
import br.edu.ifrn.chamados.exception.DadosInvalidosException;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final SetorService setorService;

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable) {
        String filtroNome = nome == null ? "" : nome.trim();
        return PageResponse.from(usuarioRepository.filtrar(filtroNome, tipo, setorId, pageable)
                .map(UsuarioResponse::from));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.from(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe um usuário com o email " + email);
        }
        Usuario usuario = new Usuario();
        preencher(usuario, request, email);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidade(id);
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new RegraNegocioException("Já existe um usuário com o email " + email);
        }
        if (usuario.getTipo() != request.tipo() && chamadoRepository.existsBySolicitanteIdOrTecnicoId(id, id)) {
            throw new RegraNegocioException("Não é possível alterar o tipo de um usuário que já possui chamados");
        }
        preencher(usuario, request, email);
        return UsuarioResponse.from(usuario);
    }

    Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado"));
    }

    private void preencher(Usuario usuario, UsuarioRequest request, String email) {
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setMatricula(request.matricula().trim());
        usuario.setTipo(request.tipo());
        // Apenas técnicos pertencem a um setor
        if (request.tipo() == TipoUsuario.TECNICO) {
            if (request.setorId() == null) {
                throw new DadosInvalidosException("setorId é obrigatório para usuários do tipo TECNICO");
            }
            usuario.setSetor(setorService.buscarEntidade(request.setorId()));
        } else {
            usuario.setSetor(null);
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
