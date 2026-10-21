package io.github.tutualican.yina.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion is physics, not decoration.
 *
 * Rules of thumb:
 *  - touch feedback must be immediate (< 90 ms to first visible change);
 *  - release always settles with a spring, never a linear curve;
 *  - nothing bounces more than once, nothing moves further than it needs to;
 *  - reduced-motion users get opacity-only transitions.
 */
object Motion {

    /** Press-in: fast and slightly overdamped so the finger feels contact. */
    fun <T> press() = spring<T>(
        dampingRatio = 0.62f,
        stiffness = 900f,
    )

    /** Release/settle: one soft overshoot, then rest. */
    fun <T> settle() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = 380f,
    )

    /** Sheet / large surface entrance. */
    fun <T> enter() = spring<T>(
        dampingRatio = 0.88f,
        stiffness = 260f,
    )

    /** Snappy state change for text and numbers. */
    fun <T> quick() = tween<T>(
        durationMillis = 180,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    )

    /** Calm fade for cross-fades. */
    fun <T> fade() = tween<T>(
        durationMillis = 240,
        easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
    )

    const val PRESS_SCALE = 0.975f
    const val SWIPE_THRESHOLD_DP = 96f
    const val STAGGER_MS = 40
}
