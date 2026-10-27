package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.exception.DadosInvalidosException;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.*;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final InteracaoRepository interacaoRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;

    // Regras 1 e 2
    @Transactional
    public ChamadoResponse abrir(ChamadoRequest request) {
        Usuario solicitante = usuarioService.buscarEntidade(request.solicitanteId());
        if (!solicitante.isSolicitante()) {
            throw new RegraNegocioException("Apenas usuários do tipo SOLICITANTE podem abrir chamados");
        }
        Categoria categoria = categoriaService.buscarEntidade(request.categoriaId());

        Chamado chamado = new Chamado();
        chamado.setTitulo(request.titulo().trim());
        chamado.setDescricao(request.descricao().trim());
        chamado.setLocal(request.local().trim());
        chamado.setPrioridade(request.prioridade() != null ? request.prioridade() : Prioridade.MEDIA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categoria);
        return ChamadoResponse.from(chamadoRepository.save(chamado));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChamadoResponse> listar(StatusChamado status, Prioridade prioridade, Long setorId,
                                                Long categoriaId, Long solicitanteId, Long tecnicoId,
                                                LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new DadosInvalidosException("dataInicio não pode ser posterior a dataFim");
        }
        // O período é inclusivo nas duas pontas: dataFim cobre o dia inteiro
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.plusDays(1).atStartOfDay() : null;
        return PageResponse.from(chamadoRepository.filtrar(status, prioridade, setorId, categoriaId,
                solicitanteId, tecnicoId, inicio, fim, pageable).map(ChamadoResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChamadoResponse> listarDoUsuario(Long usuarioId, StatusChamado status, Pageable pageable) {
        Usuario usuario = usuarioService.buscarEntidade(usuarioId);
        Long solicitanteId = usuario.isSolicitante() ? usuarioId : null;
        Long tecnicoId = usuario.isTecnico() ? usuarioId : null;
        return PageResponse.from(chamadoRepository.filtrar(status, null, null, null,
                solicitanteId, tecnicoId, null, null, pageable).map(ChamadoResponse::from));
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse detalhar(Long id) {
        return ChamadoDetalheResponse.from(buscarEntidade(id));
    }

    // Regra 3
    @Transactional
    public ChamadoResponse assumir(Long id, AssumirChamadoRequest request) {
        Chamado chamado = buscarEntidade(id);
        validarNaoFinalizado(chamado);
        Usuario tecnico = usuarioService.buscarEntidade(request.tecnicoId());
        if (!tecnico.isTecnico()) {
            throw new RegraNegocioException("Apenas usuários do tipo TECNICO podem assumir chamados");
        }
        validarTransicao(chamado, StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO);
        if (!Objects.equals(chamado.getCategoria().getSetor().getId(), tecnico.getSetor().getId())) {
            throw new RegraNegocioException("O técnico pertence ao setor " + tecnico.getSetor().getSigla()
                    + ", mas a categoria do chamado é do setor " + chamado.getCategoria().getSetor().getSigla());
        }
        chamado.setTecnico(tecnico);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        return ChamadoResponse.from(chamado);
    }

    // Regra 4
    @Transactional
    public ChamadoResponse resolver(Long id, ResolverChamadoRequest request) {
        Chamado chamado = buscarEntidade(id);
        validarNaoFinalizado(chamado);
        Usuario tecnico = usuarioService.buscarEntidade(request.tecnicoId());
        validarTransicao(chamado, StatusChamado.EM_ATENDIMENTO, StatusChamado.RESOLVIDO);
        if (!isMesmoUsuario(tecnico, chamado.getTecnico())) {
            throw new RegraNegocioException("Somente o técnico atribuído ao chamado pode resolvê-lo");
        }
        if (request.solucao() == null || request.solucao().isBlank()) {
            throw new DadosInvalidosException("A solução é obrigatória para resolver o chamado");
        }
        chamado.setSolucao(request.solucao().trim());
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setDataFechamento(LocalDateTime.now());
        return ChamadoResponse.from(chamado);
    }

    // Regra 5
    @Transactional
    public ChamadoResponse cancelar(Long id, CancelarChamadoRequest request) {
        Chamado chamado = buscarEntidade(id);
        validarNaoFinalizado(chamado);
        Usuario solicitante = usuarioService.buscarEntidade(request.solicitanteId());
        if (!isMesmoUsuario(solicitante, chamado.getSolicitante())) {
            throw new RegraNegocioException("Somente o solicitante que abriu o chamado pode cancelá-lo");
        }
        validarTransicao(chamado, StatusChamado.ABERTO, StatusChamado.CANCELADO);
        chamado.setStatus(StatusChamado.CANCELADO);
        chamado.setDataFechamento(LocalDateTime.now());
        return ChamadoResponse.from(chamado);
    }

    // Regras 6 e 7
    @Transactional
    public InteracaoResponse registrarInteracao(Long id, InteracaoRequest request) {
        Chamado chamado = buscarEntidade(id);
        validarNaoFinalizado(chamado);
        Usuario autor = usuarioService.buscarEntidade(request.autorId());
        if (!isMesmoUsuario(autor, chamado.getSolicitante()) && !isMesmoUsuario(autor, chamado.getTecnico())) {
            throw new RegraNegocioException(
                    "Apenas o solicitante e o técnico atribuído podem registrar interações no chamado");
        }
        Interacao interacao = new Interacao();
        interacao.setTexto(request.texto().trim());
        interacao.setDataHora(LocalDateTime.now());
        interacao.setChamado(chamado);
        interacao.setAutor(autor);
        return InteracaoResponse.from(interacaoRepository.save(interacao));
    }

    private Chamado buscarEntidade(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + id + " não encontrado"));
    }

    // Regra 6: chamados RESOLVIDO ou CANCELADO não aceitam alterações nem novas interações
    private void validarNaoFinalizado(Chamado chamado) {
        if (chamado.isFinalizado()) {
            throw new RegraNegocioException("O chamado " + chamado.getId() + " está " + chamado.getStatus()
                    + " e não aceita alterações nem novas interações");
        }
    }

    // Ciclo de vida: ABERTO -> EM_ATENDIMENTO -> RESOLVIDO e ABERTO -> CANCELADO
    private void validarTransicao(Chamado chamado, StatusChamado esperado, StatusChamado destino) {
        if (chamado.getStatus() != esperado) {
            throw new RegraNegocioException("Transição inválida: o chamado está " + chamado.getStatus()
                    + " e só pode ir para " + destino + " a partir de " + esperado);
        }
    }

    private boolean isMesmoUsuario(Usuario usuario, Usuario outro) {
        return outro != null && Objects.equals(usuario.getId(), outro.getId());
    }
}
