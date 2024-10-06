package ru.morozovit.ultimatesecurity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.morozovit.ultimatesecurity.databinding.SettingsBinding

class SettingsFragment : Fragment() {
    private lateinit var binding: SettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
}