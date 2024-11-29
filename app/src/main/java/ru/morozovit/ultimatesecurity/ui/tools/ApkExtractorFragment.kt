package ru.morozovit.ultimatesecurity.ui.tools

import android.animation.ValueAnimator
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import ru.morozovit.android.async
import ru.morozovit.android.packageManager
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.databinding.ApkExtractorBinding

class ApkExtractorFragment: Fragment() {
    private lateinit var binding: ApkExtractorBinding
    private lateinit var backCb: OnBackPressedCallback

    private var filtersOpened = false

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
            bottomMargin = resources.getDimensionPixelSize(R.dimen.padding)
        }
        requireActivity().onBackPressedDispatcher.addCallback {
            backCb = this
            binding.apkextractorSearchView.apply {
                handleBackInvoked()
            }
        }
        // TODO search implementation
        async {
            val appList = packageManager.getInstalledPackages(0).toMutableList()
            binding.root.post {
                binding.apkextractorApps.layoutManager = LinearLayoutManager(requireActivity())
                binding.apkextractorApps.adapter = AppAdapter(appList)
            }
        }

        binding.apkextractorFilters.setOnClickListener {
            filtersOpened = !filtersOpened
            if (filtersOpened) {
                binding.apkextractorFiltersOpenclose
                    .animate()
                    .rotation(180f)

                binding.apkextractorFiltersContainer.apply {
                    measure(
                        View.MeasureSpec.makeMeasureSpec(
                            width,
                            View.MeasureSpec.EXACTLY
                        ),
                        View.MeasureSpec.makeMeasureSpec(
                            0,
                            View.MeasureSpec.UNSPECIFIED
                        )
                    )
                    ValueAnimator.ofInt(0, measuredHeight).apply {
                        addUpdateListener { animation ->
                            updateLayoutParams {
                                height = animation.animatedValue as Int
                            }
                        }
                        start()
                    }
                }
            } else {
                binding.apkextractorFiltersOpenclose
                    .animate()
                    .rotation(0f)

                ValueAnimator.ofInt(binding.apkextractorFiltersContainer.height, 0).apply {
                    addUpdateListener { animation ->
                        binding.apkextractorFiltersContainer.updateLayoutParams {
                            height = animation.animatedValue as Int
                        }
                    }
                    start()
                }
            }
        }
    }

    inner class AppAdapter(private val appList: MutableList<PackageInfo>) :
        RecyclerView.Adapter<AppAdapter.AppViewHolder>() {
        inner class AppViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
            private val appName: TextView = itemView.findViewById(R.id.appName)
            private val appPackage: TextView = itemView.findViewById(R.id.appPackage)

            fun bind(packageInfo: PackageInfo) {
                (itemView as ShimmerFrameLayout).startShimmerAnimation()
                async {
                    val icon = packageInfo.applicationInfo!!.loadIcon(packageManager)
                    val label = packageInfo.applicationInfo!!.loadLabel(packageManager).toString()
                    val packageName = packageInfo.packageName
                    itemView.post {
                        appIcon.setImageDrawable(icon)
                        appName.text = label
                        appPackage.text = packageName

                        itemView.setOnClickListener {
                            // TODO Open app info
                        }
                        (itemView as ShimmerFrameLayout).stopShimmerAnimation()
                        appIcon.background = null
                        appName.background = null
                        appPackage.background = null
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
        = AppViewHolder(
            layoutInflater.inflate(
                R.layout.app_item_2,
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: AppViewHolder, position: Int)
        = holder.bind(appList[position])

        override fun getItemCount() = appList.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::backCb.isInitialized) backCb.remove()
    }
}