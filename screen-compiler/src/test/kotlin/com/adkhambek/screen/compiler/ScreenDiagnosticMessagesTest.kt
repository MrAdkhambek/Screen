package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenDiagnosticMessagesTest {

    // ── SCREEN_NOT_ON_FRAGMENT error message text ───────────────────────────

    @Nested
    inner class NotOnFragmentMessage {

        @Test
        fun `plain class reports exact SCREEN_NOT_ON_FRAGMENT message`() {
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
            assertTrue(
                result.messages.contains(
                    "@Screen can only be applied to classes that extend Fragment or DialogFragment."
                )
            ) {
                "Expected exact SCREEN_NOT_ON_FRAGMENT error message, got:\n${result.messages}"
            }
        }

        @Test
        fun `data class reports exact SCREEN_NOT_ON_FRAGMENT message`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen

                @Screen
                data class NotAFragment(val x: Int)
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(
                result.messages.contains(
                    "@Screen can only be applied to classes that extend Fragment or DialogFragment."
                )
            ) {
                "Expected exact SCREEN_NOT_ON_FRAGMENT error message, got:\n${result.messages}"
            }
        }

        @Test
        fun `object reports exact SCREEN_NOT_ON_FRAGMENT message`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen

                @Screen
                object NotAFragment
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(
                result.messages.contains(
                    "@Screen can only be applied to classes that extend Fragment or DialogFragment."
                )
            ) {
                "Expected exact SCREEN_NOT_ON_FRAGMENT error message, got:\n${result.messages}"
            }
        }

        @Test
        fun `interface reports exact SCREEN_NOT_ON_FRAGMENT message`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen

                @Screen
                interface NotAFragment
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(
                result.messages.contains(
                    "@Screen can only be applied to classes that extend Fragment or DialogFragment."
                )
            ) {
                "Expected exact SCREEN_NOT_ON_FRAGMENT error message, got:\n${result.messages}"
            }
        }
    }

    // ── SCREEN_ARG_NOT_PARCELABLE error message text ────────────────────────

    @Nested
    inner class ArgNotParcelableMessage {

        @Test
        fun `plain class arg reports exact SCREEN_ARG_NOT_PARCELABLE message`() {
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
            assertTrue(
                result.messages.contains(
                    "@Screen 'arg' class must implement android.os.Parcelable."
                )
            ) {
                "Expected exact SCREEN_ARG_NOT_PARCELABLE error message, got:\n${result.messages}"
            }
        }

        @Test
        fun `data class without Parcelable reports exact SCREEN_ARG_NOT_PARCELABLE message`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                data class MyArgs(val id: Int)

                @Screen(arg = MyArgs::class)
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(
                result.messages.contains(
                    "@Screen 'arg' class must implement android.os.Parcelable."
                )
            ) {
                "Expected exact SCREEN_ARG_NOT_PARCELABLE error message, got:\n${result.messages}"
            }
        }

        @Test
        fun `interface chain not extending Parcelable reports exact message`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                interface Routes
                sealed interface MessageRoutes : Routes

                @Screen(arg = MessageRoutes::class)
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(
                result.messages.contains(
                    "@Screen 'arg' class must implement android.os.Parcelable."
                )
            ) {
                "Expected exact SCREEN_ARG_NOT_PARCELABLE error message, got:\n${result.messages}"
            }
        }
    }
}
