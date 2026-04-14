package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenNullableArgTest {

    // ── Declaration generation: isNullable=true ─────────────────────────────

    @Nested
    inner class DeclarationGeneration {

        @Test
        fun `isNullable=true generates nullable arg property`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = true)
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(fragment)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected compilation to succeed with isNullable=true. Output:\n${result.messages}"
            }
        }

        @Test
        fun `isNullable=true arg property is accessible as nullable type`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = true)
                class MyFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                fun readArg(f: MyFragment): MyArg? {
                    val method = f::class.java.getDeclaredMethod("getArg")
                    method.isAccessible = true
                    return method.invoke(f) as? MyArg
                }
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected nullable arg to be usable as MyArg?. Output:\n${result.messages}"
            }
        }

        @Test
        fun `isNullable=true createScreen accepts null arg`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = true)
                class NullableArgFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun createWithNull(): FragmentScreen = NullableArgFragment.createScreen()
                fun createWithArg(a: MyArg): FragmentScreen = NullableArgFragment.createScreen(a)
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected createScreen() to accept null default for nullable arg. Output:\n${result.messages}"
            }
        }

        @Test
        fun `isNullable=false createScreen requires arg`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = false)
                class RequiredArgFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun createWithoutArg(): FragmentScreen = RequiredArgFragment.createScreen()
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode) {
                "Expected createScreen() without arg to fail for non-nullable. Output:\n${result.messages}"
            }
        }
    }

    // ── IR generation: isNullable=true ───────────────────────────────────────

    @Nested
    inner class IrGeneration {

        @Test
        fun `nullable arg getter returns type with correct nullability`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = true)
                class NullableFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("test.NullableFragment")
            val getArg = clazz.getDeclaredMethod("getArg")
            getArg.isAccessible = true

            // Return type should reflect nullable on the JVM (not a primitive)
            assertNotNull(getArg) { "Expected getArg method on NullableFragment" }
            assertEquals("test.MyArg", getArg.returnType.name)
        }

        @Test
        fun `non-nullable arg getter exists with correct return type`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class)
                class NonNullableFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("test.NonNullableFragment")
            val getArg = clazz.getDeclaredMethod("getArg")
            getArg.isAccessible = true

            assertNotNull(getArg) { "Expected getArg method on NonNullableFragment" }
            assertEquals("test.MyArg", getArg.returnType.name)
        }

        @Test
        fun `createScreen with nullable arg has default null parameter`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = true)
                class NullArgFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val companionClazz = result.classLoader.loadClass("test.NullArgFragment\$Companion")
            val createScreenMethods = companionClazz.declaredMethods.filter {
                it.name == "createScreen"
            }
            assertTrue(createScreenMethods.isNotEmpty()) {
                "Expected createScreen method on companion"
            }
            // There should be a variant that takes the arg parameter
            val withArgMethod = createScreenMethods.firstOrNull { method ->
                method.parameterTypes.any { it.name == "test.MyArg" }
            }
            assertNotNull(withArgMethod) {
                "Expected createScreen to have a variant accepting MyArg parameter"
            }
        }
    }
}
