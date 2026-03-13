package Jar.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovieSuggestionResponse {
    private String title;
    private String releaseYear;
    private String posterUrl;
}
