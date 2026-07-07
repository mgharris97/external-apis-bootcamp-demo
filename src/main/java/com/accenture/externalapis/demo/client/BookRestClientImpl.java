package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);
            return new BookDto(
                    response.title(),
                    response.author(),
                    response.genre(),
                    response.price()
            );
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ClientException("Book not found: " + id, ex);
        } catch (HttpClientErrorException ex) {
            throw new ClientException("Client error fetching book " + id + ": " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new ClientException("External service error fetching book " + id + ": " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            throw new ClientException("Could not reach external service for book " + id, ex);
        }

    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] responses = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);

            List<BookDto> books = new ArrayList<>();
            for (BookApiResponse response : responses) {
                books.add(new BookDto(response.title(), response.author(), response.genre(), response.price()));
            }
            return books;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ClientException("Books endpoint not found", ex);
        } catch (HttpClientErrorException ex) {
            throw new ClientException("Client error fetching all books: " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new ClientException("External service error fetching all books: " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            throw new ClientException("Could not reach external service while fetching all books", ex);
        }
    }
}
