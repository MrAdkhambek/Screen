package com.adkhambek.screen.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenCrossPackageTest {

    // ── Arg type defined in a different package than the fragment ────────────

    @Nested
    inner class DifferentPackageArg {

        @Test
        fun `arg class in separate package compiles and generates createScreen`() {
            val argSource = SourceFile.kotlin(
                "Arg.kt",
                """
                package com.example.model

                import android.os.Parcelable

                class UserArg(val userId: Long) : Parcelable
                """,
            )
            val fragmentSource = SourceFile.kotlin(
                "UserFragment.kt",
                """
                package com.example.ui.user

                import com.adkhambek.screen.Screen
                import com.example.model.UserArg
                import androidx.fragment.app.Fragment

                @Screen(arg = UserArg::class)
                class UserFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package com.example.ui.user

                import com.example.model.UserArg
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(arg: UserArg): FragmentScreen = UserFragment.createScreen(arg)
                """,
            )
            val result = compileWithScreenPlugin(argSource, fragmentSource, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Cross-package arg should compile. Output:\n${result.messages}"
            }
        }

        @Test
        fun `arg class in separate package generates correct arg getter`() {
            val argSource = SourceFile.kotlin(
                "Arg.kt",
                """
                package com.example.model

                import android.os.Parcelable

                class ProfileArg(val name: String) : Parcelable
                """,
            )
            val fragmentSource = SourceFile.kotlin(
                "ProfileFragment.kt",
                """
                package com.example.ui.profile

                import com.adkhambek.screen.Screen
                import com.example.model.ProfileArg
                import androidx.fragment.app.Fragment

                @Screen(arg = ProfileArg::class)
                class ProfileFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(argSource, fragmentSource)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            // Verify the arg getter exists and returns the cross-package type
            val fragmentClass = result.classLoader.loadClass("com.example.ui.profile.ProfileFragment")
            val argMethod = fragmentClass.getDeclaredMethod("getArg")
            assertNotNull(argMethod, "arg getter should exist for cross-package arg")
            assertEquals(
                "com.example.model.ProfileArg",
                argMethod.returnType.name,
                "arg getter should return the cross-package type",
            )
        }

        @Test
        fun `arg interface in separate package with sealed subclass compiles`() {
            val routesSource = SourceFile.kotlin(
                "Routes.kt",
                """
                package com.example.navigation

                import android.os.Parcelable

                sealed interface AppRoute : Parcelable
                class HomeRoute : AppRoute
                class SettingsRoute(val section: String) : AppRoute
                """,
            )
            val fragmentSource = SourceFile.kotlin(
                "AppFragment.kt",
                """
                package com.example.ui

                import com.adkhambek.screen.Screen
                import com.example.navigation.AppRoute
                import androidx.fragment.app.Fragment

                @Screen(arg = AppRoute::class)
                class AppFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package com.example.ui

                import com.example.navigation.HomeRoute
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(): FragmentScreen = AppFragment.createScreen(HomeRoute())
                """,
            )
            val result = compileWithScreenPlugin(routesSource, fragmentSource, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Cross-package sealed interface arg should compile. Output:\n${result.messages}"
            }
        }
    }

    // ── Arg type via cross-package inheritance chain ─────────────────────────

    @Nested
    inner class CrossPackageInheritanceChain {

        @Test
        fun `Parcelable interface in pkg A, sealed interface in pkg B, fragment in pkg C`() {
            val baseSource = SourceFile.kotlin(
                "Base.kt",
                """
                package pkg.base

                import android.os.Parcelable

                interface BaseRoute : Parcelable
                """,
            )
            val midSource = SourceFile.kotlin(
                "Mid.kt",
                """
                package pkg.mid

                import pkg.base.BaseRoute

                sealed interface MessageRoute : BaseRoute
                class InboxRoute : MessageRoute
                """,
            )
            val fragmentSource = SourceFile.kotlin(
                "MessageFragment.kt",
                """
                package pkg.ui

                import com.adkhambek.screen.Screen
                import pkg.mid.MessageRoute
                import androidx.fragment.app.Fragment

                @Screen(arg = MessageRoute::class)
                class MessageFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package pkg.ui

                import pkg.mid.InboxRoute
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun create(): FragmentScreen = MessageFragment.createScreen(InboxRoute())
                """,
            )
            val result = compileWithScreenPlugin(baseSource, midSource, fragmentSource, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Three-package inheritance chain should compile. Output:\n${result.messages}"
            }
        }

        @Test
        fun `abstract class in different package extending Parcelable interface`() {
            val parcelableIface = SourceFile.kotlin(
                "Contracts.kt",
                """
                package contracts

                import android.os.Parcelable

                interface Navigable : Parcelable
                """,
            )
            val abstractArg = SourceFile.kotlin(
                "AbstractArg.kt",
                """
                package args

                import contracts.Navigable

                abstract class BaseArg : Navigable
                """,
            )
            val concreteArg = SourceFile.kotlin(
                "ConcreteArg.kt",
                """
                package args.impl

                import args.BaseArg

                class ConcreteArg(val data: Int) : BaseArg()
                """,
            )
            val fragment = SourceFile.kotlin(
                "NavFragment.kt",
                """
                package ui

                import com.adkhambek.screen.Screen
                import args.impl.ConcreteArg
                import androidx.fragment.app.Fragment

                @Screen(arg = ConcreteArg::class)
                class NavFragment : Fragment()
                """,
            )
            val usage = SourceFile.kotlin(
                "Usage.kt",
                """
                package ui

                import args.impl.ConcreteArg
                import com.github.terrakok.cicerone.androidx.FragmentScreen

                fun nav(): FragmentScreen = NavFragment.createScreen(ConcreteArg(1))
                """,
            )
            val result = compileWithScreenPlugin(parcelableIface, abstractArg, concreteArg, fragment, usage)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Cross-package abstract class chain should compile. Output:\n${result.messages}"
            }
        }
    }

    // ── Cross-package round-trip (write + read back) ────────────────────────

    @Nested
    inner class CrossPackageRoundTrip {

        @Test
        fun `arg from different package round-trips through createScreen and getter`() {
            val argSource = SourceFile.kotlin(
                "Arg.kt",
                """
                package cross.model

                import android.os.Parcelable

                class ItemArg(val itemId: Int) : Parcelable
                """,
            )
            val fragmentSource = SourceFile.kotlin(
                "ItemFragment.kt",
                """
                package cross.ui

                import com.adkhambek.screen.Screen
                import cross.model.ItemArg
                import androidx.fragment.app.Fragment

                @Screen(arg = ItemArg::class)
                class ItemFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(argSource, fragmentSource)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            // Build an arg instance
            val argClass = result.classLoader.loadClass("cross.model.ItemArg")
            val argInstance = argClass.getDeclaredConstructor(Int::class.java).newInstance(777)

            // Call createScreen(arg) and extract the fragment
            val fragmentFqn = "cross.ui.ItemFragment"
            val fragmentClass = result.classLoader.loadClass(fragmentFqn)
            val companionClass = result.classLoader.loadClass("$fragmentFqn\$Companion")
            val companionInstance = fragmentClass.getDeclaredField("Companion").get(null)

            val createScreenMethod = companionClass.declaredMethods.first {
                it.name == "createScreen" && it.parameterCount >= 1
            }

            val screenObj = when (createScreenMethod.parameterCount) {
                3 -> createScreenMethod.invoke(companionInstance, argInstance, null, true)
                5 -> createScreenMethod.invoke(companionInstance, argInstance, null, true, 0, null)
                else -> createScreenMethod.invoke(companionInstance, argInstance, null, true)
            }
            assertNotNull(screenObj)

            // Extract the fragment from the creator
            val screenClass = result.classLoader.loadClass(
                "com.github.terrakok.cicerone.androidx.FragmentScreen"
            )
            val creatorField = screenClass.getDeclaredField("creator")
            creatorField.isAccessible = true
            val creator = creatorField.get(screenObj)

            val creatorClass = result.classLoader.loadClass(
                "com.github.terrakok.cicerone.androidx.Creator"
            )
            val createMethod = creatorClass.getMethod("create", Any::class.java)

            val factoryClass = result.classLoader.loadClass("androidx.fragment.app.FragmentFactory")
            val factory = factoryClass.getDeclaredConstructor().newInstance()
            val fragment = createMethod.invoke(creator, factory)

            // Read back the arg
            val argMethod = fragmentClass.getDeclaredMethod("getArg")
            argMethod.isAccessible = true
            val readBack = argMethod.invoke(fragment)

            assertNotNull(readBack, "Cross-package arg should be read back")
            val readId = argClass.getDeclaredMethod("getItemId").invoke(readBack)
            assertEquals(777, readId, "Cross-package round-trip should preserve arg value")
        }
    }

    // ── Cross-package diagnostic validation ─────────────────────────────────

    @Nested
    inner class CrossPackageDiagnostics {

        @Test
        fun `non-Parcelable arg from different package reports error`() {
            val argSource = SourceFile.kotlin(
                "Arg.kt",
                """
                package other.pkg

                class NotParcelableArg(val x: Int)
                """,
            )
            val fragmentSource = SourceFile.kotlin(
                "TestFragment.kt",
                """
                package ui.pkg

                import com.adkhambek.screen.Screen
                import other.pkg.NotParcelableArg
                import androidx.fragment.app.Fragment

                @Screen(arg = NotParcelableArg::class)
                class BadFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(argSource, fragmentSource)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("Parcelable")) {
                "Should report Parcelable error for cross-package non-Parcelable arg"
            }
        }

        @Test
        fun `Parcelable arg from different package passes diagnostics`() {
            val argSource = SourceFile.kotlin(
                "Arg.kt",
                """
                package data.models

                import android.os.Parcelable

                class ValidArg : Parcelable
                """,
            )
            val fragmentSource = SourceFile.kotlin(
                "TestFragment.kt",
                """
                package presentation.screens

                import com.adkhambek.screen.Screen
                import data.models.ValidArg
                import androidx.fragment.app.Fragment

                @Screen(arg = ValidArg::class)
                class GoodFragment : Fragment()
                """,
            )
            val result = compileWithScreenPlugin(argSource, fragmentSource)
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
                "Cross-package Parcelable arg should pass diagnostics. Output:\n${result.messages}"
            }
        }
    }
}
