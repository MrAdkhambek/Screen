package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScreenIrGenerationTest {

    private fun readKeyValue(result: JvmCompilationResult, fqn: String): String {
        val clazz = result.classLoader.loadClass(fqn)
        // const val KEY is generated on the companion. Try companion class first,
        // then fall back to the outer class (where Kotlin copies const vals).
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

    @Test
    fun `KEY value equals fully qualified class name`() {
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
    fun `KEY value for top-level fragment without package`() {
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
    fun `KEY value for deeply nested package`() {
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
}
