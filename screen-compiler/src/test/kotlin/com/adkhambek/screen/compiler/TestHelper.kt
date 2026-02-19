package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

// ── Stub sources ────────────────────────────────────────────────────────────

val FragmentStub = SourceFile.kotlin(
    "Fragment.kt",
    """
    package androidx.fragment.app

    open class Fragment {
        private var mArguments: android.os.Bundle? = null
        fun getArguments(): android.os.Bundle? = mArguments
        fun setArguments(args: android.os.Bundle?) { mArguments = args }
    }
    """,
)

val DialogFragmentStub = SourceFile.kotlin(
    "DialogFragment.kt",
    """
    package androidx.fragment.app

    open class DialogFragment : Fragment()
    """,
)

val ParcelableStub = SourceFile.kotlin(
    "Parcelable.kt",
    """
    package android.os

    interface Parcelable
    """,
)

val BundleStub = SourceFile.kotlin(
    "Bundle.kt",
    """
    package android.os

    open class Bundle(capacity: Int = 0) {
        private val map = mutableMapOf<String, Any?>()
        constructor() : this(0)
        fun putParcelable(key: String, value: Parcelable?) { map[key] = value }
        fun <T : Parcelable> getParcelable(key: String, clazz: Class<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return map[key] as? T
        }
    }
    """,
)

val BundleCompatStub = SourceFile.java(
    "BundleCompat.java",
    """
    package androidx.core.os;

    public final class BundleCompat {
        private BundleCompat() {}

        @SuppressWarnings("unchecked")
        public static <T extends android.os.Parcelable> T getParcelable(
                android.os.Bundle bundle, String key, java.lang.Class<T> clazz) {
            return bundle.getParcelable(key, clazz);
        }
    }
    """,
)

val FragmentScreenStub = SourceFile.kotlin(
    "FragmentScreen.kt",
    """
    package com.github.terrakok.cicerone.androidx

    fun interface Creator<in A, out R> {
        fun create(argument: A): R
    }

    class FragmentScreen(
        val screenKey: String?,
        val clearContainer: Boolean,
        private val creator: Creator<androidx.fragment.app.FragmentFactory, androidx.fragment.app.Fragment>,
    ) {
        companion object {
            operator fun invoke(
                key: String? = null,
                clearContainer: Boolean = true,
                fragmentCreator: Creator<androidx.fragment.app.FragmentFactory, androidx.fragment.app.Fragment>,
            ): FragmentScreen = FragmentScreen(key, clearContainer, fragmentCreator)
        }
    }
    """,
)

val FragmentFactoryStub = SourceFile.kotlin(
    "FragmentFactory.kt",
    """
    package androidx.fragment.app

    open class FragmentFactory {
        open fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return Class.forName(className, true, classLoader).getDeclaredConstructor().newInstance() as Fragment
        }
    }
    """,
)

val AllStubs = listOf(
    FragmentStub,
    DialogFragmentStub,
    ParcelableStub,
    BundleStub,
    BundleCompatStub,
    FragmentScreenStub,
    FragmentFactoryStub,
)

// ── Compilation helper ──────────────────────────────────────────────────────

@OptIn(ExperimentalCompilerApi::class)
fun compileWithScreenPlugin(vararg sources: SourceFile): JvmCompilationResult {
    return KotlinCompilation().apply {
        this.sources = AllStubs + sources.toList()
        compilerPluginRegistrars = listOf(ScreenCompilerPluginRegistrar())
        commandLineProcessors = listOf(ScreenCommandLineProcessor())
        languageVersion = "2.0"
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()
}
