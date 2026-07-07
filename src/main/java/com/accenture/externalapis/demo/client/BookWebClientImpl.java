package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class BookWebClientImpl implements BookWebClient {

    private final WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                //builder.defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .bodyToMono(BookApiResponse.class)
                .map(response -> new BookDto(response.title(), response.author(), response.genre(), response.price()))
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> Mono.error(new ClientException("Book not found: " + id, ex)))
                .onErrorResume(WebClientResponseException.class, ex -> Mono.error(new ClientException("Client error fetching book " + id + ex.getStatusCode(), ex)))
                .onErrorResume(WebClientRequestException.class, ex -> Mono.error(new ClientException("Could not reach external service for book " + id, ex)));
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookApiResponse.class)
                .map(response -> new BookDto(response.title(), response.author(), response.genre(), response.price()))
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> Flux.error(new ClientException("Books endpoint not found", ex)))
                .onErrorResume(WebClientResponseException.class, ex -> Flux.error(new ClientException("Client error fetching all books: " + ex.getStatusCode(), ex)))
                .onErrorResume(WebClientRequestException.class, ex -> Flux.error(new ClientException("Could not reach external service while fetching all books", ex)));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        Mono<BookDto> book1 = getBookAsync(id1);
        Mono<BookDto> book2 = getBookAsync(id2);
        return Mono.zip(book1, book2)
                .map(tuple -> List.of(
                        tuple.getT1(),
                        tuple.getT2()
                ));
    }
}
