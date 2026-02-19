package com.adkhambek.screen.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.adkhambek.screen.Screen

@Screen(arg = Arg::class, isNullable = false)
class SampleFragment : Fragment(R.layout.fragment_sample) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textView.setText(arg.id.toString())
        Log.d("TTT", (binding === binding).toString())
        Toast.makeText(requireContext(), arg.id.toString(), Toast.LENGTH_LONG).show()
    }
}
