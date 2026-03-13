package Jar.repository;

import Jar.entity.Watchlist;
import Jar.entity.User;
import Jar.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUser(User user);

    Optional<Watchlist> findByUserAndMovie(User user, Movie movie);
    Page<Watchlist> findByUser(User user, Pageable pageable);

    Page<Watchlist> findByUserAndWatched(User user, boolean watched, Pageable pageable);
}