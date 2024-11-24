package ru.morozovit.ultimatesecurity.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.databinding.ApkExtractorBinding

class ApkExtractorFragment: Fragment() {
    private lateinit var binding: ApkExtractorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ApkExtractorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // margin fixes for search
        binding.apkextractorSearchView.findViewById<View>(com.google.android.material.R.id
            .open_search_view_status_bar_spacer).visibility = GONE
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.apkextractorSearchView
        ) { _, _ -> CONSUMED }
        binding.apkextractorSearch.updateLayoutParams<MarginLayoutParams> {
            topMargin = 0
            bottomMargin = resources.getDimensionPixelSize(R.dimen.padding_small)
        }

        // TODO search implementation
    }
}