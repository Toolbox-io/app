package ru.morozovit.ultimatesecurity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.morozovit.ultimatesecurity.databinding.HomeBinding

class HomeFragment : Fragment() {
    private lateinit var binding: HomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HomeBinding.inflate(inflater, container, false)
        return binding.root
    }
}