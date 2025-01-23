package ru.morozovit.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// TODO remove when Shortcuts screen is rewritten
abstract class BottomSheet: BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return onCreateView(inflater, container)
    }
    abstract fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View

    final override fun getDialog() = super.getDialog() as BottomSheetDialog

    val behavior get() = dialog.behavior

    open fun configure(behavior1: BottomSheetBehavior<*>) {
        applyAll(behavior, behavior1) {
            state = BottomSheetBehavior.STATE_EXPANDED
            isFitToContents = true
            saveFlags = BottomSheetBehavior.SAVE_ALL
        }
        behavior1.addBottomSheetCallback {
            runCatching {
                behavior.state = it
            }
        }
    }

    open fun configure(behavior1: View) = configure(BottomSheetBehavior.from(behavior1))
}