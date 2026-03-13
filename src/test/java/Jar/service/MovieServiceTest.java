package Jar.service;

import Jar.entity.Movie;
import Jar.exception.BadRequestException;
import Jar.exception.ResourceNotFoundException;
import Jar.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MovieService movieService;

    @Test
    void fetchAndSaveMovie_shouldThrowWhenTitleIsBlank() {
        ReflectionTestUtils.setField(movieService, "apiKey", "omdb-key");
        assertThrows(BadRequestException.class, () -> movieService.fetchAndSaveMovie("  "));
        verifyNoInteractions(restTemplate, movieRepository);
    }

    @Test
    void fetchAndSaveMovie_shouldReturnExistingMovieByExternalId() {
        ReflectionTestUtils.setField(movieService, "apiKey", "omdb-key");

        Movie existing = Movie.builder()
                .id(5L)
                .externalId("tt0133093")
                .title("The Matrix")
                .releaseYear("1999")
                .genre("Action, Sci-Fi")
                .rating(8.7)
                .build();

        Map<String, Object> response = Map.of(
                "imdbID", "tt0133093",
                "Title", "The Matrix",
                "Year", "1999",
                "Genre", "Action, Sci-Fi",
                "imdbRating", "8.7"
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        when(movieRepository.findByExternalId("tt0133093")).thenReturn(Optional.of(existing));

        Movie result = movieService.fetchAndSaveMovie("The Matrix");

        assertEquals(5L, result.getId());
        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void fetchAndSaveMovie_shouldThrowNotFoundWhenOmdbReturnsNoImdbId() {
        ReflectionTestUtils.setField(movieService, "apiKey", "omdb-key");

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of("Response", "False"));

        assertThrows(ResourceNotFoundException.class, () -> movieService.fetchAndSaveMovie("Unknown"));
    }
}
