package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException

class ScreenRuntimeArgTest {

    // ── Runtime arg getter invocation ───────────────────────────────────────

    @Nested
    inner class ArgGetterInvocation {

        @Test
        fun `arg getter returns value from bundle for non-nullable arg`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment
                import android.os.Bundle

                class MyArg : Parcelable

                @Screen(arg = MyArg::class)
                class ArgFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            // Create a fragment instance and set its arguments bundle manually
            val fragmentClazz = result.classLoader.loadClass("test.ArgFragment")
            val fragment = fragmentClazz.getDeclaredConstructor().newInstance()

            val argClazz = result.classLoader.loadClass("test.MyArg")
            val argInstance = argClazz.getDeclaredConstructor().newInstance()

            // Create a bundle and put the arg in it using the fragment's FQN as key
            val bundleClazz = result.classLoader.loadClass("android.os.Bundle")
            val bundle = bundleClazz.getDeclaredConstructor(Int::class.java).newInstance(1)
            val putParcelable = bundleClazz.getDeclaredMethod(
                "putParcelable",
                String::class.java,
                result.classLoader.loadClass("android.os.Parcelable"),
            )
            putParcelable.invoke(bundle, "test.ArgFragment", argInstance)

            // Set arguments on the fragment
            val setArguments = fragmentClazz.getMethod("setArguments", bundleClazz)
            setArguments.invoke(fragment, bundle)

            // Invoke the generated arg getter
            val getArg = fragmentClazz.getDeclaredMethod("getArg")
            getArg.isAccessible = true
            val retrieved = getArg.invoke(fragment)

            assertNotNull(retrieved) { "Expected arg getter to return the value from the bundle" }
            assertEquals(argClazz, retrieved!!::class.java) {
                "Expected arg getter to return instance of MyArg"
            }
        }

        @Test
        fun `arg getter throws on missing arguments for non-nullable arg`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class)
                class NoArgSetFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val fragmentClazz = result.classLoader.loadClass("test.NoArgSetFragment")
            val fragment = fragmentClazz.getDeclaredConstructor().newInstance()

            val getArg = fragmentClazz.getDeclaredMethod("getArg")
            getArg.isAccessible = true

            // Without setting arguments, the getter should throw (requireNotNull on getArguments())
            assertThrows(InvocationTargetException::class.java) {
                getArg.invoke(fragment)
            }
        }

        @Test
        fun `nullable arg getter returns null when arg not in bundle`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment
                import android.os.Bundle

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = true)
                class NullableArgFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val fragmentClazz = result.classLoader.loadClass("test.NullableArgFragment")
            val fragment = fragmentClazz.getDeclaredConstructor().newInstance()

            // Set an empty bundle (no arg put in it)
            val bundleClazz = result.classLoader.loadClass("android.os.Bundle")
            val bundle = bundleClazz.getDeclaredConstructor().newInstance()
            val setArguments = fragmentClazz.getMethod("setArguments", bundleClazz)
            setArguments.invoke(fragment, bundle)

            val getArg = fragmentClazz.getDeclaredMethod("getArg")
            getArg.isAccessible = true
            val retrieved = getArg.invoke(fragment)

            assertEquals(null, retrieved) {
                "Expected nullable arg getter to return null when key is missing from bundle"
            }
        }

        @Test
        fun `nullable arg getter returns value when arg is in bundle`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package test

                import com.adkhambek.screen.Screen
                import android.os.Parcelable
                import androidx.fragment.app.Fragment

                class MyArg : Parcelable

                @Screen(arg = MyArg::class, isNullable = true)
                class NullableWithArgFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val fragmentClazz = result.classLoader.loadClass("test.NullableWithArgFragment")
            val fragment = fragmentClazz.getDeclaredConstructor().newInstance()

            val argClazz = result.classLoader.loadClass("test.MyArg")
            val argInstance = argClazz.getDeclaredConstructor().newInstance()

            val bundleClazz = result.classLoader.loadClass("android.os.Bundle")
            val bundle = bundleClazz.getDeclaredConstructor(Int::class.java).newInstance(1)
            val putParcelable = bundleClazz.getDeclaredMethod(
                "putParcelable",
                String::class.java,
                result.classLoader.loadClass("android.os.Parcelable"),
            )
            putParcelable.invoke(bundle, "test.NullableWithArgFragment", argInstance)

            val setArguments = fragmentClazz.getMethod("setArguments", bundleClazz)
            setArguments.invoke(fragment, bundle)

            val getArg = fragmentClazz.getDeclaredMethod("getArg")
            getArg.isAccessible = true
            val retrieved = getArg.invoke(fragment)

            assertNotNull(retrieved) {
                "Expected nullable arg getter to return value when arg is in bundle"
            }
            assertEquals(argClazz, retrieved!!::class.java)
        }
    }
}
