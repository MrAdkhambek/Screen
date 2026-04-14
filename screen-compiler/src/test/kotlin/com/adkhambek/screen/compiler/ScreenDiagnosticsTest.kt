package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenDiagnosticsTest {

    // ── SCREEN_NOT_ON_FRAGMENT ───────────────────────────────────────────────

    @Nested
    inner class NotOnFragment {

        @Test
        fun `plain class reports error`() {
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
        fun `data class reports error`() {
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
        }

        @Test
        fun `object reports error`() {
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
        }

        @Test
        fun `interface reports error`() {
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
        }
    }

    // ── Fragment hierarchy ───────────────────────────────────────────────────

    @Nested
    inner class FragmentHierarchy {

        @Test
        fun `direct Fragment subclass compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `direct DialogFragment subclass compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.DialogFragment

                @Screen
                class MyDialogFragment : DialogFragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `one-level custom base fragment compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                open class BaseFragment : Fragment()

                @Screen
                class ChildFragment : BaseFragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `two-level custom base fragment compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                open class BaseFragment : Fragment()
                open class MiddleFragment : BaseFragment()

                @Screen
                class LeafFragment : MiddleFragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `three-level custom base fragment compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                open class Level1 : Fragment()
                open class Level2 : Level1()
                open class Level3 : Level2()

                @Screen
                class Level4 : Level3()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }
    }

    // ── SCREEN_ARG_NOT_PARCELABLE – error cases ──────────────────────────────

    @Nested
    inner class ArgNotParcelable {

        @Test
        fun `plain class arg reports error`() {
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
        fun `class implementing unrelated interface reports error`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                interface Serializable
                class MyArg : Serializable

                @Screen(arg = MyArg::class)
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        }

        @Test
        fun `interface not extending Parcelable reports error`() {
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
        }

        @Test
        fun `data class without Parcelable reports error`() {
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
        }
    }

    // ── SCREEN_ARG_NOT_PARCELABLE – success cases (direct Parcelable) ────────

    @Nested
    inner class ArgDirectParcelable {

        @Test
        fun `class directly implementing Parcelable compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `sealed class directly implementing Parcelable compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                sealed class MyRoutes : Parcelable

                @Screen(arg = MyRoutes::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `interface directly extending Parcelable compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable

                @Screen(arg = Routes::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `sealed interface directly extending Parcelable compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                sealed interface Routes : Parcelable

                @Screen(arg = Routes::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `class implementing Parcelable and another interface compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Taggable
                class MyArg : Taggable, Parcelable

                @Screen(arg = MyArg::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }
    }

    // ── SCREEN_ARG_NOT_PARCELABLE – success cases (indirect Parcelable) ──────
    // These cover the bug fix: Parcelable reachable only via inheritance chain.

    @Nested
    inner class ArgIndirectParcelable {

        @Test
        fun `interface inheriting from Parcelable interface compiles successfully`() {
            // Routes : Parcelable
            // MessageRoutes : Routes  <-- the exact reported bug case
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable
                sealed interface MessageRoutes : Routes

                @Screen(arg = MessageRoutes::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `sealed interface inheriting explicitly Parcelable and without explicit Parcelable compiles`() {
            // Both variants from the bug report
            val withExplicit = SourceFile.kotlin(
                "WithExplicit.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable
                sealed interface MessageRoutes : Routes, Parcelable

                @Screen(arg = MessageRoutes::class)
                class FragmentA : Fragment()
                """,
            )
            val withoutExplicit = SourceFile.kotlin(
                "WithoutExplicit.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes2 : Parcelable
                sealed interface OtherRoutes : Routes2

                @Screen(arg = OtherRoutes::class)
                class FragmentB : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(withExplicit).exitCode)
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(withoutExplicit).exitCode)
        }

        @Test
        fun `two-level interface chain compiles successfully`() {
            // Base : Parcelable, Mid : Base, Leaf : Mid
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Base : Parcelable
                interface Mid : Base
                sealed interface Leaf : Mid

                @Screen(arg = Leaf::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `three-level interface chain compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface A : Parcelable
                interface B : A
                interface C : B
                sealed interface D : C

                @Screen(arg = D::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `class extending abstract class that implements Parcelable compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                abstract class BaseArgs : Parcelable
                class ConcreteArgs : BaseArgs()

                @Screen(arg = ConcreteArgs::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `class with Parcelable via both interface and class hierarchy compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable
                abstract class AbstractRoutes : Routes
                class ConcreteRoutes : AbstractRoutes()

                @Screen(arg = ConcreteRoutes::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `sealed class subtype of Parcelable interface compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable
                sealed class SettingsRoutes : Routes

                @Screen(arg = SettingsRoutes::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `class implementing interface that mixes Parcelable in diamond hierarchy compiles`() {
            // X : Parcelable, Y (no Parcelable), Z : X, Y
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface WithId : Parcelable
                interface WithName
                class MyArg : WithId, WithName

                @Screen(arg = MyArg::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }
    }

    // ── Cross-package arg resolution ──────────────────────────────────────────

    @Nested
    inner class CrossPackageArg {

        @Test
        fun `arg class in different package with explicit import compiles successfully`() {
            val argFile = SourceFile.kotlin(
                "MyArg.kt",
                """
                package com.other.pkg

                import android.os.Parcelable

                class MyArg : Parcelable
                """,
            )
            val fragment = SourceFile.kotlin(
                "TestClass.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment
                import com.other.pkg.MyArg

                @Screen(arg = MyArg::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(argFile, fragment).exitCode)
        }

        @Test
        fun `arg class in different package with star import compiles successfully`() {
            val argFile = SourceFile.kotlin(
                "MyArg.kt",
                """
                package com.other.pkg

                import android.os.Parcelable

                class MyArg : Parcelable
                """,
            )
            val fragment = SourceFile.kotlin(
                "TestClass.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment
                import com.other.pkg.*

                @Screen(arg = MyArg::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(argFile, fragment).exitCode)
        }

        @Test
        fun `non-Parcelable arg in different package reports error`() {
            val argFile = SourceFile.kotlin(
                "MyArg.kt",
                """
                package com.other.pkg

                class NotParcelable
                """,
            )
            val fragment = SourceFile.kotlin(
                "TestClass.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment
                import com.other.pkg.NotParcelable

                @Screen(arg = NotParcelable::class)
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(argFile, fragment)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("Parcelable")) {
                "Expected SCREEN_ARG_NOT_PARCELABLE diagnostic, got:\n${result.messages}"
            }
        }
    }

    // ── No arg (default Unit) ────────────────────────────────────────────────

    @Nested
    inner class NoArg {

        @Test
        fun `omitting arg compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }

        @Test
        fun `explicit Unit arg compiles successfully`() {
            val source = SourceFile.kotlin(
                "TestClass.kt",
                """
                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen(arg = Unit::class)
                class MyFragment : Fragment()
                """,
            )
            assertEquals(KotlinCompilation.ExitCode.OK, compileWithScreenPlugin(source).exitCode)
        }
    }
}
