package com.adkhambek.viewbinding.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ViewBindingDeclarationGenerationTest {

    @Test
    fun `@Screen with layout generates binding property`() {
        val source = SourceFile.kotlin(
            "MyFragment.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment
            import com.example.databinding.FragmentSampleBinding

            @Screen
            class MyFragment : Fragment(com.example.R.layout.fragment_sample) {
                fun useBinding(): FragmentSampleBinding = this.binding
            }
            """,
        )
        val result = compileWithViewBindingPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected binding property to be generated. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen without layout does NOT generate binding`() {
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
            fun useBinding(f: MyFragment) {
                f.binding
            }
            """,
        )
        val result = compileWithViewBindingPlugin(fragment, usage)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode) {
            "Expected unresolved reference for binding without layout. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen with layout but no matching binding class does NOT generate binding`() {
        // Use a layout name that has no matching binding stub
        val source = SourceFile.kotlin(
            "MyFragment.kt",
            """
            package test

            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            object R {
                object layout {
                    const val fragment_missing: Int = 0x7f0a0099
                }
            }

            @Screen
            class MyFragment : Fragment(R.layout.fragment_missing) {
                fun useBinding() {
                    this.binding
                }
            }
            """,
        )
        val result = compileWithViewBindingPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode) {
            "Expected unresolved reference when binding class is missing. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Screen with layout but no namespace does NOT generate binding`() {
        val source = SourceFile.kotlin(
            "MyFragment.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment

            @Screen
            class MyFragment : Fragment(com.example.R.layout.fragment_sample) {
                fun useBinding() {
                    this.binding
                }
            }
            """,
        )
        // Pass empty namespace so layoutNameToBindingClassId returns null
        val result = compileWithViewBindingPlugin(source, namespace = "")
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode) {
            "Expected unresolved reference when namespace is empty. Output:\n${result.messages}"
        }
    }

    @Test
    fun `layout name snake_case converts to PascalCaseBinding`() {
        val source = SourceFile.kotlin(
            "MyActivity.kt",
            """
            import com.adkhambek.screen.Screen
            import androidx.fragment.app.Fragment
            import com.example.databinding.ActivityMainBinding

            @Screen
            class MyActivity : Fragment(com.example.R.layout.activity_main) {
                fun useBinding(): ActivityMainBinding = this.binding
            }
            """,
        )
        val result = compileWithViewBindingPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected activity_main -> ActivityMainBinding conversion. Output:\n${result.messages}"
        }
    }
}
