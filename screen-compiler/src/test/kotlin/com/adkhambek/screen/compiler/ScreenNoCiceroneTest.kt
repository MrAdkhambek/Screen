package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenNoCiceroneTest {

    // ── @Screen without Cicerone on classpath ────────────────────────────────

    @Nested
    inner class NoCicerone {

        @Test
        fun `compiles successfully without Cicerone`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPluginNoCicerone(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected compilation to succeed without Cicerone. Output:\n${result.messages}"
            }
        }

        @Test
        fun `KEY is still generated without Cicerone`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                fun useKey(): String = MyFragment.KEY
                """,
            )
            val result = compileWithScreenPluginNoCicerone(source, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected KEY to be generated without Cicerone. Output:\n${result.messages}"
            }
        }

        @Test
        fun `KEY has correct value without Cicerone`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPluginNoCicerone(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("test.MyFragment")
            val companionClazz = result.classLoader.loadClass("test.MyFragment\$Companion")
            val keyField = try {
                companionClazz.getDeclaredField("KEY")
            } catch (_: NoSuchFieldException) {
                clazz.getDeclaredField("KEY")
            }
            keyField.isAccessible = true
            assertEquals("test.MyFragment", keyField.get(null))
        }

        @Test
        fun `arg property is still generated without Cicerone`() {
            val source = SourceFile.kotlin(
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
            val result = compileWithScreenPluginNoCicerone(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("test.ArgFragment")
            val getArg = clazz.declaredMethods.firstOrNull { it.name == "getArg" }
            assertNotNull(getArg) {
                "Expected arg getter to be generated without Cicerone. Methods: ${
                    clazz.declaredMethods.map { it.name }
                }"
            }
        }

        @Test
        fun `createScreen is NOT generated without Cicerone`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPluginNoCicerone(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val companionClazz = result.classLoader.loadClass("test.MyFragment\$Companion")
            val hasCreateScreen = companionClazz.declaredMethods.any {
                it.name == "createScreen"
            }
            assertFalse(hasCreateScreen) {
                "Expected createScreen to NOT be generated without Cicerone"
            }
        }

        @Test
        fun `referencing createScreen without Cicerone causes compilation error`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                fun create() = MyFragment.createScreen()
                """,
            )
            val result = compileWithScreenPluginNoCicerone(source, usage)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode) {
                "Expected unresolved reference for createScreen without Cicerone. Output:\n${result.messages}"
            }
        }

        @Test
        fun `diagnostics still work without Cicerone`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen

                @Screen
                class NotAFragment
                """,
            )
            val result = compileWithScreenPluginNoCicerone(source)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(
                result.messages.contains(
                    "@Screen can only be applied to classes that extend Fragment or DialogFragment."
                )
            ) {
                "Expected SCREEN_NOT_ON_FRAGMENT diagnostic without Cicerone, got:\n${result.messages}"
            }
        }
    }
}
