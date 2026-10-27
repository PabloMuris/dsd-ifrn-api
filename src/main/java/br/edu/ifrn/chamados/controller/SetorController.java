package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.CategoriaResponse;
import br.edu.ifrn.chamados.dto.PageResponse;
import br.edu.ifrn.chamados.dto.SetorRequest;
import br.edu.ifrn.chamados.dto.SetorResponse;
import br.edu.ifrn.chamados.service.CategoriaService;
import br.edu.ifrn.chamados.service.SetorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/setores")
@RequiredArgsConstructor
public class SetorController {

    private final SetorService setorService;
    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<SetorResponse> criar(@Valid @RequestBody SetorRequest request) {
        SetorResponse criado = setorService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    public PageResponse<SetorResponse> listar(@RequestParam(required = false) String nome,
                                              @PageableDefault(sort = "id") Pageable pageable) {
        return setorService.listar(nome, pageable);
    }

    @GetMapping("/{id}")
    public SetorResponse buscar(@PathVariable Long id) {
        return setorService.buscar(id);
    }

    @GetMapping("/{id}/categorias")
    public PageResponse<CategoriaResponse> listarCategorias(@PathVariable Long id,
                                                            @RequestParam(required = false) String nome,
                                                            @PageableDefault(sort = "id") Pageable pageable) {
        return categoriaService.listarPorSetor(id, nome, pageable);
    }

    @PutMapping("/{id}")
    public SetorResponse atualizar(@PathVariable Long id, @Valid @RequestBody SetorRequest request) {
        return setorService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        setorService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
