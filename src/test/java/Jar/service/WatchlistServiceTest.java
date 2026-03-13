package Jar.service;

import Jar.dto.WatchlistResponse;
import Jar.entity.Movie;
import Jar.entity.User;
import Jar.entity.Watchlist;
import Jar.exception.BadRequestException;
import Jar.repository.MovieRepository;
import Jar.repository.UserRepository;
import Jar.repository.WatchlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyWatchlist_shouldFilterByWatchedAndMapResponse() {
        String email = "user@example.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null)
        );

        User user = User.builder().id(1L).email(email).password("x").role("USER").build();
        Movie movie = Movie.builder()
                .id(10L)
                .title("Inception")
                .releaseYear("2010")
                .genre("Sci-Fi")
                .rating(8.8)
                .build();
        Watchlist entry = Watchlist.builder()
                .id(100L)
                .user(user)
                .movie(movie)
                .watched(true)
                .rating(9)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(watchlistRepository.findByUserAndWatched(eq(user), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<WatchlistResponse> result = watchlistService.getMyWatchlist(0, 5, true);

        assertEquals(1, result.getTotalElements());
        WatchlistResponse dto = result.getContent().get(0);
        assertEquals(10L, dto.getMovieId());
        assertEquals("Inception", dto.getTitle());
        assertTrue(dto.isWatched());
        assertEquals(9, dto.getUserRating());
    }

    @Test
    void rateMovie_shouldRejectOutOfRangeRating() {
        assertThrows(BadRequestException.class, () -> watchlistService.rateMovie(1L, 11));
        verifyNoInteractions(userRepository, movieRepository, watchlistRepository);
    }
}
