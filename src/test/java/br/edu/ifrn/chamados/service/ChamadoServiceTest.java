package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.*;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private InteracaoRepository interacaoRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private ChamadoService chamadoService;

    private Setor cotic;
    private Setor manut;
    private Categoria redes;
    private Usuario solicitante;
    private Usuario tecnicoCotic;
    private Usuario tecnicoManut;

    @BeforeEach
    void setUp() {
        cotic = setor(1L, "COTIC");
        manut = setor(2L, "MANUT");
        redes = new Categoria();
        redes.setId(1L);
        redes.setNome("Rede e Wi-Fi");
        redes.setSetor(cotic);
        solicitante = usuario(1L, TipoUsuario.SOLICITANTE, null);
        tecnicoCotic = usuario(3L, TipoUsuario.TECNICO, cotic);
        tecnicoManut = usuario(4L, TipoUsuario.TECNICO, manut);
    }

    @Test
    void abrirDefineStatusAbertoDataEPrioridadeMediaPorPadrao() {
        when(usuarioService.buscarEntidade(1L)).thenReturn(solicitante);
        when(categoriaService.buscarEntidade(1L)).thenReturn(redes);
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(inv -> inv.getArgument(0));

        ChamadoResponse resposta = chamadoService.abrir(
                new ChamadoRequest("Wi-Fi caiu", "Sem rede", "Lab 3", null, 1L, 1L));

        assertThat(resposta.status()).isEqualTo(StatusChamado.ABERTO);
        assertThat(resposta.prioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(resposta.dataAbertura()).isNotNull();
    }

    @Test
    void tecnicoNaoPodeAbrirChamado() {
        when(usuarioService.buscarEntidade(3L)).thenReturn(tecnicoCotic);

        assertThatThrownBy(() -> chamadoService.abrir(
                new ChamadoRequest("x", "x", "x", Prioridade.ALTA, 1L, 3L)))
                .isInstanceOf(RegraNegocioException.class);
        verify(chamadoRepository, never()).save(any());
    }

    @Test
    void tecnicoDeOutroSetorNaoPodeAssumir() {
        Chamado chamado = chamado(StatusChamado.ABERTO, null);
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarEntidade(4L)).thenReturn(tecnicoManut);

        assertThatThrownBy(() -> chamadoService.assumir(10L, new AssumirChamadoRequest(4L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("setor");
        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
    }

    @Test
    void assumirMudaStatusParaEmAtendimento() {
        Chamado chamado = chamado(StatusChamado.ABERTO, null);
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarEntidade(3L)).thenReturn(tecnicoCotic);

        chamadoService.assumir(10L, new AssumirChamadoRequest(3L));

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
        assertThat(chamado.getTecnico()).isSameAs(tecnicoCotic);
    }

    @Test
    void somenteTecnicoAtribuidoPodeResolver() {
        Usuario outroTecnicoCotic = usuario(5L, TipoUsuario.TECNICO, cotic);
        Chamado chamado = chamado(StatusChamado.EM_ATENDIMENTO, tecnicoCotic);
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarEntidade(5L)).thenReturn(outroTecnicoCotic);

        assertThatThrownBy(() -> chamadoService.resolver(10L, new ResolverChamadoRequest(5L, "feito")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void resolverPreencheSolucaoEDataFechamento() {
        Chamado chamado = chamado(StatusChamado.EM_ATENDIMENTO, tecnicoCotic);
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarEntidade(3L)).thenReturn(tecnicoCotic);

        chamadoService.resolver(10L, new ResolverChamadoRequest(3L, "AP substituído"));

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.RESOLVIDO);
        assertThat(chamado.getSolucao()).isEqualTo("AP substituído");
        assertThat(chamado.getDataFechamento()).isNotNull();
    }

    @Test
    void naoPodeCancelarChamadoEmAtendimento() {
        Chamado chamado = chamado(StatusChamado.EM_ATENDIMENTO, tecnicoCotic);
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarEntidade(1L)).thenReturn(solicitante);

        assertThatThrownBy(() -> chamadoService.cancelar(10L, new CancelarChamadoRequest(1L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Transição inválida");
    }

    @Test
    void chamadoFinalizadoNaoAceitaInteracao() {
        Chamado chamado = chamado(StatusChamado.RESOLVIDO, tecnicoCotic);
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));

        assertThatThrownBy(() -> chamadoService.registrarInteracao(10L, new InteracaoRequest(1L, "oi")))
                .isInstanceOf(RegraNegocioException.class);
        verify(interacaoRepository, never()).save(any());
    }

    @Test
    void usuarioDeForaNaoPodeRegistrarInteracao() {
        Chamado chamado = chamado(StatusChamado.EM_ATENDIMENTO, tecnicoCotic);
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarEntidade(4L)).thenReturn(tecnicoManut);

        assertThatThrownBy(() -> chamadoService.registrarInteracao(10L, new InteracaoRequest(4L, "oi")))
                .isInstanceOf(RegraNegocioException.class);
    }

    private Chamado chamado(StatusChamado status, Usuario tecnico) {
        Chamado chamado = new Chamado();
        chamado.setId(10L);
        chamado.setTitulo("Wi-Fi caiu");
        chamado.setStatus(status);
        chamado.setPrioridade(Prioridade.MEDIA);
        chamado.setCategoria(redes);
        chamado.setSolicitante(solicitante);
        chamado.setTecnico(tecnico);
        return chamado;
    }

    private static Setor setor(Long id, String sigla) {
        Setor setor = new Setor();
        setor.setId(id);
        setor.setSigla(sigla);
        setor.setNome(sigla);
        return setor;
    }

    private static Usuario usuario(Long id, TipoUsuario tipo, Setor setor) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário " + id);
        usuario.setTipo(tipo);
        usuario.setSetor(setor);
        return usuario;
    }
}
