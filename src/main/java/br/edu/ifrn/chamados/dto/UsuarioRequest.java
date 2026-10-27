package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 150) String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email mal formatado") @Size(max = 150) String email,
        @NotBlank(message = "matrícula é obrigatória") @Size(max = 30) String matricula,
        @NotNull(message = "tipo é obrigatório") TipoUsuario tipo,
        Long setorId
) {
}
