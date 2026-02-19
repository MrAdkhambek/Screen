package com.adkhambek.viewbinding.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ViewBindingIrGenerationTest {

    @Test
    fun `binding getter compiles and class loads`() {
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
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed. Output:\n${result.messages}"
        }
        val clazz = result.classLoader.loadClass("com.example.MyFragment")
        assertNotNull(clazz) { "Expected MyFragment class to load" }
    }

    @Test
    fun `binding property exists with correct return type`() {
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
            "Expected getBinding method on MyFragment. Methods: ${clazz.declaredMethods.map { it.name }}"
        }
        assertEquals("com.example.databinding.FragmentSampleBinding", getBinding!!.returnType.name) {
            "Expected binding return type to be FragmentSampleBinding"
        }
    }
}
