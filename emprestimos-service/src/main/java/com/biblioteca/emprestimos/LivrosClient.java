package com.biblioteca.emprestimos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP do livros-service.
 * A URL base vem de livros.service.url; dentro do Docker ela e http://livros-service:8081,
 * onde "livros-service" e o nome do container resolvido pelo DNS interno do Docker.
 */
@Component
public class LivrosClient {

    private static final Logger log = LoggerFactory.getLogger(LivrosClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public LivrosClient(@Value("${livros.service.url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        log.info("livros-service configurado em {}", baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Optional<LivroDTO> buscarLivro(Long id) {
        try {
            return Optional.ofNullable(
                    restClient.get().uri("/livros/{id}", id).retrieve().body(LivroDTO.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public List<LivroDTO> listarDisponiveis() {
        LivroDTO[] livros = restClient.get().uri("/livros").retrieve().body(LivroDTO[].class);
        return livros == null ? List.of()
                : Arrays.stream(livros).filter(LivroDTO::disponivel).toList();
    }

    public void alterarDisponibilidade(Long id, boolean disponivel) {
        restClient.put()
                .uri("/livros/{id}/disponibilidade", id)
                .body(Map.of("disponivel", disponivel))
                .retrieve()
                .toBodilessEntity();
    }
}
