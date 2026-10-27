package br.edu.ifrn.chamados.exception;

/**
 * Lançada quando o recurso solicitado não existe (HTTP 404).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
