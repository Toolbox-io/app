package ru.morozovit.android

import android.widget.RadioButton

@Suppress("MemberVisibilityCanBePrivate", "unused")
class RadioButtonController(private val radioButtons: MutableList<RadioButton> = mutableListOf()) {
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

    fun getChecked(): RadioButton? {
        for (rb in radioButtons) {
            if (rb.isChecked) return rb
        }
        return null
    }

    fun getCheckedIndex(): Int = radioButtons.indexOf(getChecked())
}