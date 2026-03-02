package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenDeclarationGenerationTest {

    // ── KEY generation ───────────────────────────────────────────────────────

    @Nested
    inner class KeyGeneration {

        @Test
        fun `generates KEY on companion object`() {
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
        fun `generates companion when class has none`() {
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
        fun `augments existing companion object with KEY`() {
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
        fun `multiple Screen fragments each get their own KEY`() {
            val fragments = SourceFile.kotlin(
                "Fragments.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen class FragmentA : Fragment()
                @Screen class FragmentB : Fragment()
                @Screen class FragmentC : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                val keys: List<String> = listOf(
                    FragmentA.KEY,
                    FragmentB.KEY,
                    FragmentC.KEY,
                )
                """,
            )
            val result = compileWithScreenPlugin(fragments, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected all fragments to have KEY. Output:\n${result.messages}"
            }
        }

        @Test
        fun `Screen on custom base fragment generates KEY`() {
            val fragments = SourceFile.kotlin(
                "Fragments.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                open class BaseFragment : Fragment()

                @Screen
                class ChildFragment : BaseFragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                fun key(): String = ChildFragment.KEY
                """,
            )
            val result = compileWithScreenPlugin(fragments, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected KEY on Fragment subclass. Output:\n${result.messages}"
            }
        }
    }

    // ── createScreen (no arg) ────────────────────────────────────────────────

    @Nested
    inner class CreateScreenNoArg {

        @Test
        fun `generates createScreen function`() {
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
        fun `createScreen is accessible on DialogFragment`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.DialogFragment

                @Screen
                class MyDialog : DialogFragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(): FragmentScreen = MyDialog.createScreen()
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected createScreen() on DialogFragment. Output:\n${result.messages}"
            }
        }
    }

    // ── createScreen (with direct Parcelable arg) ────────────────────────────

    @Nested
    inner class CreateScreenWithDirectArg {

        @Test
        fun `generates createScreen with arg parameter`() {
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

        @Test
        fun `generates arg read property (compilation succeeds)`() {
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
            val result = compileWithScreenPlugin(fragment)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected compilation to succeed with arg. Output:\n${result.messages}"
            }
        }

        @Test
        fun `sealed class arg generates createScreen`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                sealed class NavRoutes : Parcelable

                @Screen(arg = NavRoutes::class)
                class NavFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(route: NavRoutes): FragmentScreen = NavFragment.createScreen(route)
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected createScreen(route) to work with sealed class arg. Output:\n${result.messages}"
            }
        }
    }

    // ── createScreen (with indirect Parcelable arg) ──────────────────────────

    @Nested
    inner class CreateScreenWithIndirectArg {

        @Test
        fun `interface inheriting Parcelable generates createScreen`() {
            // The bug fix: MessageRoutes : Routes where Routes : Parcelable
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable
                sealed interface MessageRoutes : Routes

                @Screen(arg = MessageRoutes::class)
                class MessagesFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(route: MessageRoutes): FragmentScreen = MessagesFragment.createScreen(route)
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected createScreen with indirect Parcelable to work. Output:\n${result.messages}"
            }
        }

        @Test
        fun `two-level interface chain generates createScreen`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Base : Parcelable
                interface Mid : Base
                sealed interface Leaf : Mid

                @Screen(arg = Leaf::class)
                class LeafFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(arg: Leaf): FragmentScreen = LeafFragment.createScreen(arg)
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected createScreen with two-level chain. Output:\n${result.messages}"
            }
        }

        @Test
        fun `abstract class extending Parcelable interface generates createScreen`() {
            val fragment = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface AppRoutes : Parcelable
                abstract class AbstractRoutes : AppRoutes
                class ConcreteRoutes : AbstractRoutes()

                @Screen(arg = ConcreteRoutes::class)
                class AppFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package test

                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(arg: ConcreteRoutes): FragmentScreen = AppFragment.createScreen(arg)
                """,
            )
            val result = compileWithScreenPlugin(fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected createScreen with abstract class chain. Output:\n${result.messages}"
            }
        }
    }
}
