package dev.tasker2.domain

/** User-adjustable display settings. Pure and Android-free so ranges and defaults are unit tested. */
data class AppSettings(
    val tabFontSp: Int = DEFAULT_TAB_FONT_SP,
    val listFontSp: Int = DEFAULT_LIST_FONT_SP,
    val tabGapSpaces: Int = DEFAULT_TAB_GAP_SPACES,
    val taskGapDp: Int = DEFAULT_TASK_GAP_DP,
    val showPriorityNumber: Boolean = true,
    val showPriorityBar: Boolean = true,
) {
    /** Every value forced into its allowed range. */
    fun normalized(): AppSettings = AppSettings(
        tabFontSp = tabFontSp.coerceIn(TAB_FONT_RANGE),
        listFontSp = listFontSp.coerceIn(LIST_FONT_RANGE),
        tabGapSpaces = tabGapSpaces.coerceIn(TAB_GAP_RANGE),
        taskGapDp = taskGapDp.coerceIn(TASK_GAP_RANGE),
        showPriorityNumber = showPriorityNumber,
        showPriorityBar = showPriorityBar,
    )

    /** Task notes scale with the title: 13sp notes at the default 17sp title. */
    val notesFontSp: Float get() = listFontSp * NOTES_RATIO

    companion object {
        const val DEFAULT_TAB_FONT_SP = 15
        const val DEFAULT_LIST_FONT_SP = 17
        const val DEFAULT_TAB_GAP_SPACES = 6
        /** Spacing between task rows; 0 equals the 1.4 spacing, so nothing changes until adjusted. */
        const val DEFAULT_TASK_GAP_DP = 0
        const val NOTES_RATIO = 13f / 17f
        val TAB_FONT_RANGE = 11..24
        val LIST_FONT_RANGE = 12..24
        val TAB_GAP_RANGE = 1..12
        val TASK_GAP_RANGE = 0..24
    }
}
