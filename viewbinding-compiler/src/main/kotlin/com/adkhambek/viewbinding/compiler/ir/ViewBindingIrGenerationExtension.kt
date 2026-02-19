// Package declaration for the IR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.ir

// Import the IrGenerationExtension interface for participating in the IR phase.
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
// Import IrPluginContext for accessing IR symbols, types, and factory.
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
// Import IrModuleFragment representing the top-level IR node for the module.
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
// Import transformChildrenVoid for recursively applying a transformer to all IR elements.
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// IR generation extension for the ViewBinding compiler plugin.
// This extension is registered by ViewBindingCompilerPluginRegistrar and runs after the FIR phase.
// Its purpose is to generate the actual getter body for the `binding` property declaration
// that was created as a stub during the FIR phase.
// It delegates all transformation logic to ViewBindingIrElementTransformer.
class ViewBindingIrGenerationExtension : IrGenerationExtension {
    // Called by the Kotlin compiler during the IR generation phase.
    // Applies the ViewBindingIrElementTransformer to all IR elements in the module.
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(ViewBindingIrElementTransformer(pluginContext))
    }
}
