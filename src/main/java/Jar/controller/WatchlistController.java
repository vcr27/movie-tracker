package Jar.controller;

import Jar.dto.WatchlistResponse;
import Jar.entity.Watchlist;
import Jar.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import Jar.dto.RatingRequest;
import org.springframework.data.domain.Page;



import java.util.List;


@RestController
@RequestMapping("/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping("/add/{movieId}")
    public String add(@PathVariable Long movieId) {
        return watchlistService.addToWatchlist(movieId);
    }

    @GetMapping("/my")
    public Page<WatchlistResponse> myWatchlist(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "5") int size,
                                                @RequestParam(required = false) Boolean watched) {
        return watchlistService.getMyWatchlist(page, size, watched);
    }

    @PutMapping("/{movieId}/watched")
    public String markWatched(@PathVariable Long movieId) {
        return watchlistService.markAsWatched(movieId);
    }

    @PutMapping("/{movieId}/rate")
    public ResponseEntity<?> rateMovie(@PathVariable Long movieId, @RequestBody RatingRequest request) {
        watchlistService.rateMovie(movieId, request.getRating());
        return ResponseEntity.ok("Rating updated");
    }
}
