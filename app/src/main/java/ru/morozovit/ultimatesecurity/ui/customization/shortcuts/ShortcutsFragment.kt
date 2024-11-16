package ru.morozovit.ultimatesecurity.ui.customization.shortcuts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.SAVE_ALL
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.android.getSystemService
import ru.morozovit.android.launchFiles
import ru.morozovit.android.supportFragmentManager
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.Shortcuts.files
import ru.morozovit.ultimatesecurity.Settings.Shortcuts.files_choice
import ru.morozovit.ultimatesecurity.databinding.FilesShortcutBinding
import ru.morozovit.ultimatesecurity.databinding.ShortcutsBinding
import ru.morozovit.ultimatesecurity.ui.customization.shortcuts.ShortcutsFragment.FilesShortcutBottomSheet.Companion.FILES_ICON_CREATED

class ShortcutsFragment: Fragment() {
    companion object {
        @Suppress("unused")
        const val FILES_SHORTCUT = "files-shortcut"
    }
    private lateinit var binding: ShortcutsBinding

    class FilesShortcutBottomSheet: BottomSheetDialogFragment() {
        private lateinit var binding: FilesShortcutBinding
        private var callback: ((Int) -> Unit)? = null

        companion object {
            const val NO_RESULT = 0
            const val FILES_ICON_CREATED = 1
        }

        data class RadiobuttonData(
            val first: LinearLayout,
            val second: RadioButton,
            var clickListener: ((View) -> Unit)? = null
        )

        fun show(fragmentManager: FragmentManager, callback: ((Int) -> Unit)? = null) {
            show(fragmentManager, "")
            this.callback = callback
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FilesShortcutBinding.inflate(inflater, container, false)
            return binding.root
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val behavior = (dialog as BottomSheetDialog).behavior
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isFitToContents = true
            behavior.saveFlags = SAVE_ALL
            val behavior1 = BottomSheetBehavior.from(binding.shortcutsFilesBs)
            behavior1.state = BottomSheetBehavior.STATE_EXPANDED
            behavior1.isFitToContents = true
            behavior1.saveFlags = SAVE_ALL

            val rb = arrayOf(
                RadiobuttonData(binding.shortcutsFilesBsO1, binding.shortcutsFilesBsO1R),
                RadiobuttonData(binding.shortcutsFilesBsO2, binding.shortcutsFilesBsO2R)
            )

            rb.forEach { p ->
                p.first.setOnClickListener {
                    p.second.performClick()
                }
            }

            rb.forEach { p ->
                val l = { _: View ->
                    rb.forEach { other ->
                        if (other != p) {
                            other.second.isChecked = false
                        }
                    }
                    p.second.isChecked = true
                }
                p.first.setOnClickListener(l)
                p.clickListener = l
                p.second.setOnTouchListener { _, event ->
                    if (event.action == ACTION_DOWN) {
                        p.first.isPressed = true
                    } else if (event.action == ACTION_UP || event.action == ACTION_OUTSIDE) {
                        p.first.isPressed = false
                    }
                    if (event.action == ACTION_UP) l(p.second)
                    false
                }
            }

            fun getSelectedOption(): Int {
                rb.forEachIndexed { index, it ->
                    if (it.second.isChecked) {
                        return index
                    }
                }
                return -1
            }

            binding.shortcutsFilesBsContinue.setOnClickListener l@ {
                val option = getSelectedOption()
                processChoice(option)
                if (binding.shortcutsFilesBsRc.isChecked) {
                    Toast.makeText(requireContext(), R.string.cr, LENGTH_SHORT).show()
                    files_choice = option
                }
            }


            if (files_choice != -1) {
                binding.shortcutsFilesBsRc.isChecked = true
                val b = rb[files_choice]
                b.clickListener?.invoke(b.second)
            } else {
                files_choice = -1
            }
        }

