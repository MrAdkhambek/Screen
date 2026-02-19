package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScreenDeclarationGenerationTest {

    @Test
    fun `@Screen generates KEY on companion object`() {
        val fragment = SourceFile.kotlin(
            "MyFragment.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            @Screen
            class MyFragment : Fragment()
            """,
        )
        val usage = SourceFile.kotlin(
            "Usage.kt",
            """
            fun useKey(): String = MyFragment.KEY
            """,
        )
        val result = compileWithScreenPlugin(fragment, usage)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected KEY to be accessible on companion. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen with arg generates arg property`() {
        val fragment = SourceFile.kotlin(
            "MyFragment.kt",
            """
            package test

            import com.adkhambek.screen.Screen
            import android.os.Parcelable
            import androidx.fragment.app.Fragment

            class MyArg : Parcelable

            @Screen(arg = MyArg::class)
            class MyFragment : Fragment()
            """,
        )
        // The arg property is private, so we can't access it from outside.
        // Just verify the compilation succeeds.
        val result = compileWithScreenPlugin(fragment)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed with arg. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen generates companion when class has none`() {
        val fragment = SourceFile.kotlin(
            "MyFragment.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            @Screen
            class NoCompanionFragment : Fragment()
            """,
        )
        val usage = SourceFile.kotlin(
            "Usage.kt",
            """
            fun useKey(): String = NoCompanionFragment.KEY
            """,
        )
        val result = compileWithScreenPlugin(fragment, usage)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected auto-generated companion with KEY. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen augments existing companion object`() {
        val fragment = SourceFile.kotlin(
            "MyFragment.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            @Screen
            class WithCompanionFragment : Fragment() {
                companion object {
                    const val EXTRA = "extra"
                }
            }
            """,
        )
        val usage = SourceFile.kotlin(
            "Usage.kt",
            """
            fun useKey(): String = WithCompanionFragment.KEY
            fun useExtra(): String = WithCompanionFragment.EXTRA
            """,
        )
        val result = compileWithScreenPlugin(fragment, usage)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected existing companion augmented with KEY. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen generates createScreen function`() {
        val fragment = SourceFile.kotlin(
            "MyFragment.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            @Screen
            class ScreenFragment : Fragment()
            """,
        )
        val usage = SourceFile.kotlin(
            "Usage.kt",
            """
            import com.github.terrakok.cicerone.androidx.FragmentScreen

            fun create(): FragmentScreen = ScreenFragment.createScreen()
            """,
        )
        val result = compileWithScreenPlugin(fragment, usage)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected createScreen() to be accessible. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen with arg generates createScreen with arg parameter`() {
        val fragment = SourceFile.kotlin(
            "MyFragment.kt",
            """
            package test

            import com.adkhambek.screen.Screen
            import android.os.Parcelable
            import androidx.fragment.app.Fragment

            class MyArg : Parcelable

            @Screen(arg = MyArg::class)
            class ArgFragment : Fragment()
            """,
        )
        val usage = SourceFile.kotlin(
            "Usage.kt",
            """
            package test

            import com.github.terrakok.cicerone.androidx.FragmentScreen

            fun create(arg: MyArg): FragmentScreen = ArgFragment.createScreen(arg)
            """,
        )
        val result = compileWithScreenPlugin(fragment, usage)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected createScreen(arg) to be accessible. Output:\n${result.messages}"
        }
    }
}
