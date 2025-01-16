package ru.morozovit.ultimatesecurity.ui.customization.shortcuts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.morozovit.android.BottomSheet
import ru.morozovit.android.Mipmap
import ru.morozovit.android.RadioButtonController
import ru.morozovit.android.TextButton
import ru.morozovit.android.getSystemService
import ru.morozovit.android.invoke
import ru.morozovit.android.launchFiles
import ru.morozovit.android.plus
import ru.morozovit.android.previewUtils
import ru.morozovit.android.supportFragmentManager
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.Shortcuts.files
import ru.morozovit.ultimatesecurity.Settings.Shortcuts.files_choice
import ru.morozovit.ultimatesecurity.databinding.FilesShortcutBinding
import ru.morozovit.ultimatesecurity.databinding.ShortcutsBinding
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import ru.morozovit.ultimatesecurity.ui.customization.shortcuts.ShortcutsFragment.FilesShortcutBottomSheet.Companion.FILES_ICON_CREATED

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
@PhonePreview
fun ShortcutsScreen() {
    AppTheme {
        WindowInsetsHandler {
            val (_, runOrNoop, _, valueOrTrue) = previewUtils()
            val context = LocalContext()
            val scope = rememberCoroutineScope()

            var removeIconVisible by remember {
                mutableStateOf(valueOrTrue { files })
            }

            val sheetState = rememberModalBottomSheetState()
            var showFilesBottomSheet by remember {
                mutableStateOf(valueOrTrue { false })
            }

            fun hideFilesBottomSheet() {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showFilesBottomSheet = false
                    }
                }
            }

            if (showFilesBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showFilesBottomSheet = false
                    },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.ht_add_sc),
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center
                        )

                        // TODO finish
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            RadioButtonController {
                                val (asShortcut, asApp) = createIds()

                                val asShortcutChecked = remember { mutableStateOf(false) }
                                val asAppChecked = remember { mutableStateOf(true) }

                                @Composable
                                fun Option(
                                    selected: MutableState<Boolean>,
                                    onClick: () -> Unit = { selected.value = true },
                                    painter: Painter,
                                    contentDescription: String,
                                    modifier: Modifier = Modifier
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        RadioButton(
                                            selected = selected.value,
                                            onClick = onClick
                                        )
                                        Box(
                                            Modifier
                                                .background(
                                                    if (isSystemInDarkTheme())
                                                        Color(0xFF1D1B20)
                                                    else
                                                        Color(0xFFF7F2F9)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .padding(10.dp)
                                        ) {
                                            Image(
                                                painter = painter,
                                                contentDescription = contentDescription,
                                                modifier = Modifier
                                                    .width(81.dp)
                                                    .height(110.dp)
                                                        + modifier
                                            )
                                        }
                                    }
                                }

                                Option(
                                    selected = asShortcutChecked,
                                    painter = painterResource(R.drawable.files_shortcut1),
                                    contentDescription = stringResource(R.string.asas),
                                )

                                Option(
                                    selected = asAppChecked,
                                    painter = painterResource(R.drawable.files_shortcut2),
                                    contentDescription = stringResource(R.string.asasa),
                                )

                                addRadioButtons(
                                    asShortcut to asShortcutChecked,
                                    asApp to asAppChecked,
                                    coroutineScope = rememberCoroutineScope()
                                )
                            }
                        }
                    }
                }
            }

            FlowRow(Modifier.padding(10.dp)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.padding(bottom = 5.dp)) {
                            Mipmap(
                                id = R.mipmap.files_icon,
                                contentDescription = stringResource(R.string.files),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .size(60.dp)
                            )
                        }
                        Text(stringResource(R.string.files))

                        TextButton(
                            onClick = { showFilesBottomSheet = true },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add)
                                )
                            }
                        ) {
                            Text(stringResource(R.string.add))
                        }

                        AnimatedVisibility(visible = removeIconVisible) {
                            TextButton(
                                onClick = {
                                    runOrNoop {
                                        files = false
                                        Toast.makeText(context, R.string.ir, LENGTH_SHORT).show()
                                    }
                                    removeIconVisible = false
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.ri)
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.ri))
                            }
                        }
                    }
                }
            }
        }
    }
}

// TODO rewrite in Jetpack Compose
class ShortcutsFragment: Fragment() {
    companion object {
        const val FILES_SHORTCUT = "files-shortcut"
    }
    private lateinit var binding: ShortcutsBinding

    class FilesShortcutBottomSheet: BottomSheet() {
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
            container: ViewGroup?
        ): View {
            binding = FilesShortcutBinding.inflate(inflater, container, false)
            return binding.root
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure(binding.shortcutsFilesBs)

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
                                .setIcon(android.graphics.drawable.Icon.createWithResource(context, R.mipmap.files_icon))
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