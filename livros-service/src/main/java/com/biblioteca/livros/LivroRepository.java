package com.biblioteca.livros;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Repositorio em memoria: os dados se perdem quando o container para. */
@Repository
public class LivroRepository {

    private final Map<Long, Livro> livros = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    public LivroRepository() {
        salvar(new Livro(null, "Dom Casmurro", "Machado de Assis"));
        salvar(new Livro(null, "O Cortico", "Aluisio Azevedo"));
        salvar(new Livro(null, "Clean Architecture", "Robert C. Martin"));
    }

    public List<Livro> listar() {
        return new ArrayList<>(livros.values());
    }

    public Optional<Livro> buscar(Long id) {
        return Optional.ofNullable(livros.get(id));
    }

    public Livro salvar(Livro livro) {
        if (livro.getId() == null) {
            livro.setId(sequencia.incrementAndGet());
        }
        livros.put(livro.getId(), livro);
        return livro;
    }

    public boolean remover(Long id) {
        return livros.remove(id) != null;
    }
}
