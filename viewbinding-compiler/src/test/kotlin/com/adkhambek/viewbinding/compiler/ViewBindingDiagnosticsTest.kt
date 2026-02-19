package com.adkhambek.viewbinding.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViewBindingDiagnosticsTest {

    @Test
    fun `@Screen on non-Fragment class reports VIEW_BINDING_NOT_ON_FRAGMENT`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            import com.adkhambek.screen.Screen

            @Screen
            class NotAFragment
            """,
        )
        val result = compileWithViewBindingPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("Fragment")) {
            "Expected VIEW_BINDING_NOT_ON_FRAGMENT diagnostic, got:\n${result.messages}"
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
            class MyFragment : Fragment(com.example.R.layout.fragment_sample)
            """,
        )
        val result = compileWithViewBindingPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen on DialogFragment compiles successfully`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.DialogFragment

            @Screen
            class MyDialogFragment : DialogFragment(com.example.R.layout.fragment_sample)
            """,
        )
        val result = compileWithViewBindingPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed. Output:\n${result.messages}"
        }
    }
}
