package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScreenDiagnosticsTest {

    @Test
    fun `@Screen on non-Fragment class reports SCREEN_NOT_ON_FRAGMENT`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            import com.adkhambek.screen.Screen

            @Screen
            class NotAFragment
            """,
        )
        val result = compileWithScreenPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("extend") || result.messages.contains("Fragment")) {
            "Expected SCREEN_NOT_ON_FRAGMENT diagnostic, got:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen with non-Parcelable arg reports SCREEN_ARG_NOT_PARCELABLE`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            class NotParcelable

            @Screen(arg = NotParcelable::class)
            class MyFragment : Fragment()
            """,
        )
        val result = compileWithScreenPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("Parcelable")) {
            "Expected SCREEN_ARG_NOT_PARCELABLE diagnostic, got:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen on Fragment subclass compiles successfully`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            @Screen
            class MyFragment : Fragment()
            """,
        )
        val result = compileWithScreenPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `@Screen on DialogFragment compiles successfully`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.DialogFragment

            @Screen
            class MyDialogFragment : DialogFragment()
            """,
        )
        val result = compileWithScreenPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}
