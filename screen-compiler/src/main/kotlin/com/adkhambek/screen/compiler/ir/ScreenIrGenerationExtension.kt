// Package declaration for the IR (Intermediate Representation) phase of the Screen compiler plugin.
package com.adkhambek.screen.compiler.ir

// Import the IrGenerationExtension interface that plugins implement to participate in the IR phase.
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
// Import IrPluginContext which provides access to IR symbols, types, and the IR factory.
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
// Import IrModuleFragment which represents the top-level IR node containing all module declarations.
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
// Import transformChildrenVoid to recursively apply an IrElementTransformerVoid to all IR elements.
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// IR generation extension for the Screen compiler plugin.
// This extension is registered by ScreenCompilerPluginRegistrar and runs after the FIR phase.
// Its purpose is to generate the actual code (method bodies, constant initializers) for the
// declarations that were created as stubs during the FIR phase.
// It delegates all transformation logic to ScreenIrElementTransformer.
class ScreenIrGenerationExtension : IrGenerationExtension {
    // Called by the Kotlin compiler during the IR generation phase.
    // moduleFragment contains the entire IR tree for the current compilation module.
    // pluginContext provides access to symbol resolution, type creation, and the IR factory.
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // Apply the ScreenIrElementTransformer to all children of the module fragment.
        // transformChildrenVoid recursively visits all IR elements (classes, functions, properties)
        // and calls the transformer's visit methods for each one.
        moduleFragment.transformChildrenVoid(ScreenIrElementTransformer(pluginContext))
    }
}
