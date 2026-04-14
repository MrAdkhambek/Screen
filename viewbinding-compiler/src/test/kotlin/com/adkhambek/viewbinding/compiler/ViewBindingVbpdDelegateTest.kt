package com.adkhambek.viewbinding.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ViewBindingVbpdDelegateTest {

    // Stub for the VBPD library's ViewBindingProperty and viewBinding function.
    private val vbpdStub = SourceFile.kotlin(
        "ViewBindingPropertyDelegate.kt",
        """
        package dev.androidbroadcast.vbpd

        import kotlin.properties.ReadOnlyProperty
        import kotlin.reflect.KProperty

        class ViewBindingProperty<in F, out V>(
            private val viewBinder: (F) -> V,
        ) : ReadOnlyProperty<F, @kotlin.UnsafeVariance V> {
            private var binding: @kotlin.UnsafeVariance V? = null

            override fun getValue(thisRef: F, property: KProperty<*>): V {
                @Suppress("UNCHECKED_CAST")
                return binding ?: viewBinder(thisRef).also { binding = it as @kotlin.UnsafeVariance V }
            }
        }

        fun <F : androidx.fragment.app.Fragment, V> androidx.fragment.app.Fragment.viewBinding(
            viewBinder: (androidx.fragment.app.Fragment) -> V,
        ): ViewBindingProperty<androidx.fragment.app.Fragment, V> {
            return ViewBindingProperty(viewBinder)
        }
        """,
    )

    @OptIn(ExperimentalCompilerApi::class)
    private fun compileWithVbpd(
        vararg sources: SourceFile,
        namespace: String = "com.example",
    ) = KotlinCompilation().apply {
        this.sources = AllStubs + listOf(vbpdStub) + sources.toList()
        compilerPluginRegistrars = listOf(ViewBindingCompilerPluginRegistrar())
        commandLineProcessors = listOf(ViewBindingCommandLineProcessor())
        pluginOptions = listOf(
            PluginOption("com.adkhambek.viewbinding.compiler", "namespace", namespace),
        )
        languageVersion = "2.0"
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()

    // ── VBPD delegate path ──────────────────────────────────────────────────

    @Nested
    inner class VbpdDelegatePath {

        @Test
        fun `compiles successfully with VBPD on classpath`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment(R.layout.fragment_sample)
                """,
            )
            val result = compileWithVbpd(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Expected compilation to succeed with VBPD. Output:\n${result.messages}"
            }
        }

        @Test
        fun `delegate fields are created when VBPD is available`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment(R.layout.fragment_sample)
                """,
            )
            val result = compileWithVbpd(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("com.example.MyFragment")
            val fields = clazz.declaredFields.map { it.name }

            assertTrue(fields.contains("binding\$kprop")) {
                "Expected binding\$kprop static field for KProperty. Fields: $fields"
            }
            assertTrue(fields.contains("binding\$delegate")) {
                "Expected binding\$delegate instance field. Fields: $fields"
            }
        }

        @Test
        fun `binding kprop field is static`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment(R.layout.fragment_sample)
                """,
            )
            val result = compileWithVbpd(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("com.example.MyFragment")
            val kpropField = clazz.getDeclaredField("binding\$kprop")
            assertTrue(java.lang.reflect.Modifier.isStatic(kpropField.modifiers)) {
                "Expected binding\$kprop to be static"
            }
        }

        @Test
        fun `binding delegate field is not static`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment(R.layout.fragment_sample)
                """,
            )
            val result = compileWithVbpd(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("com.example.MyFragment")
            val delegateField = clazz.getDeclaredField("binding\$delegate")
            assertFalse(java.lang.reflect.Modifier.isStatic(delegateField.modifiers)) {
                "Expected binding\$delegate to be an instance field"
            }
        }

        @Test
        fun `binding getter still exists with correct return type`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment(R.layout.fragment_sample)
                """,
            )
            val result = compileWithVbpd(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("com.example.MyFragment")
            val getBinding = clazz.declaredMethods.firstOrNull { it.name == "getBinding" }
            assertNotNull(getBinding) {
                "Expected getBinding method. Methods: ${clazz.declaredMethods.map { it.name }}"
            }
            assertEquals("com.example.databinding.FragmentSampleBinding", getBinding!!.returnType.name)
        }
    }

    // ── Fallback without VBPD ───────────────────────────────────────────────

    @Nested
    inner class DirectBindFallback {

        @Test
        fun `no delegate fields when VBPD is not on classpath`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment(R.layout.fragment_sample)
                """,
            )
            // Use the standard compilation helper without VBPD
            val result = compileWithViewBindingPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("com.example.MyFragment")
            val fields = clazz.declaredFields.map { it.name }

            assertFalse(fields.contains("binding\$kprop")) {
                "Expected no binding\$kprop without VBPD. Fields: $fields"
            }
            assertFalse(fields.contains("binding\$delegate")) {
                "Expected no binding\$delegate without VBPD. Fields: $fields"
            }
        }

        @Test
        fun `binding getter still works without VBPD`() {
            val source = SourceFile.kotlin(
                "MyFragment.kt",
                """
                package com.example

                import com.adkhambek.screen.Screen
                import androidx.fragment.app.Fragment

                @Screen
                class MyFragment : Fragment(R.layout.fragment_sample)
                """,
            )
            val result = compileWithViewBindingPlugin(source)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val clazz = result.classLoader.loadClass("com.example.MyFragment")
            val getBinding = clazz.declaredMethods.firstOrNull { it.name == "getBinding" }
            assertNotNull(getBinding) {
                "Expected getBinding method even without VBPD"
            }
            assertEquals("com.example.databinding.FragmentSampleBinding", getBinding!!.returnType.name)
        }
    }
}
