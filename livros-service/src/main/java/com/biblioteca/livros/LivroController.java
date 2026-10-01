package com.biblioteca.livros;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/livros")
public class LivroController {

    private final LivroRepository repository;

    public LivroController(LivroRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Livro> listar() {
        return repository.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Livro> buscar(@PathVariable Long id) {
        return repository.buscar(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Livro> criar(@RequestBody Livro livro) {
        livro.setId(null);
        livro.setDisponivel(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.salvar(livro));
    }

    /** Usado pelo emprestimos-service para marcar o livro como emprestado/devolvido. */
    @PutMapping("/{id}/disponibilidade")
    public ResponseEntity<Livro> alterarDisponibilidade(@PathVariable Long id,
                                                        @RequestBody Map<String, Boolean> body) {
        Boolean disponivel = body.get("disponivel");
        if (disponivel == null) {
            return ResponseEntity.badRequest().build();
        }
        return repository.buscar(id)
                .map(livro -> {
                    livro.setDisponivel(disponivel);
                    return ResponseEntity.ok(repository.salvar(livro));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return repository.remover(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
