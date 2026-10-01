package com.biblioteca.emprestimos;

import java.time.LocalDate;

public class Emprestimo {
    private Long id;
    private Long livroId;
    private String tituloLivro;
    private String nomeLeitor;
    private LocalDate dataEmprestimo;
    private LocalDate dataDevolucao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLivroId() { return livroId; }
    public void setLivroId(Long livroId) { this.livroId = livroId; }
    public String getTituloLivro() { return tituloLivro; }
    public void setTituloLivro(String tituloLivro) { this.tituloLivro = tituloLivro; }
    public String getNomeLeitor() { return nomeLeitor; }
    public void setNomeLeitor(String nomeLeitor) { this.nomeLeitor = nomeLeitor; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDate dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(LocalDate dataDevolucao) { this.dataDevolucao = dataDevolucao; }
    public boolean isAtivo() { return dataDevolucao == null; }
}
