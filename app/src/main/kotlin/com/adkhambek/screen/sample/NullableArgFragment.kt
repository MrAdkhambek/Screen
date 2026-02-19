package com.adkhambek.screen.sample

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.adkhambek.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data class Arg(
    val id: Long,
) : Parcelable

@Screen(arg = Arg::class, isNullable = true)
class NullableArgFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arg?.id
    }

}

@Screen
class A: DialogFragment()