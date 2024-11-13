package ru.morozovit.ultimatesecurity.ui.protection.unlockprotection.intruderphoto

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Window
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.databinding.IntruderPhotoActivityBinding
import java.io.File

class IntruderPhotoActivity: BaseActivity(false) {
    private lateinit var binding: IntruderPhotoActivityBinding

    companion object {
        var data: IntruderPhotoSettingsActivity.IntruderPhoto? = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        binding = IntruderPhotoActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photo = data!!
        binding.upActionsIpVI.setImageDrawable(photo.drawables.first)

        val menu = binding.upActionsIpVTb.menu
        menu.findItem(R.id.up_actions_ip_v_tb_d).setOnMenuItemClickListener {
            var file = File(filesDir.absolutePath + "/${photo.path}/${photo.name}")
            if (!file.delete()) {
                file = File(filesDir.absolutePath + "/${if (photo.path == "front") "back" else "front"}/${photo.name}")
                file.delete()
            }
            setResult(RESULT_FIRST_USER)
            finish()
            true
        }
        data = null
        binding.upActionsIpVTb.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        setResult(RESULT_OK)
        finishAfterTransition()
    }
}