package br.edu.ifrn.chamados.exception;

/**
 * Lançada quando os dados enviados são inválidos para a operação (HTTP 400).
 */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
