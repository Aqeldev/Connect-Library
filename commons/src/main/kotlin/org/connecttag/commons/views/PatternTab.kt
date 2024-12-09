package org.connecttag.commons.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import androidx.biometric.auth.AuthPromptHost
import androidx.core.os.postDelayed
import androidx.core.widget.TextViewCompat
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import org.connecttag.commons.R
import org.connecttag.commons.databinding.TabPatternBinding
import org.connecttag.commons.extensions.getProperPrimaryColor
import org.connecttag.commons.extensions.getProperTextColor
import org.connecttag.commons.extensions.performHapticFeedback
import org.connecttag.commons.extensions.updateTextColors
import org.connecttag.commons.helpers.PROTECTION_PATTERN
import org.connecttag.commons.interfaces.BaseSecurityTab
import org.connecttag.commons.interfaces.HashListener

class PatternTab(context: Context, attrs: AttributeSet) : BaseSecurityTab(context, attrs) {
    private var scrollView: MyScrollView? = null

    private lateinit var binding: TabPatternBinding

    override val protectionType = PROTECTION_PATTERN
    override val defaultTextRes = R.string.insert_pattern
    override val wrongTextRes = R.string.wrong_pattern
    override val titleTextView: TextView
        get() = binding.patternLockTitle

    @SuppressLint("ClickableViewAccessibility")
    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = TabPatternBinding.bind(this)

        val textColor = context.getProperTextColor()
        context.updateTextColors(binding.patternLockHolder)

        binding.patternLockView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> scrollView?.isScrollable = false
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scrollView?.isScrollable = true
            }
            false
        }

        binding.patternLockView.correctStateColor = context.getProperPrimaryColor()
        binding.patternLockView.normalStateColor = textColor
        binding.patternLockView.addPatternLockListener(object : PatternLockViewListener {
            override fun onComplete(pattern: MutableList<PatternLockView.Dot>?) {
                receivedHash(PatternLockUtils.patternToSha1(binding.patternLockView, pattern))
            }

            override fun onCleared() {}

            override fun onStarted() {}

            override fun onProgress(progressPattern: MutableList<PatternLockView.Dot>?) {}
        })

        TextViewCompat.setCompoundDrawableTintList(binding.patternLockTitle, ColorStateList.valueOf(textColor))
        maybeShowCountdown()
    }

    override fun initTab(
        requiredHash: String,
        listener: HashListener,
        scrollView: MyScrollView?,
        biometricPromptHost: AuthPromptHost,
        showBiometricAuthentication: Boolean
    ) {
        this.requiredHash = requiredHash
        this.scrollView = scrollView
        computedHash = requiredHash
        hashListener = listener
    }

    override fun onLockedOutChange(lockedOut: Boolean) {
        binding.patternLockView.isInputEnabled = !lockedOut
    }

    private fun receivedHash(newHash: String) {
        if (isLockedOut()) {
            performHapticFeedback()
            return
        }

        when {
            computedHash.isEmpty() -> {
                computedHash = newHash
                binding.patternLockView.clearPattern()
                binding.patternLockTitle.setText(R.string.repeat_pattern)
            }

            computedHash == newHash -> {
                binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
                onCorrectPassword()
            }

            else -> {
                onIncorrectPassword()
                binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
                Handler().postDelayed(delayInMillis = 1000) {
                    binding.patternLockView.clearPattern()
                    if (requiredHash.isEmpty()) {
                        computedHash = ""
                    }
                }
            }
        }
    }
}