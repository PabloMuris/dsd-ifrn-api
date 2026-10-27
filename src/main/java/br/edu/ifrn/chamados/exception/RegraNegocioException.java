package br.edu.ifrn.chamados.exception;

/**
 * Lançada quando uma regra de negócio é violada (HTTP 409).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
