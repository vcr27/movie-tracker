import { useEffect, useMemo, useState } from "react";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const FALLBACK_POSTER =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="450"><rect width="100%" height="100%" fill="#121722"/><text x="50%" y="50%" fill="#a8b1d2" font-size="22" font-family="Arial" text-anchor="middle">No Poster</text></svg>'
  );

async function request(path, options = {}, token = "") {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const message =
      (isJson && (data.message || data.error)) ||
      (typeof data === "string" ? data : "Request failed");
    throw new Error(message);
  }

  return data;
}

export default function App() {
  const [email, setEmail] = useState("demo@movietracker.dev");
  const [password, setPassword] = useState("password123");
  const [token, setToken] = useState(() => localStorage.getItem("movietracker_token") || "");

  const [title, setTitle] = useState("");
  const [movie, setMovie] = useState(null);
  const [searchSuggestions, setSearchSuggestions] = useState([]);
  const [showSearchSuggestions, setShowSearchSuggestions] = useState(false);
  const [suggestions, setSuggestions] = useState([]);
  const [suggestionIndex, setSuggestionIndex] = useState(0);

  const [watchlist, setWatchlist] = useState([]);
  const [status, setStatus] = useState("Ready");
  const [loading, setLoading] = useState(false);

  const authState = useMemo(() => (token ? "Authenticated" : "Guest"), [token]);
  const watchedCount = useMemo(() => watchlist.filter((item) => item.watched).length, [watchlist]);
  const ratedCount = useMemo(() => watchlist.filter((item) => item.userRating != null).length, [watchlist]);

  useEffect(() => {
    if (token) {
      localStorage.setItem("movietracker_token", token);
      loadWatchlist(token);
      return;
    }
    localStorage.removeItem("movietracker_token");
    setWatchlist([]);
  }, [token]);

  useEffect(() => {
    loadSuggestions();
  }, []);

  useEffect(() => {
    if (suggestions.length <= 1) {
      return;
    }
    const id = setInterval(() => {
      setSuggestionIndex((current) => (current + 1) % suggestions.length);
    }, 3500);
    return () => clearInterval(id);
  }, [suggestions]);

  useEffect(() => {
    const id = setInterval(() => {
      loadSuggestions();
    }, 20000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (!token || title.trim().length < 2) {
      setSearchSuggestions([]);
      return;
    }
    const id = setTimeout(async () => {
      try {
        const data = await request(`/movies/autocomplete?query=${encodeURIComponent(title.trim())}`, {}, token);
        setSearchSuggestions(Array.isArray(data) ? data : []);
      } catch (_) {
        setSearchSuggestions([]);
      }
    }, 250);
    return () => clearTimeout(id);
  }, [title, token]);

  async function run(action, callback) {
    try {
      setLoading(true);
      setStatus(`${action}...`);
      await callback();
      setStatus(`${action} complete`);
    } catch (error) {
      setStatus(`${action} failed: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }

  async function register() {
    await run("Register", async () => {
      const data = await request(
        "/auth/register",
        {
          method: "POST",
          body: JSON.stringify({ email, password })
        }
      );
      setStatus(typeof data === "string" ? data : "Registered successfully");
    });
  }

  async function login() {
    await run("Login", async () => {
      const jwt = await request(
        "/auth/login",
        {
          method: "POST",
          body: JSON.stringify({ email, password })
        }
      );
      setToken(typeof jwt === "string" ? jwt : "");
    });
  }

  async function searchMovie() {
    await run("Search", async () => {
      const data = await request(`/movies/search?title=${encodeURIComponent(title)}`, {}, token);
      setMovie(data);
      setShowSearchSuggestions(false);
    });
  }

  async function loadSuggestions() {
    try {
      const data = await request("/movies/suggestions");
      setSuggestions(Array.isArray(data) ? data : []);
    } catch (_) {
      setSuggestions([]);
    }
  }

  async function addToWatchlist(movieId) {
    await run("Add", async () => {
      await request(`/watchlist/add/${movieId}`, { method: "POST" }, token);
      await loadWatchlist();
    });
  }

  async function loadWatchlist(tokenOverride) {
    const authToken = tokenOverride || token;
    await run("Load watchlist", async () => {
      const data = await request("/watchlist/my?page=0&size=20", {}, authToken);
      setWatchlist(data.content || []);
    });
  }

  async function markWatched(movieId) {
    await run("Mark watched", async () => {
      await request(`/watchlist/${movieId}/watched`, { method: "PUT" }, token);
      await loadWatchlist();
    });
  }

  async function rate(movieId, value) {
    await run("Rate", async () => {
      await request(
        `/watchlist/${movieId}/rate`,
        {
          method: "PUT",
          body: JSON.stringify({ rating: Number(value) })
        },
        token
      );
      await loadWatchlist();
    });
  }

  function logout() {
    setToken("");
    setStatus("Logged out");
  }

  function selectSuggestion(item) {
    setTitle(item.title);
    setShowSearchSuggestions(false);
  }

  if (!token) {
    return (
      <div className="page">
        <div className="bg-shape bg-shape-a" />
        <div className="bg-shape bg-shape-b" />
        <div className="bg-shape bg-shape-c" />

        <main className="auth-shell">
          <section className="panel cinematic auth-card">
            <p className="brand">CineScope</p>
            <p className="kicker">Welcome Back</p>
            <h1>Sign in to continue your movie journey</h1>
            <p className="subtext">Register once, then login to manage your watchlist and ratings.</p>

            <label>Email</label>
            <input value={email} onChange={(e) => setEmail(e.target.value)} />
            <label>Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            <div className="actions">
              <button onClick={register} disabled={loading}>Register</button>
              <button onClick={login} disabled={loading}>Login</button>
            </div>
            <p className="subtext">{status}</p>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="bg-shape bg-shape-a" />
      <div className="bg-shape bg-shape-b" />
      <div className="bg-shape bg-shape-c" />

      <header className="hero panel cinematic">
        <div className="topbar">
          <p className="brand">CineScope</p>
          <div className="badge-row">
            <span className={`pill ${token ? "pill-live" : "pill-muted"}`}>{authState}</span>
            <span className="pill pill-api">API {API_BASE_URL}</span>
            <button onClick={logout} disabled={loading}>Logout</button>
          </div>
        </div>

        <p className="kicker">Your Personal Screening Room</p>
        <h1>Track What You Watch. Curate What You Love.</h1>
        <p className="subtext">
          Build a living watchlist, rate films from 1 to 10, and keep your movie history organized like a modern streaming app.
        </p>

        <div className="stats-row">
          <div className="stat-card">
            <span>Total Titles</span>
            <strong>{watchlist.length}</strong>
          </div>
          <div className="stat-card">
            <span>Watched</span>
            <strong>{watchedCount}</strong>
          </div>
          <div className="stat-card">
            <span>Rated</span>
            <strong>{ratedCount}</strong>
          </div>
        </div>
      </header>

      <main className="grid">
        <section className="panel full-width glass">
          <h2>Movie Search</h2>
          <label>Title</label>
          <div className="row search-row">
            <div className="search-field">
              <input
                placeholder="Search titles like Spider-Man..."
                value={title}
                onChange={(e) => {
                  setTitle(e.target.value);
                  setShowSearchSuggestions(true);
                }}
                onFocus={() => setShowSearchSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSearchSuggestions(false), 140)}
              />
              {showSearchSuggestions && searchSuggestions.length > 0 && (
                <div className="search-suggestions">
                  {searchSuggestions.map((item, index) => (
                    <button
                      key={`${item.title}-${index}`}
                      className="suggestion-item"
                      onClick={() => selectSuggestion(item)}
                      type="button"
                    >
                      <img
                        src={item.posterUrl || FALLBACK_POSTER}
                        alt={item.title}
                        onError={(e) => {
                          e.currentTarget.src = FALLBACK_POSTER;
                        }}
                      />
                      <span>{item.title} {item.releaseYear ? `(${item.releaseYear})` : ""}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <button onClick={searchMovie} disabled={loading || !title.trim()}>
              Search
            </button>
          </div>

          {movie && (
            <article className="movie-card featured-card">
              <p className="card-eyebrow">Featured Result</p>
              <div className="poster-layout">
                <img
                  className="poster"
                  src={movie.posterUrl || FALLBACK_POSTER}
                  alt={movie.title}
                  loading="lazy"
                  onError={(e) => {
                    e.currentTarget.src = FALLBACK_POSTER;
                  }}
                />
                <div>
                  <h3>{movie.title}</h3>
                  <div className="meta-row">
                    <span>{movie.releaseYear}</span>
                    <span>{movie.genre}</span>
                    <span>IMDb {movie.rating}</span>
                  </div>
                </div>
              </div>
              <button onClick={() => addToWatchlist(movie.id)} disabled={loading}>Add to Watchlist</button>
            </article>
          )}
        </section>

        <section className="panel full-width glass">
          <div className="row spread">
            <h2>My Watchlist</h2>
            <button onClick={loadWatchlist} disabled={!token || loading}>Refresh</button>
          </div>

          {watchlist.length === 0 ? (
            <p className="subtext">No entries yet. Search and add your first movie.</p>
          ) : (
            <div className="watchlist-grid">
              {watchlist.map((entry) => (
                <article key={entry.movieId} className="watch-item">
                  <div className="poster-layout">
                    <img
                      className="poster"
                      src={entry.posterUrl || FALLBACK_POSTER}
                      alt={entry.title}
                      loading="lazy"
                      onError={(e) => {
                        e.currentTarget.src = FALLBACK_POSTER;
                      }}
                    />
                    <div>
                      <h3>{entry.title}</h3>
                      <div className="meta-row">
                        <span>{entry.releaseYear}</span>
                        <span>{entry.genre}</span>
                      </div>
                    </div>
                  </div>
                  <p>IMDb {entry.imdbRating} | Your rating {entry.userRating ?? "-"}</p>
                  <p className={entry.watched ? "watch-state done" : "watch-state pending"}>
                    {entry.watched ? "Watched" : "Planned"}
                  </p>
                  <div className="actions">
                    <button
                      onClick={() => markWatched(entry.movieId)}
                      disabled={loading || entry.watched}
                    >
                      Mark Watched
                    </button>
                    <label className="rating-label">
                      Rate:
                      <select
                        defaultValue={entry.userRating ?? ""}
                        onChange={(e) => {
                          const value = e.target.value;
                          if (value) {
                            rate(entry.movieId, Number(value));
                          }
                        }}
                        disabled={loading}
                      >
                        <option value="">Select</option>
                        {Array.from({ length: 10 }, (_, i) => i + 1).map((score) => (
                          <option key={score} value={score}>
                            {score}
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="panel full-width glass">
          <div className="row spread">
            <h2>Trending Now</h2>
            <div className="row">
              <p className="subtext">Compact picks from the backend</p>
              <button onClick={loadSuggestions} disabled={loading}>Refresh</button>
            </div>
          </div>

          {suggestions.length === 0 ? (
            <p className="subtext">Suggestions unavailable right now.</p>
          ) : (
            <div className="trending-strip">
              {suggestions.slice(0, 4).map((item, index) => (
                <article
                  key={item.id || item.externalId || item.title}
                  className={`trend-card ${index === suggestionIndex % 4 ? "active" : ""}`}
                >
                  <img
                    src={item.posterUrl || FALLBACK_POSTER}
                    alt={item.title}
                    loading="lazy"
                    onError={(e) => {
                      e.currentTarget.src = FALLBACK_POSTER;
                    }}
                  />
                  <div className="trend-meta">
                    <h3>{item.title}</h3>
                    <p>{item.releaseYear} | IMDb {item.rating ?? "-"}</p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>

      <footer className="status panel">
        <p>{status}</p>
      </footer>
    </div>
  );
}