        fun processChoice(choice: Int, context: Context = requireContext()): Int {
            var ret = NO_RESULT
            when (choice) {
                0 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val shortcutManager = context.getSystemService(ShortcutManager::class)!!
                        if (shortcutManager.isRequestPinShortcutSupported) {
                            val shortcutInfo = ShortcutInfo.Builder(
                                context, FILES_SHORTCUT
                            )
                                .setShortLabel(context.resources.getString(R.string.files))
                                .setLongLabel(context.resources.getString(R.string.files))
                                .setIcon(Icon.createWithResource(context, R.mipmap.files_icon))
                                .setIntent(Intent(context, FilesShortcut::class.java).apply {
                                    action = ACTION_MAIN
                                })
                                .build()
                            shortcutManager.requestPinShortcut(shortcutInfo, null)
                        }
                    }
                    dismiss()
                }

                1 -> {
                    files = true
                    Toast.makeText(context, R.string.fsh, LENGTH_SHORT)
                        .show()
                    dismiss()
                    ret = FILES_ICON_CREATED
                }
                else -> {
                    Toast.makeText(context, R.string.smthwentwrong, LENGTH_SHORT)
                        .show()
                }
            }
            callback?.invoke(ret)
            return ret
        }

        override fun dismiss() {
            try {
                super.dismiss()
            } catch (_: Exception) {}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ShortcutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.shortcutsFiles.isClickable = true
        binding.shortcutsFiles.isFocusable = true
        binding.shortcutsFiles.setOnClickListener {
            with(binding.shortcutsFilesCard) {
                // TODO make it work
                /* val coords = intArrayOf(0, 0)
                val x = coords[0]
                val y = coords[1]
                getLocationInWindow(coords)
                val centerX = (binding.root.width / 2) - width / 2
                val centerY = (binding.root.height / 2) - height / 2

                val translationX = centerX - x
                val translationY = centerY - y

                val clone = createBlankClone()
                val parent = parentViewGroup
                val pos = pos

                val layoutWidth = layoutParams.width
                val layoutHeight = layoutParams.height
                removeSelf()
                parent.addView(clone, pos)
                binding.root.addView(this)
                updateLayoutParams<AbsoluteLayout.LayoutParams> {
                    this.x = x
                    this.y = y
                    this.width = layoutWidth
                    this.height = layoutHeight
                }

                animate()
                    .scaleX(4f)
                    .scaleY(4f)
                    .translationX(translationX.toFloat())
                    .translationY(translationY.toFloat())
                    .setDuration(500)
                    .setInterpolator(AccelerateInterpolator())
                    .start() */
                if (!launchFiles()) {
                    Snackbar.make(binding.root, R.string.failed_to_launch_files, LENGTH_SHORT).show()
                }
            }
        }
        val pm = requireActivity().packageManager
        try {
            binding.shortcutsFiles.setImageDrawable(
                pm.getApplicationIcon("com.android.documentsui")
            )
            binding.shortcutsFilesLabel.text = pm.getApplicationLabel(
                pm.getApplicationInfo("com.android.documentsui", 0)
            )
        } catch (e: NameNotFoundException) {
            Log.e("Shortcuts", "${e::class.qualifiedName}: ${e.message}")
            binding.shortcutsFiles.setImageResource(R.mipmap.files_icon)
            binding.shortcutsFilesLabel.text = getString(R.string.files)
        }

        fun processResult(result: Int) {
            if (result == FILES_ICON_CREATED) {
                binding.shortcutsFilesRi.visibility = VISIBLE
            }
        }

        binding.shortcutsFilesAdd.setOnClickListener {
            val sheet = FilesShortcutBottomSheet()
            if (files_choice == -1) {
                sheet.show(supportFragmentManager, ::processResult)
            } else {
                processResult(sheet.processChoice(files_choice, requireActivity()))
            }
        }

        binding.shortcutsFilesAdd.setOnLongClickListener {
            FilesShortcutBottomSheet().show(supportFragmentManager, ::processResult)
            true
        }

        binding.shortcutsFilesRi.setOnClickListener {
            files = false
            Toast.makeText(requireActivity(), R.string.ir, LENGTH_SHORT).show()
            binding.shortcutsFilesRi.visibility = GONE
        }

        binding.shortcutsFilesRi.visibility = if (files) VISIBLE else GONE
    }
}