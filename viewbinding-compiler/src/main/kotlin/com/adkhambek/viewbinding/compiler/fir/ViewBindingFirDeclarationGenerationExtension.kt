// Package declaration for the FIR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.fir

// Import FirSession which represents the current compilation session.
import org.jetbrains.kotlin.fir.FirSession
// Import DirectDeclarationsAccess opt-in for accessing direct declarations on a symbol.
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
// Import isCompanion to check if a class is a companion object.
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
// Import FirExpression which is the base type for FIR expressions.
import org.jetbrains.kotlin.fir.expressions.FirExpression
// Import FirPropertyAccessExpression for property access expressions (e.g., R.layout.name).
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
// Import FirResolvedQualifier for fully resolved qualified names.
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
// Import FirResolvePhase for specifying minimum resolution phases.
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
// Import lazyResolveToPhase for triggering lazy resolution.
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
// Import Visibilities for setting property visibility.
import org.jetbrains.kotlin.descriptors.Visibilities
// Import DeclarationGenerationContext for contextual info during generation.
import org.jetbrains.kotlin.fir.extensions.DeclarationGenerationContext
// Import FirDeclarationGenerationExtension, the base class for declaration generation plugins.
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
// Import FirDeclarationPredicateRegistrar for registering predicates.
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
// Import predicateBasedProvider for predicate-based symbol lookups.
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
// Import createConeType for constructing ConeKotlinType from ClassId.
import org.jetbrains.kotlin.fir.plugin.createConeType
// Import createMemberProperty for creating property declarations.
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
// Import symbolProvider for looking up symbols by ClassId.
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
// Import SymbolInternals opt-in for accessing internal symbol properties.
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
// Import FirClassSymbol for class symbol representation.
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
// Import FirConstructorSymbol for constructor symbol representation.
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
// Import FirPropertySymbol for property symbol representation.
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
// Import CallableId for callable identification.
import org.jetbrains.kotlin.name.CallableId
// Import ClassId for class identification.
import org.jetbrains.kotlin.name.ClassId
// Import FqName for fully qualified names.
import org.jetbrains.kotlin.name.FqName
// Import Name for simple identifiers.
import org.jetbrains.kotlin.name.Name

