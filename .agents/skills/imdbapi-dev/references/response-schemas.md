# IMDbAPI.dev — Full Response Schemas

Complete field reference for all objects returned by `api.imdbapi.dev`.

---

## Table of Contents

1. [Title](#title)
2. [Name](#name)
3. [Credit](#credit)
4. [Episode](#episode)
5. [Season](#season)
6. [Image](#image)
7. [Video](#video)
8. [Rating](#rating)
9. [AwardNomination](#awardnomination)
10. [BoxOffice](#boxoffice)
11. [Certificate](#certificate)
12. [ReleaseDate](#releasedate)
13. [AKA](#aka)
14. [CompanyCredit](#companycredit)
15. [ParentsGuide](#parentsguide)
16. [Interest](#interest)
17. [NameRelationship](#namerelationship)
18. [NameTrivia](#nametrivia)
19. [PrecisionDate](#precisiondate)
20. [List Response Wrappers](#list-response-wrappers)

---

## Title

```typescript
Title {
  id: string                    // "tt1234567"
  type: string                  // "MOVIE" | "TV_SERIES" | "TV_MINI_SERIES" | "TV_SPECIAL" | "TV_MOVIE" | "SHORT" | "VIDEO" | "VIDEO_GAME"
  isAdult: boolean
  primaryTitle: string          // Most recognized name
  originalTitle: string         // Original release name
  primaryImage: Image | null
  startYear: number | null      // Release year (movies) or start year (series)
  endYear: number | null        // End year for series; null if ongoing or not a series
  runtimeSeconds: number | null // Total runtime in SECONDS (not minutes!)
  genres: string[]              // e.g. ["Drama", "Crime"]
  rating: Rating | null         // null for unrated titles
  metacritic: Metacritic | null
  plot: string | null
  directors: Name[]
  writers: Name[]
  stars: Name[]
  originCountries: Country[]
  spokenLanguages: Language[]
  interests: Interest[]
}
```

---

## Name

```typescript
Name {
  id: string                      // "nm1234567"
  displayName: string
  alternativeNames: string[]      // Stage names, nicknames
  primaryImage: Image | null
  primaryProfessions: string[]    // e.g. ["actor", "director", "producer"]
  biography: string | null
  heightCm: number | null
  birthName: string | null
  birthDate: PrecisionDate | null
  birthLocation: string | null
  deathDate: PrecisionDate | null // null if alive
  deathLocation: string | null
  deathReason: string | null
  meterRanking: NameMeterRanking | null
}

NameMeterRanking {
  currentRank: number
  changeDirection: "UP" | "DOWN" | "none"
  difference: number              // Positive = rank improved, negative = dropped
}
```

---

## Credit

```typescript
Credit {
  title: Title | null   // Present on name filmography responses
  name: Name | null     // Present on title credits responses
  category: string      // "actor" | "actress" | "director" | "writer" | "producer" | etc.
  characters: string[]  // Character names (for actors/actresses)
  episodeCount: number | null  // For TV appearances
}
```

---

## Episode

```typescript
Episode {
  id: string
  title: string | null
  primaryImage: Image | null
  season: string | null
  episodeNumber: number | null
  runtimeSeconds: number | null
  plot: string | null
  rating: Rating | null
  releaseDate: PrecisionDate | null
}
```

---

## Season

```typescript
Season {
  season: string        // Season number as string, e.g. "1"
  episodeCount: number
}
```

---

## Image

```typescript
Image {
  url: string           // Direct image URL
  width: number         // Pixels
  height: number        // Pixels
  type: string          // "poster" | "still_frame" | "event" | "behind_the_scenes" | etc.
}
```

Images are served at full resolution from the `url`. You can resize via URL manipulation on some CDN-backed URLs, but this is not officially documented — use `width`/`height` for layout calculations.

---

## Video

```typescript
Video {
  id: string
  type: string          // e.g. "trailer", "clip", "featurette"
  name: string | null
  primaryImage: Image | null  // Thumbnail
  description: string | null
  width: number | null  // Video resolution
  height: number | null
  runtimeSeconds: number | null
}
```

---

## Rating

```typescript
Rating {
  aggregateRating: number   // 1.0–10.0
  voteCount: number
}
```

---

## Metacritic

```typescript
Metacritic {
  url: string       // Link to Metacritic page
  score: number     // 0–100
  reviewCount: number
}
```

---

## AwardNomination

```typescript
AwardNomination {
  titles: Title[]         // Associated titles
  nominees: Name[]        // Nominated people
  event: Event
  year: number
  text: string            // e.g. "Best Picture"
  category: string        // Award category name
  isWinner: boolean
  winnerRank: number | null
}

Event {
  id: string
  name: string            // e.g. "Academy Awards, USA"
}

AwardNominationStats {
  nominationCount: number
  winCount: number
}
```

`GET /titles/{id}/awardNominations` response includes:
```json
{
  "stats": { "nominationCount": 14, "winCount": 7 },
  "awardNominations": [...],
  "nextPageToken": "..."
}
```

---

## BoxOffice

```typescript
BoxOffice {
  domesticGross: Money | null
  worldwideGross: Money | null
  openingWeekendGross: OpeningWeekendGross | null
  productionBudget: Money | null
}

Money {
  amount: string    // Large integer as string (int64)
  currency: string  // e.g. "USD"
}

OpeningWeekendGross {
  gross: Money
  weekendEndDate: PrecisionDate
}
```

---

## Certificate

```typescript
Certificate {
  rating: string      // e.g. "PG-13", "R", "15", "U/A"
  country: Country
  attributes: string[] // e.g. ["violence", "language"]
}
```

---

## ReleaseDate

```typescript
ReleaseDate {
  country: Country
  releaseDate: PrecisionDate
  attributes: string[]  // e.g. ["Theatrical", "Limited", "DVD"]
}
```

---

## AKA

```typescript
AKA {
  text: string          // Title in this language/country
  country: Country | null
  language: Language | null
  attributes: string[]  // e.g. ["original title", "working title", "alternative title"]
}
```

---

## CompanyCredit

```typescript
CompanyCredit {
  company: Company
  category: string      // "production" | "distribution" | "sales" | "miscellaneous"
  countries: Country[]
  yearsInvolved: YearsInvolved | null
  attributes: string[]
}

Company {
  id: string
  name: string
}

YearsInvolved {
  startYear: number | null
  endYear: number | null
}
```

---

## ParentsGuide

```typescript
ParentsGuide {
  category: ParentsGuideCategory
  severityBreakdowns: ParentsGuideSeverity[]
  reviews: ParentsGuideReview[]
}

// Categories
ParentsGuideCategory =
  | "SEXUAL_CONTENT"          // Sex & Nudity
  | "VIOLENCE"                // Violence & Gore
  | "PROFANITY"               // Profanity
  | "ALCOHOL_DRUGS"           // Alcohol, Drugs & Smoking
  | "FRIGHTENING_INTENSE_SCENES"

ParentsGuideSeverity {
  severityLevel: string   // "None" | "Mild" | "Moderate" | "Severe"
  voteCount: number
}

ParentsGuideReview {
  text: string
  isSpoiler: boolean
}
```

---

## Interest

```typescript
Interest {
  id: string
  name: string            // e.g. "Action", "Action Epic", "Adult Animation"
  primaryImage: Image | null
  description: string | null
  isSubgenre: boolean
  similarInterests: Interest[]
}

InterestCategory {
  category: string        // Category identifier
  interests: Interest[]
}
```

Use `GET /interests` to get all categories + interests. Use interest IDs in `GET /titles?interestIds=...`.

---

## NameRelationship

```typescript
NameRelationship {
  name: Name
  relationType: string    // "spouse" | "parent" | "child" | "sibling" | etc.
  attributes: string[]    // e.g. ["divorced", "1990–1995"]
}
```

---

## NameTrivia

```typescript
NameTrivia {
  id: string
  text: string
  interestCount: number   // Users who found this interesting
  voteCount: number
}
```

---

## PrecisionDate

```typescript
PrecisionDate {
  year: number
  month: number | null    // null if only year is known
  day: number | null      // null if only year/month known
}
```

---

## Country & Language

```typescript
Country {
  code: string    // ISO 3166-1 alpha-2: "US", "GB", "IN", "JP", etc.
  name: string    // "United States", "United Kingdom", etc.
}

Language {
  code: string    // ISO 639-3: "eng", "jpn", "hin", etc.
  name: string    // "English", "Japanese", "Hindi", etc.
}
```

---

## List Response Wrappers

Most list endpoints return a consistent wrapper:

```typescript
// Title list responses
{
  titles: Title[]
  totalCount: number
  nextPageToken: string | null
}

// Credits
{
  credits: Credit[]
  totalCount: number
  nextPageToken: string | null
}

// Episodes
{
  episodes: Episode[]
  totalCount: number    // Total episodes that have aired
  nextPageToken: string | null
}

// Images
{
  images: Image[]
  totalCount: number
  nextPageToken: string | null
}

// Videos
{
  videos: Video[]
  totalCount: number
  nextPageToken: string | null
}

// Seasons (no pagination — all seasons returned at once)
{
  seasons: Season[]
}

// AKAs (no pagination)
{
  akas: AKA[]
}

// Certificates
{
  certificates: Certificate[]
  totalCount: number
}

// ParentsGuide (no pagination)
{
  parentsGuide: ParentsGuide[]
}

// Names
{
  names: Name[]
  nextPageToken: string | null
}

// Star meter
{
  names: Name[]
  nextPageToken: string | null
}

// Search titles
{
  titles: Title[]   // No totalCount or pagination
}

// Batch titles
{
  titles: Title[]   // In same order as requested IDs
}

// Batch names
{
  names: Name[]
}

// Interest categories
{
  categories: InterestCategory[]
}
```
