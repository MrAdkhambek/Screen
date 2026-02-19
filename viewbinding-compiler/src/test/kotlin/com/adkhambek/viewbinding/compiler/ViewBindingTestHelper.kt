package com.adkhambek.viewbinding.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

// ── Stub sources ────────────────────────────────────────────────────────────

val ViewStub = SourceFile.kotlin(
    "View.kt",
    """
    package android.view

    open class View
    """,
)

val FragmentStub = SourceFile.kotlin(
    "Fragment.kt",
    """
    package androidx.fragment.app

    open class Fragment(contentLayoutId: Int = 0) {
        fun requireView(): android.view.View = android.view.View()
    }
    """,
)

val DialogFragmentStub = SourceFile.kotlin(
    "DialogFragment.kt",
    """
    package androidx.fragment.app

    open class DialogFragment(contentLayoutId: Int = 0) : Fragment(contentLayoutId)
    """,
)

val ParcelableStub = SourceFile.kotlin(
    "Parcelable.kt",
    """
    package android.os

    interface Parcelable
    """,
)

val RStub = SourceFile.kotlin(
    "R.kt",
    """
    package com.example

    object R {
        object layout {
            const val fragment_sample: Int = 0x7f0a0001
            const val activity_main: Int = 0x7f0a0002
        }
    }
    """,
)

val FragmentSampleBindingStub = SourceFile.java(
    "FragmentSampleBinding.java",
    """
    package com.example.databinding;

    public class FragmentSampleBinding {
        public static FragmentSampleBinding bind(android.view.View view) {
            return new FragmentSampleBinding();
        }
    }
    """,
)

val ActivityMainBindingStub = SourceFile.java(
    "ActivityMainBinding.java",
    """
    package com.example.databinding;

    public class ActivityMainBinding {
        public static ActivityMainBinding bind(android.view.View view) {
            return new ActivityMainBinding();
        }
    }
    """,
)

val AllStubs = listOf(
    ViewStub,
    FragmentStub,
    DialogFragmentStub,
    ParcelableStub,
    RStub,
    FragmentSampleBindingStub,
    ActivityMainBindingStub,
)

// ── Compilation helper ──────────────────────────────────────────────────────

@OptIn(ExperimentalCompilerApi::class)
fun compileWithViewBindingPlugin(
    vararg sources: SourceFile,
    namespace: String = "com.example",
): JvmCompilationResult {
    return KotlinCompilation().apply {
        this.sources = AllStubs + sources.toList()
        compilerPluginRegistrars = listOf(ViewBindingCompilerPluginRegistrar())
        commandLineProcessors = listOf(ViewBindingCommandLineProcessor())
        pluginOptions = listOf(
            PluginOption("com.adkhambek.viewbinding.compiler", "namespace", namespace),
        )
        languageVersion = "2.0"
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()
}
