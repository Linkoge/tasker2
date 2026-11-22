package dev.tasker2.domain

/** Vertical sizes (dp) of a task row for one "Gap between tasks" value. Pure, so floor and clamping are unit tested. */
data class RowMetrics(
    /** Space between adjacent rows in the list. */
    val listSpacingDp: Float,
    /** Padding above and below the row content, each. */
    val paddingDp: Float,
    /** Square size of the circle and checkbox touch targets; also the minimum height of the title band. */
    val targetDp: Float,
) {
    /** Row height for a title-only row, i.e. the tallest control; the real row is never shorter than this. */
    val minRowHeightDp: Float get() = 2 * paddingDp + targetDp
}

object TaskRowMetrics {
    /** The 1.5 look (gap 0): 4dp padding around 48dp touch targets, no list spacing. */
    const val BASE_PADDING_DP = 4f
    const val BASE_TARGET_DP = 48f
    const val BASE_LIST_SPACING_DP = 0f

    /** Smallest sane touch target: still taller than the title line (<= ~31dp) and the priority badge/bar (32dp). */
    const val MIN_TARGET_DP = 40f

    /** Visible round circle and square checkbox sizes (the touch targets are larger). */
    const val CIRCLE_DP = 22f
    const val CHECKBOX_DP = 20f

    /** Most the gap can shrink: list spacing (already 0), then padding (2 x 4dp), then touch target 48 -> 40dp. */
    const val MAX_SHRINK_DP = 16

    /** Values below this would make rows overlap or clip, so the slider minimum stops here. */
    const val MIN_GAP_DP = -MAX_SHRINK_DP
    const val MAX_GAP_DP = 16
    val GAP_RANGE = MIN_GAP_DP..MAX_GAP_DP

    fun clampGap(gapDp: Int): Int = gapDp.coerceIn(GAP_RANGE)

    /** 1.5 stored 0..24 with positive-only meaning; the same numbers keep their meaning, larger ones fit the new range. */
    fun migrateLegacyGap(stored: Int): Int = clampGap(stored)

    /** Positive: more space between rows. Negative: shrink list spacing first, then row padding, then touch targets. */
    fun metrics(gapDp: Int): RowMetrics {
        val g = clampGap(gapDp)
        if (g >= 0) return RowMetrics(BASE_LIST_SPACING_DP + g, BASE_PADDING_DP, BASE_TARGET_DP)
        var left = -g.toFloat()
        val spacingCut = minOf(left, BASE_LIST_SPACING_DP); left -= spacingCut
        val paddingCut = minOf(left, 2 * BASE_PADDING_DP); left -= paddingCut
        val targetCut = minOf(left, BASE_TARGET_DP - MIN_TARGET_DP)
        return RowMetrics(
            BASE_LIST_SPACING_DP - spacingCut,
            BASE_PADDING_DP - paddingCut / 2,
            BASE_TARGET_DP - targetCut,
        )
    }

    /** Signed label such as "+6 dp", "0 dp" or "−6 dp" (real minus sign). */
    fun label(gapDp: Int): String = when {
        gapDp > 0 -> "+$gapDp dp"
        gapDp < 0 -> "−${-gapDp} dp"
        else -> "0 dp"
    }
}

/** Where the single crossed-out line starts and ends, in px from the row's left edge. */
object StrikeLine {
    data class Span(val start: Float, val end: Float) { val length: Float get() = end - start }

    /**
     * From the left edge of the visible round circle to the right edge of the visible square checkbox.
     * Each control is centered in a touch-target box starting at [circleBoxLeft] / [checkboxBoxLeft].
     */
    fun span(
        circleBoxLeft: Float, circleBoxSize: Float, circleSize: Float,
        checkboxBoxLeft: Float, checkboxBoxSize: Float, checkboxSize: Float,
    ): Span? {
        val start = circleBoxLeft + (circleBoxSize - circleSize) / 2
        val end = checkboxBoxLeft + (checkboxBoxSize + checkboxSize) / 2
        return if (end > start) Span(start, end) else null
    }
}
