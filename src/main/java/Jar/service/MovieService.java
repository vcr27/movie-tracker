package Jar.service;

import Jar.dto.MovieSuggestionResponse;
import Jar.entity.Movie;
import Jar.exception.BadRequestException;
import Jar.exception.ResourceNotFoundException;
import Jar.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate;
    @Value("${omdb.api-key}")
    private String apiKey;
    @Value("${app.suggestions.titles}")
    private String suggestionTitles;
    @Value("${tmdb.api-key:}")
    private String tmdbApiKey;
    @Value("${tmdb.base-url:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;
    @Value("${tmdb.image-base-url:https://image.tmdb.org/t/p/w500}")
    private String tmdbImageBaseUrl;

    @Cacheable(value = "movieSearch", key = "#title == null ? '' : #title.trim().toLowerCase()")
    public Movie fetchAndSaveMovie(String title) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OMDB API key is not configured");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Movie title is required");
        }
        String preferredTitle = resolvePreferredTitle(title);
        String encodedTitle = URLEncoder.encode(preferredTitle, StandardCharsets.UTF_8);
        String url = "http://www.omdbapi.com/?t=" + encodedTitle + "&apikey=" + apiKey;

        Map response = restTemplate.getForObject(url, Map.class);

        if (response == null || response.get("imdbID") == null) {
            throw new ResourceNotFoundException("Movie not found");
        }

        String externalId = response.get("imdbID").toString();
        String releaseYear = response.get("Year") != null ? response.get("Year").toString() : "";
        String resolvedTitle = Objects.toString(response.get("Title"), preferredTitle);
        String posterUrl = normalizePoster(response.get("Poster"));
        if (posterUrl.isBlank()) {
            posterUrl = fetchPosterFromTmdb(resolvedTitle, releaseYear);
        }

        Movie existing = movieRepository.findByExternalId(externalId).orElse(null);
        if (existing != null) {
            if ((existing.getPosterUrl() == null || existing.getPosterUrl().isBlank()) && !posterUrl.isBlank()) {
                existing.setPosterUrl(posterUrl);
                return movieRepository.save(existing);
            }
            return existing;
        }

        Movie movie = Movie.builder()
                .externalId(externalId)
                .title(resolvedTitle)
                .releaseYear(releaseYear)
                .genre(Objects.toString(response.get("Genre"), "Unknown"))
                .rating(parseRating(response.get("imdbRating")))
                .posterUrl(posterUrl)
                .build();

        return movieRepository.save(movie);
    }

    @Cacheable(value = "movieSuggestions", key = "'trending'")
    public List<Movie> getSuggestedMovies() {
        if (tmdbApiKey != null && !tmdbApiKey.isBlank()) {
            try {
                List<Movie> trending = getTrendingFromTmdb();
                if (!trending.isEmpty()) {
                    return trending;
                }
            } catch (Exception ignored) {
                // Fall back to curated OMDb suggestions if TMDB call fails.
            }
        }

        List<Movie> suggestions = new ArrayList<>();
        List<String> titles = Arrays.stream(suggestionTitles.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        for (String title : titles) {
            try {
                suggestions.add(fetchAndSaveMovie(title));
            } catch (Exception ignored) {
                // Keep endpoint resilient if one title is unavailable in OMDb.
            }
        }
        return suggestions;
    }

    @Cacheable(value = "movieAutocomplete", key = "#query == null ? '' : #query.trim().toLowerCase()")
    public List<MovieSuggestionResponse> autocompleteMovies(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        if (tmdbApiKey != null && !tmdbApiKey.isBlank()) {
            try {
                return autocompleteFromTmdb(query);
            } catch (Exception ignored) {
                // fall through to OMDb
            }
        }
        return autocompleteFromOmdb(query);
    }

    private String resolvePreferredTitle(String query) {
        if (tmdbApiKey == null || tmdbApiKey.isBlank()) {
            return query;
        }
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = tmdbBaseUrl + "/search/movie?api_key=" + tmdbApiKey + "&query=" + encoded + "&include_adult=false";
            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null || !(response.get("results") instanceof List results) || results.isEmpty()) {
                return query;
            }
            Object first = results.get(0);
            if (!(first instanceof Map firstMap)) {
                return query;
            }
            String title = Objects.toString(firstMap.get("title"), "").trim();
            return title.isBlank() ? query : title;
        } catch (Exception ignored) {
            return query;
        }
    }

    private List<MovieSuggestionResponse> autocompleteFromTmdb(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = tmdbBaseUrl + "/search/movie?api_key=" + tmdbApiKey + "&query=" + encoded + "&include_adult=false";
        Map response = restTemplate.getForObject(url, Map.class);
        if (response == null || !(response.get("results") instanceof List results)) {
            return List.of();
        }

        List<MovieSuggestionResponse> items = new ArrayList<>();
        for (Object item : results.stream().limit(8).toList()) {
            if (!(item instanceof Map map)) {
                continue;
            }
            String title = Objects.toString(map.get("title"), "");
            if (title.isBlank()) {
                continue;
            }
            String releaseDate = Objects.toString(map.get("release_date"), "");
            String releaseYear = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";
            String posterPath = Objects.toString(map.get("poster_path"), "");
            String posterUrl = posterPath.isBlank() ? "" : tmdbImageBaseUrl + posterPath;

            items.add(MovieSuggestionResponse.builder()
                    .title(title)
                    .releaseYear(releaseYear)
                    .posterUrl(posterUrl)
                    .build());
        }
        return items;
    }

    private List<MovieSuggestionResponse> autocompleteFromOmdb(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "http://www.omdbapi.com/?s=" + encoded + "&type=movie&apikey=" + apiKey;
        Map response = restTemplate.getForObject(url, Map.class);
        if (response == null || !(response.get("Search") instanceof List results)) {
            return List.of();
        }

        List<MovieSuggestionResponse> items = new ArrayList<>();
        for (Object item : results.stream().limit(8).toList()) {
            if (!(item instanceof Map map)) {
                continue;
            }
            String title = Objects.toString(map.get("Title"), "");
            if (title.isBlank()) {
                continue;
            }
            items.add(MovieSuggestionResponse.builder()
                    .title(title)
                    .releaseYear(Objects.toString(map.get("Year"), ""))
                    .posterUrl(normalizePoster(map.get("Poster")))
                    .build());
        }
        return items;
    }

    private List<Movie> getTrendingFromTmdb() {
        String url = tmdbBaseUrl + "/trending/movie/week?api_key=" + tmdbApiKey;
        Map response = restTemplate.getForObject(url, Map.class);
        if (response == null || !(response.get("results") instanceof List results)) {
            return List.of();
        }

        List<Movie> movies = new ArrayList<>();
        for (Object item : results.stream().limit(8).toList()) {
            if (!(item instanceof Map map)) {
                continue;
            }
            String posterPath = Objects.toString(map.get("poster_path"), "");
            String posterUrl = posterPath.isBlank() ? "" : tmdbImageBaseUrl + posterPath;
            String releaseDate = Objects.toString(map.get("release_date"), "");
            String releaseYear = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";
            String title = Objects.toString(map.get("title"), "Unknown");
            Double rating = parseRating(map.get("vote_average"));

            movies.add(Movie.builder()
                    .id(map.get("id") instanceof Number n ? n.longValue() : null)
                    .title(title)
                    .releaseYear(releaseYear)
                    .genre("Trending")
                    .rating(rating)
                    .posterUrl(posterUrl)
                    .build());
        }
        return movies;
    }

    private String fetchPosterFromTmdb(String title, String year) {
        if (tmdbApiKey == null || tmdbApiKey.isBlank()) {
            return "";
        }

        try {
            String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String yearParam = (year != null && year.length() >= 4) ? "&year=" + year.substring(0, 4) : "";
            String url = tmdbBaseUrl + "/search/movie?api_key=" + tmdbApiKey + "&query=" + encoded + yearParam;

            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null || !(response.get("results") instanceof List results) || results.isEmpty()) {
                return "";
            }

            Object first = results.get(0);
            if (!(first instanceof Map firstMap)) {
                return "";
            }
            String posterPath = Objects.toString(firstMap.get("poster_path"), "");
            return posterPath.isBlank() ? "" : tmdbImageBaseUrl + posterPath;
        } catch (Exception ignored) {
            return "";
        }
    }

    private Double parseRating(Object raw) {
        if (raw == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private String normalizePoster(Object rawPoster) {
        if (rawPoster == null) {
            return "";
        }
        String poster = rawPoster.toString();
        if ("N/A".equalsIgnoreCase(poster)) {
            return "";
        }
        if (poster.startsWith("http://")) {
            return "https://" + poster.substring("http://".length());
        }
        return poster;
    }
}
