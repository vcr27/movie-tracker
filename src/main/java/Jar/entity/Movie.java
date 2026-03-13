package Jar.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;


@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String externalId; //from TMDB or OMDb

    private String title;
    private String releaseYear;

    private String genre;

    private Double rating;

    private String posterUrl;

    
    
}
