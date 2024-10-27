package ru.morozovit.ultimatesecurity.unlockprotection

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.ui.makeSwitchCard
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.intruderPhotoDir
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.intruderPhotoDirEnabled
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.intruderPhotoFromBackCam
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.intruderPhotoFromFrontCam
import ru.morozovit.ultimatesecurity.databinding.IntruderPhotoSettingsBinding
import ru.morozovit.ultimatesecurity.databinding.IntruderphotoBinding
import ru.morozovit.ultimatesecurity.fileExists
import java.util.Date

class IntruderPhotoSettingsActivity: AppCompatActivity() {
    private lateinit var binding: IntruderPhotoSettingsBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    data class IntruderPhoto(val drawables: Pair<Drawable, Drawable?>, val timestamp: Date, val name: String = "") {
        val is2 get() = drawables.second != null

        constructor(back: Drawable, timestamp: Date, name: String = ""): this(Pair(back, null), timestamp, name)
    }


    inner class IntruderPhotoViewHolder(val binding: IntruderphotoBinding): RecyclerView.ViewHolder(binding.root) {
        private lateinit var data: IntruderPhoto

        fun bind(data: IntruderPhoto) {
            this.data = data
            binding.upActionsIpPL.apply {
                setImageDrawable(data.drawables.first)
                visibility = VISIBLE
            }
            binding.upActionsIpPR.apply {
                if (data.is2) {
                    setImageDrawable(data.drawables.second)
                    visibility = VISIBLE
                    binding.upActionsIpPLabel.visibility = VISIBLE
                    binding.upActionsIpPGradient.visibility = VISIBLE
                } else {
                    visibility = GONE
                    binding.upActionsIpPLabel.visibility = GONE
                    binding.upActionsIpPGradient.visibility = GONE
                }
            }
        }
    }

