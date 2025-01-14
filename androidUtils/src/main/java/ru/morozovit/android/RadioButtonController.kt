package ru.morozovit.android

import android.widget.RadioButton

typealias OnSelectListener = ((rb: RadioButton, index: Int) -> Unit)?

@Suppress("MemberVisibilityCanBePrivate")
class RadioButtonController(private val radioButtons: MutableList<RadioButton> = mutableListOf()) {
    private var listener: OnSelectListener = null
    private fun check(index: Int) {
        if (index in radioButtons.indices) {
            radioButtons[index].isChecked = true
            for (rb in radioButtons) {
                if (rb != radioButtons[index]) {
                    rb.isChecked = false
                }
            }
        } else {
            throw IndexOutOfBoundsException("Radiobutton index out of range")
        }
        listener?.invoke(radioButtons[index], index)
    }

    private fun check(button: RadioButton) = check(radioButtons.indexOf(button))

    fun add(button: RadioButton) {
        radioButtons.add(button)
        button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) check(button)
        }
    }

    fun select(index: Int) {
        radioButtons[index].isChecked = true
    }
    fun select(button: RadioButton) = select(radioButtons.indexOf(button))

    val checkedItem: RadioButton? get() {
        for (rb in radioButtons) {
            if (rb.isChecked) return rb
        }
        return null
    }

    val checkedIndex get() = radioButtons.indexOf(checkedItem)

    fun setOnSelectListener(listener: OnSelectListener) {
        this.listener = listener
    }
}