// Core FIR declaration generation extension for the ViewBinding compiler plugin.
// This extension generates a `private val binding: XxxBinding` property on @Screen-annotated
// Fragment classes that specify a layout resource in their primary constructor.
//
// The generation process:
//   1. Extract the layout resource name from the Fragment's super constructor call
//      (e.g., Fragment(R.layout.fragment_sample) → "fragment_sample")
//   2. Convert the snake_case layout name to a PascalCase binding class name
//      (e.g., "fragment_sample" → "FragmentSampleBinding")
//   3. Resolve the binding class under {namespace}.databinding
//   4. Generate the property declaration if the binding class exists
class ViewBindingFirDeclarationGenerationExtension(
    session: FirSession,
    // The Android namespace (e.g., "com.example.app") for resolving binding classes.
    // Null if the namespace was not provided (binding generation will be skipped).
    private val namespace: String?,
) : FirDeclarationGenerationExtension(session) {

    // Extracts the layout resource name from the Fragment's primary constructor delegation.
    // Looks at the delegated constructor call (e.g., super(R.layout.fragment_sample))
    // and extracts the layout name "fragment_sample".
    // Returns null if no layout resource is found in the constructor.
    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun extractContentLayoutId(classSymbol: FirClassSymbol<*>): String? {
        // Ensure supertypes are resolved so we can access the delegated constructor.
        classSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        // Find the primary constructor of the class.
        val primaryConstructorSymbol = classSymbol.declarationSymbols
            .filterIsInstance<FirConstructorSymbol>()
            .firstOrNull { it.isPrimary } ?: return null

        // Access the FIR declaration to get the delegated constructor call.
        val primaryConstructor = primaryConstructorSymbol.fir
        // The delegated constructor call is the super(...) call in the primary constructor.
        val delegatedCall = primaryConstructor.delegatedConstructor ?: return null
        // Get the first argument of the super constructor call (the layout resource).
        val firstArg = delegatedCall.argumentList.arguments.firstOrNull() ?: return null

        // Extract the layout name from the expression (e.g., R.layout.fragment_sample).
        return extractLayoutNameFromExpression(firstArg)
    }

    // Extracts the layout resource name from an expression like R.layout.fragment_sample.
    // Handles two forms of the expression depending on the FIR resolution stage:
    //   - After resolution: FirPropertyAccessExpression with FirResolvedQualifier receiver
    //   - Before resolution: FirPropertyAccessExpression with FirPropertyAccessExpression receiver
    // Returns the layout name (e.g., "fragment_sample") or null if the expression
    // doesn't match the R.layout.xxx pattern.
    private fun extractLayoutNameFromExpression(expr: FirExpression): String? {
        // The expression must be a property access (the layout name identifier).
        if (expr !is FirPropertyAccessExpression) return null
        // Get the layout name from the callee reference (e.g., "fragment_sample").
        val name = expr.calleeReference.name.asString()
        // Get the receiver (R.layout part).
        val receiver = expr.explicitReceiver ?: return null

        // After resolution: the receiver is a FirResolvedQualifier for R.layout.
        if (receiver is FirResolvedQualifier) {
            val classId = receiver.classId ?: return null
            // Verify the receiver is the "layout" inner class of R.
            if (classId.shortClassName.asString() == "layout") return name
        }

        // Before resolution: the receiver is a FirPropertyAccessExpression chain.
        if (receiver is FirPropertyAccessExpression) {
            // Check if the receiver's callee is "layout".
            if (receiver.calleeReference.name.asString() == "layout") return name
        }

        return null
    }

    // Converts a snake_case layout resource name to a PascalCase ViewBinding class name.
    // The binding class is resolved under {namespace}.databinding.
    //
    // Examples:
    //   "fragment_sample" → "FragmentSampleBinding" → com.example.app.databinding.FragmentSampleBinding
    //   "fragment_user_profile" → "FragmentUserProfileBinding"
    //
    // Returns null if the namespace is not set (binding generation is skipped).
    private fun layoutNameToBindingClassId(layoutName: String): ClassId? {
        val ns = namespace ?: return null
        // Convert snake_case to PascalCase by splitting on underscores and capitalizing each part.
        val pascalCase = layoutName.split("_").joinToString("") {
            it.replaceFirstChar { c -> c.uppercase() }
        }
        // Append "Binding" suffix (Android ViewBinding convention).
        val bindingClassName = "${pascalCase}Binding"
        // Construct the ClassId under the namespace's databinding package.
        return ClassId(FqName("$ns.databinding"), Name.identifier(bindingClassName))
    }

    // Returns the set of callable names to be generated for a class.
    // For @Screen-annotated Fragment classes with a layout resource: generates "binding".
    // Skips generation if:
    //   - The class is a companion object
    //   - The class is not annotated with @Screen
    //   - The class already has a manually declared "binding" property
    //   - No layout resource is found in the constructor
    //   - The binding class doesn't exist in the databinding package
    //   - The namespace is not set
    @OptIn(DirectDeclarationsAccess::class)
    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: DeclarationGenerationContext.Member,
    ): Set<Name> {
        // Skip companion objects — binding is generated on the fragment class itself.
        if (classSymbol.isCompanion) return emptySet()
        // Only generate for @Screen-annotated classes.
        if (!classSymbol.hasViewBindingAnnotation()) return emptySet()

        // Skip if the class already has a manually declared binding property.
        // This prevents conflicts and respects user-defined implementations.
        val hasManualBinding = classSymbol.declarationSymbols
            .filterIsInstance<FirPropertySymbol>()
            .any { it.name.asString() == "binding" }
        if (hasManualBinding) return emptySet()

        // Extract the layout name from the Fragment's constructor.
        val layoutName = extractContentLayoutId(classSymbol) ?: return emptySet()
        // Convert the layout name to a binding class ClassId.
        val bindingClassId = layoutNameToBindingClassId(layoutName) ?: return emptySet()
        // Verify the binding class exists in the compilation (it's generated by the Android build).
        if (session.symbolProvider.getClassLikeSymbolByClassId(bindingClassId) == null) return emptySet()

        return setOf(Name.identifier("binding"))
    }

    // Generates the `binding` property declaration on the @Screen-annotated Fragment class.
    // Creates a `private val binding: XxxBinding` with no backing field (getter-only).
    // The actual getter body is generated in the IR phase by ViewBindingIrElementTransformer.
    override fun generateProperties(
        callableId: CallableId,
        context: DeclarationGenerationContext.Member?,
    ): List<FirPropertySymbol> {
        // Only handle the "binding" property.
        if (callableId.callableName.asString() != "binding") return emptyList()

        // Resolve the owning class symbol.
        val owner = context?.owner ?: run {
            val className = callableId.className ?: return emptyList()
            val classId = ClassId(callableId.packageName, className, false)
            session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirClassSymbol<*>
        } ?: return emptyList()

        // Skip companion objects.
        if (owner.isCompanion) return emptyList()
        // Only generate for @Screen-annotated classes.
        if (!owner.hasViewBindingAnnotation()) return emptyList()

        // Resolve the binding class from the layout name.
        val layoutName = extractContentLayoutId(owner) ?: return emptyList()
        val bindingClassId = layoutNameToBindingClassId(layoutName) ?: return emptyList()
        // Verify the binding class exists.
        session.symbolProvider.getClassLikeSymbolByClassId(bindingClassId) ?: return emptyList()
        // Create the ConeKotlinType for the binding class.
        val bindingType = bindingClassId.createConeType(session)

        // Create the property declaration: private val binding: XxxBinding.
        // hasBackingField=false because the getter reads from the delegate or calls bind() directly.
        val property = createMemberProperty(
            owner,
            ViewBindingDeclarationKey,
            callableId.callableName,
            bindingType,
            isVal = true,
            hasBackingField = false,
        ) {
            visibility = Visibilities.Private
        }

        return listOf(property.symbol)
    }

    // Helper to check if a class symbol has the @Screen annotation using the predicate-based provider.
    private fun FirClassSymbol<*>.hasViewBindingAnnotation(): Boolean {
        return session.predicateBasedProvider.matches(viewBindingPredicate, this)
    }

    // Register the viewBinding predicate so the FIR infrastructure knows which classes to watch.
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(viewBindingPredicate)
    }
}
