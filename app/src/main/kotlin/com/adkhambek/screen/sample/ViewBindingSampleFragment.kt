package com.adkhambek.screen.sample

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adkhambek.screen.Screen

@Screen
class ViewBindingSampleFragment : Fragment(R.layout.fragment_viewbinding_sample) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.label.text = "Hello from ViewBinding plugin!"
    }
}