    inner class IntruderPhotosAdapter(private var data: Array<IntruderPhoto>): RecyclerView.Adapter<IntruderPhotoViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ) = IntruderPhotoViewHolder(
            IntruderphotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(
            holder: IntruderPhotoViewHolder,
            position: Int
        ) = holder.bind(data[position])

        override fun getItemCount() = data.size

        @SuppressLint("NotifyDataSetChanged")
        fun filterList(data: Array<IntruderPhoto>) {
            this.data = data
            notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        savedInstanceState?.clear()
        binding = IntruderPhotoSettingsBinding.inflate(layoutInflater)
        activityLauncher = registerActivityForResult(this)
        setContentView(binding.root)

        binding.upActionsIpTb.setNavigationOnClickListener {
            onBackPressed()
        }

        // Main switch
        makeSwitchCard(binding.upActionsIpSwitchCard, binding.upActionsIpSwitch)
        binding.upActionsIpSwitch.isChecked = Settings.UnlockProtection.Actions.intruderPhoto
        binding.upActionsIpSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.UnlockProtection.Actions.intruderPhoto = isChecked
        }

        // Back cam switch
        binding.upActionsIpBackCam.isChecked = intruderPhotoFromBackCam
        binding.upActionsIpBackCam.setOnCheckedChangeListener { _, isChecked ->
            intruderPhotoFromBackCam = isChecked
        }

        // Front cam switch
        binding.upActionsIpFrontCam.isChecked = intruderPhotoFromFrontCam
        binding.upActionsIpFrontCam.setOnCheckedChangeListener { _, isChecked ->
            intruderPhotoFromFrontCam = isChecked
        }

        var allPhotos: MutableList<IntruderPhoto> = mutableListOf()
        var backPhotos: MutableList<IntruderPhoto> = mutableListOf()
        var frontPhotos: MutableList<IntruderPhoto> = mutableListOf()

        val tabListener = object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                try {
                    val adapter = binding.upActionsIpP.adapter as IntruderPhotosAdapter
                    when (tab!!.position) {
                        0 -> adapter.filterList(allPhotos.toTypedArray())
                        1 -> adapter.filterList(backPhotos.toTypedArray())
                        2 -> adapter.filterList(frontPhotos.toTypedArray())
                    }
                } catch (_: Exception) {}
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) = onTabSelected(tab)
        }

        fun loadAllPhotos() {
            Thread {
                binding.root.post {
                    binding.upActionsIpP.visibility = GONE
                    binding.upActionsIpS.visibility = VISIBLE
                    binding.upActionsIpP.adapter = null
                }
                if (intruderPhotoDir == "") {
                    // TODO implement
                }
                else if (intruderPhotoDirEnabled) {
                    val dir = DocumentFile.fromTreeUri(this, Uri.parse(intruderPhotoDir))!!
                    var fileTmp: DocumentFile? = dir.findFile("front")

                    val front = fileTmp ?: dir.createDirectory("front")!!
                    fileTmp = dir.findFile("back")
                    val back = fileTmp ?: dir.createDirectory("back")!!

                    val frontFiles = front.listFiles()
                    val frontDrawables = mutableListOf<IntruderPhoto>()
                    val backFiles = back.listFiles()
                    val backDrawables = mutableListOf<IntruderPhoto>()

                    val files = mutableSetOf<DocumentFile>()

                    val frontNames = mutableListOf<Pair<Int, String>>()
                    val backNames = mutableListOf<Pair<Int, String>>()

                    allPhotos = mutableListOf()
                    backPhotos = mutableListOf()
                    frontPhotos = mutableListOf()

                    for (index in frontFiles.indices) {
                        val it = frontFiles[index]
                        val stream = contentResolver.openInputStream(it.uri)
                        val drawable = Drawable.createFromStream(stream, null)!!
                        stream!!.close()
                        frontDrawables.add(
                            IntruderPhoto(
                                drawable,
                                Date(it.lastModified()),
                                it.name!!
                            )
                        )
                        files.add(it)
                        frontNames.add(Pair(index, it.name!!))
                    }
                    for (index in backFiles.indices) {
                        val it = backFiles[index]
                        val stream = contentResolver.openInputStream(it.uri)
                        val drawable = Drawable.createFromStream(stream, null)!!
                        stream!!.close()
                        backDrawables.add(
                            IntruderPhoto(
                                drawable,
                                Date(it.lastModified()),
                                it.name!!
                            )
                        )
                        files.add(it)
                        backNames.add(Pair(index, it.name!!))
                    }

                    files.forEach {
                        var bool1 = false
                        var frontIndex: Int? = null
                        var frontName: Pair<Int, String>? = null
                        for (frontName1 in frontNames) {
                            if (frontName1.second == it.name) {
                                bool1 = true
                                frontIndex = frontName1.first
                                frontName = frontName1
                                break
                            }
                        }
                        var bool2 = false
                        var backIndex: Int? = null
                        var backName: Pair<Int, String>? = null
                        for (backName1 in backNames) {
                            if (backName1.second == it.name) {
                                bool2 = true
                                backIndex = backName1.first
                                backName = backName1
                                break
                            }
                        }
                        if (bool1 && bool2 && frontIndex != null && backIndex != null && backName != null && frontName != null) {
                            val frontDrawable = frontDrawables[frontIndex]
                            val backDrawable = backDrawables[backIndex]

                            allPhotos.add(IntruderPhoto(
                                Pair(
                                    backDrawable.drawables.first,
                                    frontDrawable.drawables.first
                                ),
                                backDrawable.timestamp
                            ))
                            backPhotos.add(backDrawable)
                            frontPhotos.add(frontDrawable)
                            frontNames.remove(frontName)
                            backNames.remove(backName)
                        } else if (bool2 && backIndex != null) {
                            val backDrawable = backDrawables[backIndex]
                            allPhotos.add(IntruderPhoto(backDrawable.drawables.first, backDrawable.timestamp))
                            backPhotos.add(backDrawable)
                            backNames.remove(backName)
                        } else if (bool1 && frontIndex != null) {
                            val frontDrawable = frontDrawables[frontIndex]
                            allPhotos.add(IntruderPhoto(frontDrawable.drawables.first, frontDrawable.timestamp))
                            frontPhotos.add(frontDrawable)
                            frontNames.remove(frontName)
                        }
                    }

                    allPhotos.sortByDescending { it.timestamp }
                    backPhotos.sortByDescending { it.timestamp }
                    frontPhotos.sortByDescending { it.timestamp }

                    binding.upActionsIpP.post {
                        binding.upActionsIpP.adapter = IntruderPhotosAdapter(allPhotos.toTypedArray())
                    }
                }
                binding.root.post {
                    binding.upActionsIpS.visibility = GONE
                    binding.upActionsIpP.visibility = VISIBLE
                    binding.upActionsIpP.layoutManager =
                        androidx.recyclerview.widget.GridLayoutManager(this, 3)
                    binding.upActionsIpTabs.apply {
                        tabListener.onTabSelected(getTabAt(selectedTabPosition))
                    }
                }
            }.start()
        }

        var listener = true
        var listener2 = true

        fun selectPhotoDir() {
            val intent = Intent(ACTION_OPEN_DOCUMENT_TREE)
            activityLauncher.launch(intent) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.also {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        intruderPhotoDir = "$it"
                        listener = false
                        binding.upActionsIpI1Sw.isChecked = true
                        listener = true
                    }
                    loadAllPhotos()
                }
            }
        }

        // External photo folder
        binding.upActionsIpI1Sw.isChecked = intruderPhotoDirEnabled && intruderPhotoDir != ""
        loadAllPhotos()
        binding.upActionsIpI1Sw.setOnCheckedChangeListener { v, isChecked ->
            if (listener2) {
                if (isChecked) {
                    if (intruderPhotoDir == "" || !fileExists(intruderPhotoDir)) {
                        listener2 = false
                        v.isChecked = false
                        selectPhotoDir()
                        v.isChecked = true
                        listener2 = true
                    }
                }
                loadAllPhotos()
                intruderPhotoDirEnabled = isChecked
            }
        }
        binding.upActionsIpI1Hc.setOnClickListener {
            if (listener) {
                selectPhotoDir()
            }
        }
        binding.upActionsIpP.adapter = IntruderPhotosAdapter(emptyArray())

        // Tabs
        binding.upActionsIpTabs.addOnTabSelectedListener(tabListener)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        setResult(if (binding.upActionsIpSwitch.isChecked) 1 else 2)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.clear()
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.clear()
        super.onRestoreInstanceState(savedInstanceState)
    }
}