package ru.morozovit.ultimatesecurity.ui.tools

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment

class ApkExtractorFragment: Fragment() {
//    private lateinit var binding: ApkExtractorBinding
//    private lateinit var backCb: OnBackPressedCallback
//
//    private var filtersOpened = false

    @Composable
    @Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
    fun APKExtractorScreen() {
        MaterialTheme {
            Column {
                Search()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Search() {
        val textFieldState = rememberTextFieldState()
        var expanded by rememberSaveable { mutableStateOf(false) }

        Box(
            Modifier
                .fillMaxSize()
                .semantics { isTraversalGroup = true }) {
            SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        onSearch = { expanded = false },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder = { Text("Hinted search text") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                        onQueryChange = {},
                        query = "",
                        enabled = true
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    repeat(4) { idx ->
                        val resultText = "Suggestion $idx"
                        ListItem(
                            headlineContent = { Text(resultText) },
                            supportingContent = { Text("Additional info") },
                            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier =
                            Modifier
                                .clickable {
                                    textFieldState.setTextAndPlaceCursorAtEnd(resultText)
                                    expanded = false
                                }
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 72.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { traversalIndex = 1f },
            ) {
                val list = List(100) { "Text $it" }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        binding = ApkExtractorBinding.inflate(inflater, container, false)
//        return binding.root
        return ComposeView(requireContext()).apply {
            setContent {
                APKExtractorScreen()
            }
        }
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // margin fixes for search
//        binding.apkextractorSearchView.findViewById<View>(com.google.android.material.R.id
//            .open_search_view_status_bar_spacer).visibility = GONE
//        ViewCompat.setOnApplyWindowInsetsListener(
//            binding.apkextractorSearchView
//        ) { _, _ -> CONSUMED }
//        binding.apkextractorSearch.updateLayoutParams<MarginLayoutParams> {
//            topMargin = 0
//            bottomMargin = resources.getDimensionPixelSize(R.dimen.padding)
//        }
//        requireActivity().onBackPressedDispatcher.addCallback {
//            backCb = this
//            binding.apkextractorSearchView.apply {
//                handleBackInvoked()
//            }
//        }
//        // TODO search implementation
//        async {
//            val appList = packageManager.getInstalledPackages(0).toMutableList()
//            binding.root.post {
//                binding.apkextractorApps.layoutManager = LinearLayoutManager(requireActivity())
//                binding.apkextractorApps.adapter = AppAdapter(appList)
//            }
//        }
//
//        binding.apkextractorFilters.setOnClickListener {
//            filtersOpened = !filtersOpened
//            if (filtersOpened) {
//                binding.apkextractorFiltersOpenclose
//                    .animate()
//                    .rotation(180f)
//
//                binding.apkextractorFiltersContainer.apply {
//                    measure(
//                        View.MeasureSpec.makeMeasureSpec(
//                            width,
//                            View.MeasureSpec.EXACTLY
//                        ),
//                        View.MeasureSpec.makeMeasureSpec(
//                            0,
//                            View.MeasureSpec.UNSPECIFIED
//                        )
//                    )
//                    ValueAnimator.ofInt(0, measuredHeight).apply {
//                        addUpdateListener { animation ->
//                            updateLayoutParams {
//                                height = animation.animatedValue as Int
//                            }
//                        }
//                        start()
//                    }
//                }
//            } else {
//                binding.apkextractorFiltersOpenclose
//                    .animate()
//                    .rotation(0f)
//
//                ValueAnimator.ofInt(binding.apkextractorFiltersContainer.height, 0).apply {
//                    addUpdateListener { animation ->
//                        binding.apkextractorFiltersContainer.updateLayoutParams {
//                            height = animation.animatedValue as Int
//                        }
//                    }
//                    start()
//                }
//            }
//        }
//    }
//
//    inner class AppAdapter(private val appList: MutableList<PackageInfo>) :
//        RecyclerView.Adapter<AppAdapter.AppViewHolder>() {
//        inner class AppViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
//            private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
//            private val appName: TextView = itemView.findViewById(R.id.appName)
//            private val appPackage: TextView = itemView.findViewById(R.id.appPackage)
//
//            inner class AppBottomSheet: BottomSheet() {
//                private lateinit var binding: AppInfoBinding
//
//                override fun onCreateView(
//                    inflater: LayoutInflater,
//                    container: ViewGroup
//                ): View {
//                    binding = AppInfoBinding.inflate(inflater, container, false)
//                    return binding.root
//                }
//
//                override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//                    super.onViewCreated(view, savedInstanceState)
//                    // TODO Show app info
//                    //      Give the ability to extract APK
//                }
//            }
//
//            fun bind(packageInfo: PackageInfo) {
//                (itemView as ShimmerFrameLayout).startShimmerAnimation()
//                async {
//                    val icon = packageInfo.applicationInfo!!.loadIcon(packageManager)
//                    val label = packageInfo.applicationInfo!!.loadLabel(packageManager).toString()
//                    val packageName = packageInfo.packageName
//                    itemView.post {
//                        appIcon.setImageDrawable(icon)
//                        appName.text = label
//                        appPackage.text = packageName
//
//                        itemView.setOnClickListener {
//                            // TODO Open app info
//                        }
//                        (itemView as ShimmerFrameLayout).stopShimmerAnimation()
//                        appIcon.background = null
//                        appName.background = null
//                        appPackage.background = null
//                    }
//                }
//            }
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
//        = AppViewHolder(
//            layoutInflater.inflate(
//                R.layout.app_item_2,
//                parent,
//                false
//            )
//        )
//
//        override fun onBindViewHolder(holder: AppViewHolder, position: Int)
//        = holder.bind(appList[position])
//
//        override fun getItemCount() = appList.size
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        if (::backCb.isInitialized) backCb.remove()
//    }
}