package dev.roxy.kryptonplayer.core.data.repository

object TmdbLocalMatcher {
    fun isFuzzyMatch(title: String, filename: String, alternativeTitles: List<String> = emptyList()): Boolean {
        if (isSingleFuzzyMatch(title, filename)) return true
        return alternativeTitles.any { altTitle -> isSingleFuzzyMatch(altTitle, filename) }
    }

    private fun isSingleFuzzyMatch(title: String, filename: String): Boolean {
        val cleanTitle = title.lowercase().replace(Regex("[^a-z0-9 ]"), " ")
        val cleanFilename = filename.lowercase().replace(Regex("[^a-z0-9 ]"), " ")
        
        val stopWords = setOf("the", "a", "an", "of", "and", "or", "in", "on", "at", "to", "for", "with", "by")
        val titleTokens = cleanTitle.split("\\s+".toRegex())
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopWords }
            
        if (titleTokens.isEmpty()) return false
        
        return titleTokens.all { token -> cleanFilename.contains(token) }
    }

    fun parseSeasonEpisode(filename: String): Pair<Int, Int>? {
        val cleanName = filename.lowercase()
        // Match patterns like s01e02, s1e2, 1x02, 01x02, ep03, ep 03, episode 03
        val s00e00Regex = Regex("s(\\d{1,2})e(\\d{1,3})")
        val x00Regex = Regex("(\\d{1,2})x(\\d{1,3})")
        val epRegex = Regex("(?:ep|episode)\\s*(\\d{1,3})")

        s00e00Regex.find(cleanName)?.let { match ->
            val season = match.groupValues[1].toIntOrNull() ?: 1
            val episode = match.groupValues[2].toIntOrNull() ?: 1
            return Pair(season, episode)
        }

        x00Regex.find(cleanName)?.let { match ->
            val season = match.groupValues[1].toIntOrNull() ?: 1
            val episode = match.groupValues[2].toIntOrNull() ?: 1
            return Pair(season, episode)
        }

        epRegex.find(cleanName)?.let { match ->
            val episode = match.groupValues[1].toIntOrNull() ?: 1
            return Pair(1, episode)
        }

        return null
    }

    fun isEpisodeMatch(showTitle: String, season: Int, episode: Int, filename: String, alternativeTitles: List<String> = emptyList()): Boolean {
        if (!isFuzzyMatch(showTitle, filename, alternativeTitles)) return false
        val parsed = parseSeasonEpisode(filename) ?: return false
        return parsed.first == season && parsed.second == episode
    }
}
