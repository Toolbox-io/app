package ru.morozovit.ultimatesecurity.ui.customization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.fragment.app.Fragment
import ru.morozovit.ultimatesecurity.databinding.ShortcutsBinding

class ShortcutsFragment: Fragment() {
    private lateinit var binding: ShortcutsBinding

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
        binding.shortcutsFilesCard.setOnClickListener {
            val coords = intArrayOf(0, 0)
            binding.shortcutsFilesCard.getLocationInWindow(coords)
            val centerX = (binding.root.width / 2) - binding.shortcutsFilesCard.width / 2
            val centerY = (binding.root.height / 2) - binding.shortcutsFilesCard.height / 2

            val translationX = centerX - coords[0]
            val translationY = centerY - coords[1]

            binding.shortcutsFilesCard.animate()
                .scaleX(4f)
                .scaleY(4f)
                .translationX(translationX.toFloat())
                .translationY(translationY.toFloat())
                .setDuration(500)
                .setInterpolator(AccelerateInterpolator())
                .start()
        }
    }
}