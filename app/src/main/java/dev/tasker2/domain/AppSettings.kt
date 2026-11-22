package dev.tasker2.domain

/** User-adjustable display settings. Pure and Android-free so ranges and defaults are unit tested. */
data class AppSettings(
    val tabFontSp: Int = DEFAULT_TAB_FONT_SP,
    val listFontSp: Int = DEFAULT_LIST_FONT_SP,
    val tabGapSpaces: Int = DEFAULT_TAB_GAP_SPACES,
    val taskGapDp: Int = DEFAULT_TASK_GAP_DP,
    val showPriorityNumber: Boolean = true,
    val showPriorityBar: Boolean = true,
    /** Strike line thickness in half-dp steps (4 = 2dp), so it persists as an int. */
    val strikeHalfDp: Int = DEFAULT_STRIKE_HALF_DP,
    val crossedOutStyle: CrossedOutStyle = CrossedOutStyle.FADED,
) {
    val strikeThicknessDp: Float get() = strikeHalfDp / 2f

    /** Every value forced into its allowed range. */
    fun normalized(): AppSettings = AppSettings(
        tabFontSp = tabFontSp.coerceIn(TAB_FONT_RANGE),
        listFontSp = listFontSp.coerceIn(LIST_FONT_RANGE),
        tabGapSpaces = tabGapSpaces.coerceIn(TAB_GAP_RANGE),
        taskGapDp = TaskRowMetrics.clampGap(taskGapDp),
        showPriorityNumber = showPriorityNumber,
        showPriorityBar = showPriorityBar,
        strikeHalfDp = strikeHalfDp.coerceIn(STRIKE_HALF_RANGE),
        crossedOutStyle = crossedOutStyle,
    )

    /** Task notes scale with the title: 0.75x, i.e. 12.75sp notes at the default 17sp title. */
    val notesFontSp: Float get() = listFontSp * NOTES_RATIO

    companion object {
        const val DEFAULT_TAB_FONT_SP = 15
        const val DEFAULT_LIST_FONT_SP = 17
        const val DEFAULT_TAB_GAP_SPACES = 6
        /** Gap between task rows; 0 is the 1.4/1.5 look (the middle of the slider), negative tightens, positive loosens. */
        const val DEFAULT_TASK_GAP_DP = 0
        /** 1dp, the thinnest line. */
        const val DEFAULT_STRIKE_HALF_DP = 2
        const val NOTES_RATIO = 0.75f
        val TAB_FONT_RANGE = 11..24
        val LIST_FONT_RANGE = 12..24
        val TAB_GAP_RANGE = 1..12
        /** 1..6dp in 0.5dp steps. */
        val STRIKE_HALF_RANGE = 2..12
        val TASK_GAP_RANGE = TaskRowMetrics.GAP_RANGE
    }
}

/** How crossed-out tasks are colored: neutral gray only, or gray with a heavily faded hint of the priority color. */
enum class CrossedOutStyle(val label: String) {
    GRAY("Gray"), FADED("Faded color");

    companion object {
        fun fromId(id: Int): CrossedOutStyle = entries.getOrElse(id) { FADED }
    }
}
