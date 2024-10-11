package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.morozovit.ultimatesecurity.databinding.SelectAppsBinding
import kotlin.collections.set


class SelectAppsActivity: AppCompatActivity() {
    private lateinit var binding: SelectAppsBinding

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SelectAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val appList = packageManager.getInstalledPackages(0).toMutableList()
        binding.selappsRv.layoutManager = LinearLayoutManager(this)
        binding.selappsRv.adapter = AppAdapter(appList) // TODO fix error

        val listener: (View?) -> Unit = {
            saveChoice()
            setResult(RESULT_OK)
            finish()
        }

        // Listeners are the same for the FAB and the back button
        binding.selappsTb.setNavigationOnClickListener(listener)
        binding.selappsFab.setOnClickListener(listener)
        onBackPressedDispatcher.addCallback {
            listener.invoke(null)
        }

        binding.selappsTb.subtitle = String.format(resources.getString(R.string.sel), 0)

        binding.selappsTb.menu.findItem(R.id.selapps_clear).setOnMenuItemClickListener {
            val adapter = binding.selappsRv.adapter as AppAdapter
            val selectedApps = adapter.selectedApps.toMutableMap()

            for (app in selectedApps) {
                (binding.selappsRv.findViewHolderForAdapterPosition(app.key) // TODO fix error
                        as AppAdapter.AppViewHolder).appCheckbox.isChecked = false
            }
            true
        }
        binding.selappsTb.menu.findItem(R.id.selapps_selall).setOnMenuItemClickListener {
            val adapter = binding.selappsRv.adapter as AppAdapter

            for (i in 0 until adapter.itemCount) {
                val vh = binding.selappsRv.findViewHolderForAdapterPosition(i) as AppAdapter.AppViewHolder // TODO fix error
                if (!vh.appCheckbox.isChecked)
                    vh.appCheckbox.isChecked = true
            }
            true
        }

    }

    private fun saveChoice() {
        @Suppress("UNCHECKED_CAST")
        Settings.Applocker.apps = (binding.selappsRv.adapter as AppAdapter).selectedAppsSet as Set<String>
    }

    inner class AppAdapter(private val appList: MutableList<PackageInfo>) :
        RecyclerView.Adapter<AppAdapter.AppViewHolder>() {
        private val mSelectedApps = mutableMapOf<Int,AppEntry>()

        val selectedApps get() = mSelectedApps
        val selectedAppsSet get(): Set<String?> {
            val arr = arrayOfNulls<String>(selectedApps.size)
            var i = 0
            for (item in selectedApps) {
                arr[i] = item.value.packageName
                i++
            }
            return setOf(*arr)
        }

        init {
            val selectedAppList = Settings.Applocker.apps
            for (item in appList) {
                if (selectedAppList.contains(item.packageName)) {
                    selectedApps[appList.indexOf(item)] = AppEntry(
                        item.applicationInfo.loadLabel(packageManager).toString(),
                        item.packageName,
                    )
                }
            }
        }

        inner class AppViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
            private val appName: TextView = itemView.findViewById(R.id.appName)
            private val appPackage: TextView = itemView.findViewById(R.id.appPackage)
            val appCheckbox: CheckBox = itemView.findViewById(R.id.appCheckbox)

            fun bind(packageInfo: PackageInfo, index: Int) {
                val icon = packageInfo.applicationInfo.loadIcon(this@SelectAppsActivity.packageManager)
                val label = packageInfo.applicationInfo.loadLabel(this@SelectAppsActivity.packageManager).toString()
                val packageName = packageInfo.packageName

                appIcon.setImageDrawable(icon)
                appName.text = label
                appPackage.text = packageName

                appCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedApps[index] = AppEntry(
                            label,
                            packageName,
                        )
                    } else {
                        selectedApps.remove(index)
                    }
                    binding.selappsTb.subtitle = String.format(resources.getString(R.string.sel), selectedApps.size)
                }

                appCheckbox.isChecked = selectedApps.containsKey(index)

                itemView.setOnClickListener {
                    appCheckbox.isChecked = !appCheckbox.isChecked
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val inflater = LayoutInflater.from(this@SelectAppsActivity)
            val view = inflater.inflate(R.layout.app_item, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(appList[position], position)
        }

        override fun getItemCount() = appList.size
    }

    data class AppEntry(
        val name: CharSequence,
        val packageName: String,
    )


    override fun onPause() {
        super.onPause()
        saveChoice()
    }
}