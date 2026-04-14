package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenRoundTripTest {

    /**
     * Invokes createScreen(arg) reflectively, extracts the fragment from the
     * FragmentScreen's creator, and reads back the arg property.
     *
     * createScreen() builds a FragmentScreen whose creator lambda:
     *   1. instantiates the fragment via FragmentFactory
     *   2. creates a Bundle and puts the arg via putParcelable(KEY, arg)
     *   3. calls setArguments(bundle) on the fragment
     *
     * The arg getter reads back via BundleCompat.getParcelable(getArguments(), KEY, argClass).
     *
     * Returns the value read back from the arg property.
     */
    private fun roundTrip(
        result: JvmCompilationResult,
        fragmentFqn: String,
        argValue: Any,
    ): Any? {
        val fragmentClass = result.classLoader.loadClass(fragmentFqn)
        val companionClass = result.classLoader.loadClass("$fragmentFqn\$Companion")
        val companionInstance = fragmentClass.getDeclaredField("Companion").get(null)

        // Call createScreen(arg) — the method takes (arg, key, clearContainer) with defaults
        // but we need to find the right overload; the compiler generates a single method
        // with defaults compiled as extra parameters + a mask int.
        // The simplest approach: find createScreen with the matching arg type parameter.
        val createScreenMethod = companionClass.declaredMethods.first {
            it.name == "createScreen" && it.parameterCount >= 1
        }

        // Call createScreen with the arg; the generated default params: key=null, clearContainer=true, mask, handler
        val screenObj = when (createScreenMethod.parameterCount) {
            // (arg, key, clearContainer)
            3 -> createScreenMethod.invoke(companionInstance, argValue, null, true)
            // (arg, key, clearContainer, mask, DefaultConstructorMarker)
            5 -> createScreenMethod.invoke(companionInstance, argValue, null, true, 0, null)
            // fallback: try calling with just arg + filling defaults
            else -> {
                // try the $default synthetic method
                val defaultMethod = companionClass.declaredMethods.firstOrNull {
                    it.name == "createScreen\$default"
                }
                if (defaultMethod != null) {
                    // (companion, arg, key, clearContainer, mask, handler)
                    defaultMethod.invoke(null, companionInstance, argValue, null, false, 0b110, null)
                } else {
                    createScreenMethod.invoke(companionInstance, argValue, null, true)
                }
            }
        }

        assertNotNull(screenObj, "createScreen() should return a FragmentScreen")

        // Extract the fragment by invoking the creator with a FragmentFactory
        val fragmentScreenClass = result.classLoader.loadClass(
            "com.github.terrakok.cicerone.androidx.FragmentScreen"
        )
        val creatorField = fragmentScreenClass.getDeclaredField("creator")
        creatorField.isAccessible = true
        val creator = creatorField.get(screenObj)

        val creatorClass = result.classLoader.loadClass(
            "com.github.terrakok.cicerone.androidx.Creator"
        )
        val createMethod = creatorClass.getMethod("create", Any::class.java)

        val factoryClass = result.classLoader.loadClass("androidx.fragment.app.FragmentFactory")
        val factory = factoryClass.getDeclaredConstructor().newInstance()

        val fragment = createMethod.invoke(creator, factory)
        assertNotNull(fragment, "Creator should produce a fragment instance")

        // Read the arg property back via reflection
        val argMethod = fragmentClass.getDeclaredMethod("getArg")
        argMethod.isAccessible = true
        return argMethod.invoke(fragment)
    }

    /**
     * Similar to roundTrip but for nullable args — calls createScreen(arg)
     * where arg may be null.
     */
    private fun roundTripNullable(
        result: JvmCompilationResult,
        fragmentFqn: String,
        argValue: Any?,
    ): Any? {
        val fragmentClass = result.classLoader.loadClass(fragmentFqn)
        val companionClass = result.classLoader.loadClass("$fragmentFqn\$Companion")
        val companionInstance = fragmentClass.getDeclaredField("Companion").get(null)

        val createScreenMethod = companionClass.declaredMethods.first {
            it.name == "createScreen" && it.parameterCount >= 1
        }

        val screenObj = when (createScreenMethod.parameterCount) {
            3 -> createScreenMethod.invoke(companionInstance, argValue, null, true)
            5 -> createScreenMethod.invoke(companionInstance, argValue, null, true, 0, null)
            else -> {
                val defaultMethod = companionClass.declaredMethods.firstOrNull {
                    it.name == "createScreen\$default"
                }
                if (defaultMethod != null) {
                    defaultMethod.invoke(null, companionInstance, argValue, null, false, 0b110, null)
                } else {
                    createScreenMethod.invoke(companionInstance, argValue, null, true)
                }
            }
        }

        assertNotNull(screenObj, "createScreen() should return a FragmentScreen")

        val fragmentScreenClass = result.classLoader.loadClass(
            "com.github.terrakok.cicerone.androidx.FragmentScreen"
        )
        val creatorField = fragmentScreenClass.getDeclaredField("creator")
        creatorField.isAccessible = true
        val creator = creatorField.get(screenObj)

        val creatorClass = result.classLoader.loadClass(
            "com.github.terrakok.cicerone.androidx.Creator"
        )
        val createMethod = creatorClass.getMethod("create", Any::class.java)

        val factoryClass = result.classLoader.loadClass("androidx.fragment.app.FragmentFactory")
        val factory = factoryClass.getDeclaredConstructor().newInstance()

        val fragment = createMethod.invoke(creator, factory)
        assertNotNull(fragment, "Creator should produce a fragment instance")

        val argMethod = fragmentClass.getDeclaredMethod("getArg")
        argMethod.isAccessible = true
        return argMethod.invoke(fragment)
    }

    // ── Round-trip: write arg via createScreen, read back via arg getter ─────

    @Nested
    inner class DirectArgRoundTrip {

        @Test
        fun `arg written via createScreen is read back by arg getter`() {
            val source = SourceFile.kotlin(
                "RoundTrip.kt",
                """
                package roundtrip

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg(val id: Long) : Parcelable

                @Screen(arg = MyArg::class)
                class MyFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val argClass = result.classLoader.loadClass("roundtrip.MyArg")
            val argInstance = argClass.getDeclaredConstructor(Long::class.java).newInstance(42L)

            val readBack = roundTrip(result, "roundtrip.MyFragment", argInstance)

            assertNotNull(readBack, "arg getter should return the arg instance")
            val readId = argClass.getDeclaredMethod("getId").invoke(readBack)
            assertEquals(42L, readId, "Round-tripped arg.id should match the original value")
        }

        @Test
        fun `round-trip preserves arg identity through Bundle`() {
            val source = SourceFile.kotlin(
                "Identity.kt",
                """
                package roundtrip.identity

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class SimpleArg(val name: String) : Parcelable

                @Screen(arg = SimpleArg::class)
                class IdentityFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val argClass = result.classLoader.loadClass("roundtrip.identity.SimpleArg")
            val argInstance = argClass.getDeclaredConstructor(String::class.java).newInstance("hello")

            val readBack = roundTrip(result, "roundtrip.identity.IdentityFragment", argInstance)

            assertNotNull(readBack)
            val readName = argClass.getDeclaredMethod("getName").invoke(readBack)
            assertEquals("hello", readName)
        }

        @Test
        fun `sealed class arg round-trips through createScreen and getter`() {
            val source = SourceFile.kotlin(
                "SealedRoundTrip.kt",
                """
                package roundtrip.sealed

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                sealed class NavRoute : Parcelable
                class HomeRoute : NavRoute()

                @Screen(arg = NavRoute::class)
                class NavFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val routeClass = result.classLoader.loadClass("roundtrip.sealed.HomeRoute")
            val routeInstance = routeClass.getDeclaredConstructor().newInstance()

            val readBack = roundTrip(result, "roundtrip.sealed.NavFragment", routeInstance)

            assertNotNull(readBack, "Should read back a NavRoute subclass")
            assertEquals(routeClass, readBack!!.javaClass, "Read-back type should be HomeRoute")
        }
    }

    @Nested
    inner class IndirectArgRoundTrip {

        @Test
        fun `indirect Parcelable arg round-trips correctly`() {
            val source = SourceFile.kotlin(
                "IndirectRoundTrip.kt",
                """
                package roundtrip.indirect

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface Routes : Parcelable
                class DetailRoute(val detailId: Int) : Routes

                @Screen(arg = Routes::class)
                class DetailFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val routeClass = result.classLoader.loadClass("roundtrip.indirect.DetailRoute")
            val routeInstance = routeClass.getDeclaredConstructor(Int::class.java).newInstance(99)

            val readBack = roundTrip(result, "roundtrip.indirect.DetailFragment", routeInstance)

            assertNotNull(readBack)
            val readId = routeClass.getDeclaredMethod("getDetailId").invoke(readBack)
            assertEquals(99, readId)
        }

        @Test
        fun `sealed interface arg from two-level chain round-trips correctly`() {
            val source = SourceFile.kotlin(
                "ChainRoundTrip.kt",
                """
                package roundtrip.chain

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                interface BaseRoute : Parcelable
                sealed interface ChatRoute : BaseRoute
                class NewChatRoute(val userId: String) : ChatRoute

                @Screen(arg = ChatRoute::class)
                class ChatFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val routeClass = result.classLoader.loadClass("roundtrip.chain.NewChatRoute")
            val routeInstance = routeClass.getDeclaredConstructor(String::class.java).newInstance("user-123")

            val readBack = roundTrip(result, "roundtrip.chain.ChatFragment", routeInstance)

            assertNotNull(readBack)
            val readUserId = routeClass.getDeclaredMethod("getUserId").invoke(readBack)
            assertEquals("user-123", readUserId)
        }
    }

    @Nested
    inner class NullableArgRoundTrip {

        @Test
        fun `nullable arg with non-null value round-trips correctly`() {
            val source = SourceFile.kotlin(
                "NullableNonNull.kt",
                """
                package roundtrip.nullable

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class NullableArg(val value: Int) : Parcelable

                @Screen(arg = NullableArg::class, isNullable = true)
                class NullableFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val argClass = result.classLoader.loadClass("roundtrip.nullable.NullableArg")
            val argInstance = argClass.getDeclaredConstructor(Int::class.java).newInstance(7)

            val readBack = roundTripNullable(result, "roundtrip.nullable.NullableFragment", argInstance)

            assertNotNull(readBack)
            val readValue = argClass.getDeclaredMethod("getValue").invoke(readBack)
            assertEquals(7, readValue)
        }

        @Test
        fun `nullable arg with null value round-trips as null`() {
            val source = SourceFile.kotlin(
                "NullableNull.kt",
                """
                package roundtrip.nullable2

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class OptionalArg(val flag: Boolean) : Parcelable

                @Screen(arg = OptionalArg::class, isNullable = true)
                class OptionalFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val readBack = roundTripNullable(result, "roundtrip.nullable2.OptionalFragment", null)
            assertNull(readBack, "Nullable arg with null should read back as null")
        }
    }

    @Nested
    inner class DialogFragmentRoundTrip {

        @Test
        fun `arg round-trips through DialogFragment`() {
            val source = SourceFile.kotlin(
                "DialogRoundTrip.kt",
                """
                package roundtrip.dialog

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.DialogFragment

                class DialogArg(val message: String) : Parcelable

                @Screen(arg = DialogArg::class)
                class MyDialog : DialogFragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val argClass = result.classLoader.loadClass("roundtrip.dialog.DialogArg")
            val argInstance = argClass.getDeclaredConstructor(String::class.java).newInstance("test-msg")

            val readBack = roundTrip(result, "roundtrip.dialog.MyDialog", argInstance)

            assertNotNull(readBack)
            val readMessage = argClass.getDeclaredMethod("getMessage").invoke(readBack)
            assertEquals("test-msg", readMessage)
        }
    }
}
