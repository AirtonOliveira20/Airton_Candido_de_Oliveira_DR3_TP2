package com.biblioteca.emprestimos;

/** Copia do contrato do livros-service (cada microsservico tem seu proprio modelo). */
public record LivroDTO(Long id, String titulo, String autor, boolean disponivel) {}
