package dev.tasker2.domain

/** User-adjustable display settings. Pure and Android-free so ranges and defaults are unit tested. */
data class AppSettings(
    val tabFontSp: Int = DEFAULT_TAB_FONT_SP,
    val listFontSp: Int = DEFAULT_LIST_FONT_SP,
    val tabGapSpaces: Int = DEFAULT_TAB_GAP_SPACES,
) {
    /** Every value forced into its allowed range. */
    fun normalized(): AppSettings = AppSettings(
        tabFontSp = tabFontSp.coerceIn(TAB_FONT_RANGE),
        listFontSp = listFontSp.coerceIn(LIST_FONT_RANGE),
        tabGapSpaces = tabGapSpaces.coerceIn(TAB_GAP_RANGE),
    )

    /** Task notes scale with the title: 13sp notes at the default 17sp title. */
    val notesFontSp: Float get() = listFontSp * NOTES_RATIO

    companion object {
        const val DEFAULT_TAB_FONT_SP = 15
        const val DEFAULT_LIST_FONT_SP = 17
        const val DEFAULT_TAB_GAP_SPACES = 6
        const val NOTES_RATIO = 13f / 17f
        val TAB_FONT_RANGE = 11..24
        val LIST_FONT_RANGE = 12..24
        val TAB_GAP_RANGE = 1..12
    }
}
