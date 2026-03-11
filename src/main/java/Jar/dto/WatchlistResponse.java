package Jar.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WatchlistResponse {

    private Long movieId;
    private String title;
    private String releaseYear;
    private String genre;
    private Double imdbRating;
    private boolean watched;
    private Integer userRating;
}