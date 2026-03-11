package Jar.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String externalId; //from TMDB or OMDb

    private String title;
    private String releaseYear;

    private String genre;

    private Double rating;


    
    
}
