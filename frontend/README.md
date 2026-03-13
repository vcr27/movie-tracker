# MovieTracker Frontend

Minimal React + Vite client for the Spring Boot MovieTracker API.

## Features
- Register + Login
- Search movie from OMDb-backed backend endpoint
- Add movie to watchlist
- View watchlist
- Mark watched
- Rate movie

## Run
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

Set backend URL via `.env`:
```bash
VITE_API_BASE_URL=http://localhost:8080
```

## Docker
This frontend is also built and served by Nginx via the root `docker-compose.yml`.
When running compose, the frontend is available at `http://localhost:4173`.
