package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenCreateScreenReturnTest {

    private fun getCompanionInstance(result: JvmCompilationResult, fqn: String): Any {
        val outerClazz = result.classLoader.loadClass(fqn)
        val companionField = outerClazz.getDeclaredField("Companion")
        companionField.isAccessible = true
        return companionField.get(null)
    }

    // ── createScreen() return value verification ────────────────────────────

    @Nested
    inner class ReturnValue {

        @Test
        fun `createScreen returns a FragmentScreen instance for no-arg fragment`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                @Screen
                class SimpleFragment : Fragment() {
                    companion object
                }

                fun callCreateScreen(): FragmentScreen = SimpleFragment.createScreen()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            // Invoke the helper function which calls createScreen() with defaults
            val helperClazz = result.classLoader.loadClass("test.MyFragmentKt")
            val callFn = helperClazz.getDeclaredMethod("callCreateScreen")
            val returnValue = callFn.invoke(null)

            val fragmentScreenClazz = result.classLoader.loadClass(
                "com.github.terrakok.cicerone.androidx.FragmentScreen"
            )

            assertNotNull(returnValue) { "Expected createScreen to return a non-null value" }
            assertTrue(fragmentScreenClazz.isInstance(returnValue)) {
                "Expected return value to be a FragmentScreen, got: ${returnValue!!::class.java.name}"
            }
        }

        @Test
        fun `createScreen return value has correct screenKey`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                @Screen
                class KeyFragment : Fragment() {
                    companion object
                }

                fun callCreateScreen(): FragmentScreen = KeyFragment.createScreen(key = "custom-key")
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val helperClazz = result.classLoader.loadClass("test.MyFragmentKt")
            val callFn = helperClazz.getDeclaredMethod("callCreateScreen")
            val returnValue = callFn.invoke(null)

            val fragmentScreenClazz = result.classLoader.loadClass(
                "com.github.terrakok.cicerone.androidx.FragmentScreen"
            )

            assertNotNull(returnValue)
            val getScreenKey = fragmentScreenClazz.getDeclaredMethod("getScreenKey")
            val screenKey = getScreenKey.invoke(returnValue)
            assertEquals("custom-key", screenKey) {
                "Expected FragmentScreen.screenKey to match the key parameter"
            }
        }

        @Test
        fun `createScreen return value has correct clearContainer flag`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                @Screen
                class ClearFragment : Fragment() {
                    companion object
                }

                fun callCreateScreen(): FragmentScreen =
                    ClearFragment.createScreen(clearContainer = false)
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val helperClazz = result.classLoader.loadClass("test.MyFragmentKt")
            val callFn = helperClazz.getDeclaredMethod("callCreateScreen")
            val returnValue = callFn.invoke(null)

            val fragmentScreenClazz = result.classLoader.loadClass(
                "com.github.terrakok.cicerone.androidx.FragmentScreen"
            )

            assertNotNull(returnValue)
            val getClearContainer = fragmentScreenClazz.getDeclaredMethod("getClearContainer")
            val clearContainer = getClearContainer.invoke(returnValue)
            assertEquals(false, clearContainer) {
                "Expected FragmentScreen.clearContainer to be false"
            }
        }

        @Test
        fun `createScreen with arg returns FragmentScreen`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                class MyArg : Parcelable

                @Screen(arg = MyArg::class)
                class ArgScreenFragment : Fragment() {
                    companion object
                }

                fun callCreateScreen(): FragmentScreen =
                    ArgScreenFragment.createScreen(MyArg())
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val helperClazz = result.classLoader.loadClass("test.MyFragmentKt")
            val callFn = helperClazz.getDeclaredMethod("callCreateScreen")
            val returnValue = callFn.invoke(null)

            val fragmentScreenClazz = result.classLoader.loadClass(
                "com.github.terrakok.cicerone.androidx.FragmentScreen"
            )

            assertNotNull(returnValue) { "Expected createScreen(arg) to return a non-null value" }
            assertTrue(fragmentScreenClazz.isInstance(returnValue)) {
                "Expected return value to be a FragmentScreen"
            }
        }
    }
}
