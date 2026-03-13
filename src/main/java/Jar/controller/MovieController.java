package Jar.controller;

import Jar.dto.MovieSuggestionResponse;
import Jar.entity.Movie;
import Jar.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/search")
    public Movie searchMovie(@RequestParam String title) {
        return movieService.fetchAndSaveMovie(title);
    }

    @GetMapping("/suggestions")
    public List<Movie> suggestions() {
        return movieService.getSuggestedMovies();
    }

    @GetMapping("/autocomplete")
    public List<MovieSuggestionResponse> autocomplete(@RequestParam String query) {
        return movieService.autocompleteMovies(query);
    }
}
