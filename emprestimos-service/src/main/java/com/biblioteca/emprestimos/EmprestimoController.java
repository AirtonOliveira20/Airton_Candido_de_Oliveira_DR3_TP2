package com.biblioteca.emprestimos;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {

    // Dados em memoria
    private final Map<Long, Emprestimo> emprestimos = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    private final LivrosClient livrosClient;

    public EmprestimoController(LivrosClient livrosClient) {
        this.livrosClient = livrosClient;
    }

    @GetMapping
    public List<Emprestimo> listar() {
        return new ArrayList<>(emprestimos.values());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Emprestimo> buscar(@PathVariable Long id) {
        Emprestimo e = emprestimos.get(id);
        return e == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(e);
    }

    /** Lista os livros consultando o outro microsservico - prova simples da comunicacao. */
    @GetMapping("/livros-disponiveis")
    public List<LivroDTO> livrosDisponiveis() {
        return livrosClient.listarDisponiveis();
    }

    @PostMapping
    public ResponseEntity<?> emprestar(@RequestBody EmprestimoRequest request) {
        if (request.livroId() == null || request.nomeLeitor() == null || request.nomeLeitor().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "livroId e nomeLeitor sao obrigatorios"));
        }

        // 1) Chamada HTTP ao livros-service (outro container)
        LivroDTO livro = livrosClient.buscarLivro(request.livroId()).orElse(null);
        if (livro == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Livro " + request.livroId() + " nao existe no livros-service"));
        }
        if (!livro.disponivel()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "Livro '" + livro.titulo() + "' ja esta emprestado"));
        }

        // 2) Marca o livro como indisponivel no livros-service
        livrosClient.alterarDisponibilidade(livro.id(), false);

        Emprestimo e = new Emprestimo();
        e.setId(sequencia.incrementAndGet());
        e.setLivroId(livro.id());
        e.setTituloLivro(livro.titulo());
        e.setNomeLeitor(request.nomeLeitor());
        e.setDataEmprestimo(LocalDate.now());
        emprestimos.put(e.getId(), e);
        return ResponseEntity.status(HttpStatus.CREATED).body(e);
    }

    @PostMapping("/{id}/devolucao")
    public ResponseEntity<?> devolver(@PathVariable Long id) {
        Emprestimo e = emprestimos.get(id);
        if (e == null) {
            return ResponseEntity.notFound().build();
        }
        if (!e.isAtivo()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", "Emprestimo ja devolvido"));
        }
        livrosClient.alterarDisponibilidade(e.getLivroId(), true);
        e.setDataDevolucao(LocalDate.now());
        return ResponseEntity.ok(e);
    }

    /** Se o livros-service estiver fora do ar ou inalcancavel, responde 503 com a URL tentada. */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> livrosIndisponivel(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "erro", "Nao foi possivel falar com o livros-service",
                "url", livrosClient.getBaseUrl(),
                "detalhe", String.valueOf(ex.getMessage())));
    }
}
