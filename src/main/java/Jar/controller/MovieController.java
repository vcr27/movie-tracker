package Jar.controller;

import Jar.entity.Movie;
import Jar.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/search")
    public Movie searchMovie(@RequestParam String title) {
        return movieService.fetchAndSaveMovie(title);
    }
}
