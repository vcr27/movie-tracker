package Jar.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "watchlist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Watchlist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Movie movie;

    private boolean watched;

    private Integer rating;// 1 - 10 optional

    
}
