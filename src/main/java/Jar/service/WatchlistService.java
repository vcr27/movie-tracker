package Jar.service;

import Jar.dto.WatchlistResponse;
import Jar.entity.*;
import Jar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import Jar.exception.*;


import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public String addToWatchlist(Long movieId) {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        if (watchlistRepository.findByUserAndMovie(user, movie).isPresent()) {
            return "Already in watchlist";
        }

        Watchlist entry = Watchlist.builder()
                .user(user)
                .movie(movie)
                .watched(false)
                .build();

        watchlistRepository.save(entry);

        return "Added to watchlist";
    }

        public Page<WatchlistResponse> getMyWatchlist(int page, int size, Boolean watched) {

                String email = (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();
                        
                User user = userRepository.findByEmail(email)
                        .orElseThrow();

                Pageable pageable = PageRequest.of(page, size);
                Page<Watchlist> watchlistPage;

                if (watched != null) {
                        watchlistPage = watchlistRepository.findByUserAndWatched(user, watched, pageable);
                } else {
                        watchlistPage = watchlistRepository.findByUser(user, pageable);
                }

                return watchlistPage.map(entry ->
                        WatchlistResponse.builder()
                                .movieId(entry.getMovie().getId())
                                .title(entry.getMovie().getTitle())
                                .releaseYear(entry.getMovie().getReleaseYear())
                                .genre(entry.getMovie().getGenre())
                                .imdbRating(entry.getMovie().getRating())
                                .watched(entry.isWatched())
                                .userRating(entry.getRating())
                                .build()
                );
        }

        public String markAsWatched(Long movieId){
                String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                User user = userRepository.findByEmail(email).orElseThrow();
                Movie movie = movieRepository.findById(movieId)
                        .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

                Watchlist entry = watchlistRepository.findByUserAndMovie(user, movie).orElseThrow(() -> new ResourceNotFoundException("Movie not in watchlist"));
                
                if(entry.isWatched()){
                        return "Already marked as watched";

                };

                entry.setWatched(true);
                watchlistRepository.save(entry);

                return "Marked as watched";
        
        }

        public void rateMovie(Long movieId, Integer rating){

                if(rating == null || rating < 1 || rating > 10){
                        throw new BadRequestException("Rating must be between 1 and 10");

                }

                String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                User user = userRepository.findByEmail(email).orElseThrow();
                Movie movie = movieRepository.findById(movieId)
                        .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

                Watchlist entry = watchlistRepository.findByUserAndMovie(user, movie).orElseThrow(() -> new ResourceNotFoundException("Movie not in watchlist"));

                entry.setRating(rating);
                watchlistRepository.save(entry);
        }


}