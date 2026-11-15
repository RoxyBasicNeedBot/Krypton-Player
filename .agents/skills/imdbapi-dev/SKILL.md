---
name: imdbapi-dev
description: >
  Use this skill whenever the user wants to work with imdbapi.dev — a free IMDb REST API
  for accessing movie, TV show, and people data. Covers fetching title details by IMDb ID,
  searching titles, listing credits/episodes/seasons/images/videos/awards, getting name
  (actor/director/person) info, filmography, trivia, relationships, box office data,
  batch gets, filtering/sorting titles, and integrating the API in Android/Kotlin, Python,
  or any HTTP client. Trigger on any mention of "imdbapi.dev", "free IMDb API", "IMDb REST
  API", "fetch movie by imdb id", "search imdb titles", "get actor info imdb", "imdb credits
  API", or building any feature that needs IMDb data. Always use this skill before writing
  any imdbapi.dev code or answering questions about it.
---

# imdbapi.dev Skill

Free IMDb REST API (v2.7.12) — no auth required. Base URL: `https://api.imdbapi.dev`

Join the community: [t.me/imdbapi](https://t.me/imdbapi)

---

## Quick Start

No API key needed — just make GET requests:

```http
GET https://api.imdbapi.dev/titles/tt0111161
```

```kotlin
// Kotlin / Retrofit
interface ImdbApi {
    @GET("titles/{id}")
    suspend fun getTitle(@Path("id") id: String): Title

    @GET("search/titles")
    suspend fun search(@Query("query") q: String, @Query("limit") limit: Int = 10): SearchResponse
}
// Base URL: "https://api.imdbapi.dev/"
```

---

## ID Formats

- **Title IDs** → `tt` prefix — e.g. `tt0111161` (The Shawshank Redemption)
- **Name IDs** → `nm` prefix — e.g. `nm0000093` (Brad Pitt)
- **Interest IDs** → genre/category strings from `/interests`

---

## Endpoints Cheatsheet

### 🎬 Titles

| What you want | Endpoint |
|---|---|
| Search titles | `GET /search/titles?query=inception&limit=10` |
| Get title by ID | `GET /titles/{titleId}` |
| Batch get (max 5) | `GET /titles:batchGet?titleIds=tt01&titleIds=tt02` |
| List/filter titles | `GET /titles` (see filters below) |
| Credits (cast/crew) | `GET /titles/{titleId}/credits` |
| Episodes | `GET /titles/{titleId}/episodes?season=1` |
| Seasons | `GET /titles/{titleId}/seasons` |
| Images | `GET /titles/{titleId}/images` |
| Videos | `GET /titles/{titleId}/videos` |
| Release dates | `GET /titles/{titleId}/releaseDates` |
| AKAs (alt titles) | `GET /titles/{titleId}/akas` |
| Award nominations | `GET /titles/{titleId}/awardNominations` |
| Box office | `GET /titles/{titleId}/boxOffice` |
| Parents guide | `GET /titles/{titleId}/parentsGuide` |
| Certificates (ratings) | `GET /titles/{titleId}/certificates` |
| Company credits | `GET /titles/{titleId}/companyCredits` |

### 👤 Names (People)

| What you want | Endpoint |
|---|---|
| Get person by ID | `GET /names/{nameId}` |
| Filmography | `GET /names/{nameId}/filmography` |
| Images | `GET /names/{nameId}/images` |
| Relationships | `GET /names/{nameId}/relationships` |
| Trivia | `GET /names/{nameId}/trivia` |
| Batch get (max 5) | `GET /names:batchGet?nameIds=nm01&nameIds=nm02` |
| Star meter chart | `GET /chart/starmeter` |

### 🎭 Interests (Genres/Categories)

| What you want | Endpoint |
|---|---|
| All interest categories | `GET /interests` |
| Specific interest | `GET /interests/{interestId}` |

---

## Filtering & Sorting Titles (`GET /titles`)

All params are optional:

```http
GET /titles
  ?types=MOVIE&types=TV_SERIES    # Filter by type (multi)
  &genres=Action&genres=Drama     # Filter by genre (multi)
  &countryCodes=US&countryCodes=GB # ISO 3166-1 alpha-2 (multi)
  &languageCodes=en               # ISO 639-1 or 639-2 (multi)
  &nameIds=nm0000093              # Filter by person (multi)
  &interestIds=someId             # Filter by interest (multi)
  &startYear=2000&endYear=2023    # Year range
  &minAggregateRating=7.0         # 0.0–10.0
  &maxAggregateRating=10.0
  &minVoteCount=1000              # 0–1,000,000,000
  &sortBy=SORT_BY_USER_RATING     # See sort options
  &sortOrder=DESC                 # ASC | DESC
  &pageToken=TOKEN                # Pagination
```

**`types` values:** `MOVIE`, `TV_SERIES`, `TV_MINI_SERIES`, `TV_SPECIAL`, `TV_MOVIE`, `SHORT`, `VIDEO`, `VIDEO_GAME`

**`sortBy` values:** `SORT_BY_POPULARITY`, `SORT_BY_RELEASE_DATE`, `SORT_BY_USER_RATING`, `SORT_BY_USER_RATING_COUNT`, `SORT_BY_YEAR`

---

## Pagination

Most list endpoints return a `nextPageToken`. Pass it as `?pageToken=TOKEN` for the next page.
Most list sub-endpoints also accept `pageSize` (1–50, default 20).

```json
{
  "titles": [...],
  "totalCount": 1523,
  "nextPageToken": "abc123"
}
```

---

## Key Response Shapes

### Title object
```json
{
  "id": "tt0111161",
  "type": "MOVIE",
  "primaryTitle": "The Shawshank Redemption",
  "originalTitle": "The Shawshank Redemption",
  "isAdult": false,
  "startYear": 1994,
  "endYear": null,
  "runtimeSeconds": 8520,
  "genres": ["Drama"],
  "plot": "Two imprisoned men bond...",
  "rating": { "aggregateRating": 9.3, "voteCount": 2800000 },
  "metacritic": { "score": 82, "reviewCount": 20, "url": "..." },
  "primaryImage": { "url": "...", "width": 267, "height": 400, "type": "poster" },
  "directors": [{ "id": "nm0001104", "displayName": "Frank Darabont" }],
  "writers": [...],
  "stars": [...],
  "originCountries": [{ "code": "US", "name": "United States" }],
  "spokenLanguages": [{ "code": "eng", "name": "English" }],
  "interests": [...]
}
```

### Name object
```json
{
  "id": "nm0000093",
  "displayName": "Brad Pitt",
  "primaryProfessions": ["actor", "producer"],
  "biography": "...",
  "birthDate": { "year": 1963, "month": 12, "day": 18 },
  "birthLocation": "Shawnee, Oklahoma, USA",
  "heightCm": 180,
  "primaryImage": { "url": "...", "width": 400, "height": 600 },
  "meterRanking": { "currentRank": 42, "changeDirection": "UP", "difference": 5 }
}
```

### Credit object
```json
{
  "title": { /* Title object */ },
  "name": { /* Name object */ },
  "category": "actor",
  "characters": ["Tyler Durden"],
  "episodeCount": null
}
```

---

## Common Patterns

### Search then fetch details
```kotlin
// 1. Search
val results = api.search("The Dark Knight", limit = 5)
// 2. Get full details for first result
val title = api.getTitle(results.titles[0].id)
```

### Get top-rated movies (1000+ votes)
```http
GET /titles?types=MOVIE&sortBy=SORT_BY_USER_RATING&sortOrder=DESC&minVoteCount=1000
```

### Get all episodes of a series season
```http
GET /titles/tt0903747/episodes?season=1&pageSize=50
```

### Batch fetch multiple titles
```http
GET /titles:batchGet?titleIds=tt0111161&titleIds=tt0068646&titleIds=tt0071562
```

---

## Android / Kotlin Setup (Retrofit + Gson)

```kotlin
// Data classes (minimal)
data class Title(
    val id: String,
    val primaryTitle: String?,
    val originalTitle: String?,
    val type: String?,
    val startYear: Int?,
    val runtimeSeconds: Int?,
    val genres: List<String>?,
    val plot: String?,
    val rating: Rating?,
    val primaryImage: Image?,
    val directors: List<Name>?,
    val stars: List<Name>?
)
data class Rating(val aggregateRating: Float?, val voteCount: Int?)
data class Image(val url: String?, val width: Int?, val height: Int?, val type: String?)
data class Name(val id: String?, val displayName: String?)
data class SearchResponse(val titles: List<Title>)
data class TitlesResponse(val titles: List<Title>, val totalCount: Int?, val nextPageToken: String?)
data class CreditsResponse(val credits: List<Credit>, val totalCount: Int?, val nextPageToken: String?)
data class Credit(val name: Name?, val category: String?, val characters: List<String>?)

// Retrofit interface
interface ImdbApiService {
    @GET("titles/{id}")
    suspend fun getTitle(@Path("id") id: String): Title

    @GET("titles:batchGet")
    suspend fun batchGetTitles(@Query("titleIds") ids: List<String>): BatchTitlesResponse

    @GET("titles")
    suspend fun listTitles(
        @Query("types") types: List<String>? = null,
        @Query("genres") genres: List<String>? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
        @Query("minAggregateRating") minRating: Float? = null,
        @Query("minVoteCount") minVotes: Int? = null,
        @Query("pageToken") pageToken: String? = null
    ): TitlesResponse

    @GET("search/titles")
    suspend fun searchTitles(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): SearchResponse

    @GET("titles/{id}/credits")
    suspend fun getCredits(
        @Path("id") id: String,
        @Query("categories") categories: List<String>? = null,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageToken") pageToken: String? = null
    ): CreditsResponse

    @GET("titles/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") id: String,
        @Query("season") season: String? = null,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageToken") pageToken: String? = null
    ): EpisodesResponse

    @GET("names/{id}")
    suspend fun getName(@Path("id") id: String): NameDetail

    @GET("names/{id}/filmography")
    suspend fun getFilmography(@Path("id") id: String): FilmographyResponse
}

// Retrofit instance (no auth needed)
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.imdbapi.dev/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(ImdbApiService::class.java)
```

> **Tip:** Add an OkHttp interceptor to log responses during development. No auth headers needed.

---

## Python Integration

```python
import httpx

BASE = "https://api.imdbapi.dev"

async def get_title(title_id: str) -> dict:
    async with httpx.AsyncClient() as c:
        r = await c.get(f"{BASE}/titles/{title_id}")
        r.raise_for_status()
        return r.json()

async def search(query: str, limit: int = 10) -> list[dict]:
    async with httpx.AsyncClient() as c:
        r = await c.get(f"{BASE}/search/titles", params={"query": query, "limit": limit})
        r.raise_for_status()
        return r.json()["titles"]

async def list_titles(**kwargs) -> dict:
    async with httpx.AsyncClient() as c:
        r = await c.get(f"{BASE}/titles", params=kwargs)
        r.raise_for_status()
        return r.json()
```

---

## PrecisionDate Format

Dates are returned as objects, not strings:
```json
{ "year": 1994, "month": 9, "day": 10 }
```
`month` and `day` may be `null` if only year is known.

---

## Error Responses

```json
{
  "code": 404,
  "message": "title not found",
  "details": []
}
```

Standard gRPC-gateway status codes. Handle `404` for missing IDs and `400` for bad params.

---

## Gotchas

1. **`runtimeSeconds` not minutes** — divide by 60 to display runtime
2. **`rating` can be null** — unrated titles have no `rating` field
3. **`primaryImage` can be null** — always null-check before loading
4. **Batch max is 5** — both `titles:batchGet` and `names:batchGet` cap at 5 IDs
5. **`search/titles` limit max is 50**
6. **`pageSize` range is 1–50** on paginated sub-endpoints
7. **No auth required** — don't add any Authorization headers
8. **Multi-value query params** repeat the key: `?types=MOVIE&types=TV_SERIES`

---

## Further Reference

- `references/response-schemas.md` — Full field-by-field breakdown of all response types
- `references/interests.md` — Interest IDs reference (genres, subgenres, categories)
