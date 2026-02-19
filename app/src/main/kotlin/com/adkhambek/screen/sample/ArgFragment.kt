package com.adkhambek.screen.sample

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adkhambek.screen.Screen

@Screen(arg = Arg::class, isNullable = false)
class ArgFragment : Fragment(R.layout.fragment_sample) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arg.id
    }

}
