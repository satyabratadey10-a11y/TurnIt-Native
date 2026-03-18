package com.turnit.app
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageButton
enum class ButtonState { IDLE, PROCESSING, CANCELLING }
class ButtonController(private val btn: ImageButton) {
    companion object {
        private val C_IDLE = 0xFFC084FC.toInt()
        private val C_PROC = 0xFFF87171.toInt()
        private val C_CANC = 0xFFFFD700.toInt()
    }
    var state = ButtonState.IDLE; private set
    fun toProcessing() {
        if (state == ButtonState.PROCESSING) return
        state = ButtonState.PROCESSING
        pressDown {
            btn.setImageResource(R.drawable.ic_stop)
            glow(C_IDLE, C_PROC)
            bounceUp {}
        }
    }
    fun toIdle() {
        if (state == ButtonState.IDLE) return
        state = ButtonState.IDLE
        bounceUp {
            btn.setImageResource(R.drawable.ic_send)
            glow(C_PROC, C_IDLE)
        }
    }
    fun toCancelling(onEnd: () -> Unit) {
        if (state != ButtonState.PROCESSING) return
        state = ButtonState.CANCELLING
        glow(C_PROC, C_CANC)
        btn.postDelayed({
            state = ButtonState.IDLE
            btn.setImageResource(R.drawable.ic_send)
            glow(C_CANC, C_IDLE)
            onEnd()
        }, 200)
    }
    fun animatePress() = pressDown {}
    private fun pressDown(cb: () -> Unit) {
        ScaleAnimation(1f, .88f, 1f, .88f,
            android.view.animation.Animation.RELATIVE_TO_SELF, .5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, .5f
        ).apply {
            duration = 90; fillAfter = true
            setAnimationListener(end { cb() })
        }.let { btn.startAnimation(it) }
    }
    private fun bounceUp(cb: () -> Unit) {
        ScaleAnimation(.88f, 1f, .88f, 1f,
            android.view.animation.Animation.RELATIVE_TO_SELF, .5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, .5f
        ).apply {
            duration = 280
            interpolator = OvershootInterpolator(2.5f)
            fillAfter = true
            setAnimationListener(end { cb() })
        }.let { btn.startAnimation(it) }
    }
    private fun glow(from: Int, to: Int) {
        val ld = btn.background as? LayerDrawable ?: return
        val g  = ld.getDrawable(0) as? GradientDrawable ?: return
        ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply {
            duration = 450
            addUpdateListener {
                g.setColor(it.animatedValue as Int)
                btn.invalidate()
            }
            start()
        }
    }
    private fun end(cb: () -> Unit) =
        object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(
                a: android.view.animation.Animation) = Unit
            override fun onAnimationRepeat(
                a: android.view.animation.Animation) = Unit
            override fun onAnimationEnd(
                a: android.view.animation.Animation) { cb() }
        }
}
