package Jar.service;

import Jar.entity.Movie;
import Jar.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    private final String API_KEY = "YOUR_API_KEY";

    public Movie fetchAndSaveMovie(String title) {

        RestTemplate restTemplate = new RestTemplate();

        String url = "http://www.omdbapi.com/?t=" + title + "&apikey=" + API_KEY;

        Map response = restTemplate.getForObject(url, Map.class);

        if (response == null || response.get("imdbID") == null) {
            throw new RuntimeException("Movie not found");
        }

        String externalId = response.get("imdbID").toString();


        return movieRepository.findByExternalId(externalId)
                .orElseGet(() -> {

                    Movie movie = Movie.builder()
                            .externalId(externalId)
                            .title(response.get("Title").toString())
                            .releaseYear(response.get("Year").toString())
                            .genre(response.get("Genre").toString())
                            .rating(Double.parseDouble(response.get("imdbRating").toString()))
                            .build();

                    return movieRepository.save(movie);
                });
    }
}