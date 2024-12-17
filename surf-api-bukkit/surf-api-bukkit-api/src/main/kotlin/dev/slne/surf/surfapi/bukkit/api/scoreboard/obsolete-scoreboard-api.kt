package dev.slne.surf.surfapi.bukkit.api.scoreboard

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "The SurfScoreboard API is obsolete and should be replaced with FeatherBoard"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ObsoleteScoreboardApi