package ru.morozovit.ultimatesecurity.ui.customization

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.morozovit.ultimatesecurity.Settings.Tiles.sleep
import ru.morozovit.ultimatesecurity.databinding.TilesBinding
import ru.morozovit.ultimatesecurity.services.tiles.SleepTile

class TilesFragment: Fragment() {
    private lateinit var binding: TilesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("Wakelock", "WakelockTimeout")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tilesSleepSw.setOnCheckedChangeListener { _, isChecked ->
            sleep = isChecked
            binding.tilesSleep.isEnabled = isChecked
            binding.tilesSleep.isChecked = true
        }


        binding.tilesSleep.isClickable = false
        binding.tilesSleep.isFocusable = false

        binding.tilesSleep.isChecked = SleepTile.instance?.enabled ?: true

        binding.root.setOnFocusChangeListener { _, _ ->
            binding.tilesSleep.isChecked = SleepTile.instance?.enabled ?: true
        }
    }
}