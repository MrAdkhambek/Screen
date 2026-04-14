package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenIrGenerationTest {

    private fun readKeyValue(result: JvmCompilationResult, fqn: String): String {
        val clazz = result.classLoader.loadClass(fqn)
        val companionClazz = try {
            result.classLoader.loadClass("$fqn\$Companion")
        } catch (_: ClassNotFoundException) {
            null
        }
        val keyField = if (companionClazz != null) {
            try {
                companionClazz.getDeclaredField("KEY")
            } catch (_: NoSuchFieldException) {
                clazz.getDeclaredField("KEY")
            }
        } else {
            clazz.getDeclaredField("KEY")
        }
        keyField.isAccessible = true
        return keyField.get(null) as String
    }

    private fun hasMethod(result: JvmCompilationResult, fqn: String, methodName: String): Boolean {
        val companionFqn = "$fqn\$Companion"
        val clazz = try {
            result.classLoader.loadClass(companionFqn)
        } catch (_: ClassNotFoundException) {
            result.classLoader.loadClass(fqn)
        }
        return clazz.declaredMethods.any { it.name == methodName }
    }

    // ── KEY value correctness ────────────────────────────────────────────────

    @Nested
    inner class KeyValues {

        @Test
        fun `KEY equals fully qualified class name`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertEquals("com.example.MyFragment", readKeyValue(result, "com.example.MyFragment"))
        }

        @Test
        fun `KEY for fragment without package equals simple class name`() {
            val source = SourceFile.kotlin(
                "RootFragment.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class RootFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertEquals("RootFragment", readKeyValue(result, "RootFragment"))
        }

        @Test
        fun `KEY for deeply nested package includes full package`() {
            val source = SourceFile.kotlin(
                "DeepFragment.kt",
                """
                package com.example.app.ui.home

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class HomeFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertEquals(
                "com.example.app.ui.home.HomeFragment",
                readKeyValue(result, "com.example.app.ui.home.HomeFragment"),
            )
        }

        @Test
        fun `multiple fragments in same package have distinct KEY values`() {
            val source = SourceFile.kotlin(
                "Fragments.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen class AlphaFragment : Fragment()
                @Screen class BetaFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            val keyA = readKeyValue(result, "com.example.AlphaFragment")
            val keyB = readKeyValue(result, "com.example.BetaFragment")
            assertNotEquals(keyA, keyB) { "Two different fragments must have distinct KEY values" }
            assertEquals("com.example.AlphaFragment", keyA)
            assertEquals("com.example.BetaFragment", keyB)
        }

        @Test
        fun `KEY is a String constant (not null)`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            val key = readKeyValue(result, "com.example.MyFragment")
            assertNotNull(key)
            assertTrue(key.isNotBlank())
        }
    }

    // ── createScreen presence ────────────────────────────────────────────────

    @Nested
    inner class CreateScreenPresence {

        @Test
        fun `createScreen method exists on companion for no-arg fragment`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertTrue(hasMethod(result, "com.example.MyFragment", "createScreen")) {
                "Expected createScreen method on companion"
            }
        }

        @Test
        fun `createScreen method exists for direct Parcelable arg`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class)
                class ArgFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertTrue(hasMethod(result, "com.example.ArgFragment", "createScreen")) {
                "Expected createScreen method on companion with arg"
            }
        }

        @Test
        fun `createScreen method exists for indirect Parcelable arg`() {
            // The bug fix: Routes : Parcelable, MessageRoutes : Routes
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable
                sealed interface MessageRoutes : Routes

                @Screen(arg = MessageRoutes::class)
                class MessagesFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertTrue(hasMethod(result, "com.example.MessagesFragment", "createScreen")) {
                "Expected createScreen method for indirect Parcelable arg"
            }
        }
    }

    // ── Companion object structure ───────────────────────────────────────────

    @Nested
    inner class CompanionStructure {

        @Test
        fun `companion class is generated`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            // Companion should be loadable
            val companion = try {
                result.classLoader.loadClass("com.example.MyFragment\$Companion")
            } catch (_: ClassNotFoundException) {
                null
            }
            assertNotNull(companion) { "Expected Companion class to exist" }
        }

        @Test
        fun `existing companion members are preserved after augmentation`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment() {
                    companion object {
                        const val TAG = "MyFragment"
                        const val ARG_ID = "id"
                    }
                }
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            // KEY should exist
            val key = readKeyValue(result, "com.example.MyFragment")
            assertNotNull(key)

            // Original constants should still be accessible
            val companionClazz = result.classLoader.loadClass("com.example.MyFragment\$Companion")
            val tagField = try {
                companionClazz.getDeclaredField("TAG")
            } catch (_: NoSuchFieldException) {
                result.classLoader.loadClass("com.example.MyFragment").getDeclaredField("TAG")
            }
            tagField.isAccessible = true
            assertEquals("MyFragment", tagField.get(null))
        }

        @Test
        fun `nested inner fragment uses dollar-separated binary name in createScreen`() {
            val source = SourceFile.kotlin(
                "OuterFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                class Outer {
                    @Screen
                    class InnerFragment : Fragment()
                }
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            // The KEY should use dots (it's a bundle key, not a class name)
            val key = readKeyValue(result, "com.example.Outer\$InnerFragment")
            assertNotNull(key)
            // Verify createScreen exists on the companion
            assertTrue(hasMethod(result, "com.example.Outer\$InnerFragment", "createScreen"))
        }

        @Test
        fun `Fragment subclass in nested package has correct KEY`() {
            val source = SourceFile.kotlin(
                "DetailFragment.kt",
                """
                package com.example.ui.detail

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                open class BaseDetailFragment : Fragment()

                @Screen
                class DetailFragment : BaseDetailFragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertEquals(
                "com.example.ui.detail.DetailFragment",
                readKeyValue(result, "com.example.ui.detail.DetailFragment"),
            )
        }
    }
}
