package com.apollographql.apollo3.compiler.backend

import com.apollographql.apollo3.compiler.backend.ast.AstBuilder.Companion.buildAst
import com.apollographql.apollo3.compiler.backend.codegen.implementationTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.inputObjectAdapterTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.interfaceTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.patchKotlinNativeOptionalArrayProperties
import com.apollographql.apollo3.compiler.backend.codegen.responseAdapterTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.typeSpec
import com.apollographql.apollo3.compiler.backend.codegen.typeSpecs
import com.apollographql.apollo3.compiler.backend.codegen.variablesAdapterTypeSpec
import com.apollographql.apollo3.compiler.backend.ir.BackendIr
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.toIntrospectionSchema
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal class GraphQLCodeGenerator(
    private val backendIr: BackendIr,
    private val schema: Schema,
    private val customScalarsMapping: Map<String, String>,
    private val generateAsInternal: Boolean = false,
    private val operationOutput: OperationOutput,
    private val generateFilterNotNull: Boolean,
    private val enumAsSealedClassPatternFilters: List<Regex>,
    private val enumsToGenerate: Set<String>,
    private val inputObjectsToGenerate: Set<String>,
    private val generateScalarMapping: Boolean,
    private val typesPackageName: String,
    private val fragmentsPackageName: String,
    private val generateFragmentImplementations: Boolean,
    private val generateFragmentsAsInterfaces: Boolean,
) {
  fun write(outputDir: File) {

    val introspectionSchema = schema.toIntrospectionSchema()
    val ast = backendIr.buildAst(
        schema = introspectionSchema,
        customScalarsMapping = customScalarsMapping,
        operationOutput = operationOutput,
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackageName,
        generateFragmentsAsInterfaces = generateFragmentsAsInterfaces,
    )

    if (generateScalarMapping && ast.customScalarTypes.isNotEmpty()) {
      ast.customScalarTypes.values.typeSpec()
          .toFileSpec(typesPackageName)
          .writeTo(outputDir)
    }

    ast.enumTypes
        .filter { enumType -> enumsToGenerate.contains(enumType.graphqlName) }
        .forEach { enumType ->
          fileSpecBuilder(typesPackageName, enumType.name.escapeKotlinReservedWord())
              .apply {
                enumType.typeSpecs(
                    enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters,
                    packageName = typesPackageName
                ).forEach {
                  addType(it.internal(generateAsInternal))
                }
              }.build()
              .writeTo(outputDir)
        }

    ast.inputTypes
        .filter { inputType -> inputObjectsToGenerate.contains(inputType.graphqlName) }
        .forEach { inputType ->
          inputType.typeSpec()
              .toFileSpec(typesPackageName)
              .writeTo(outputDir)
          inputType.fields.inputObjectAdapterTypeSpec(typesPackageName, inputType.name)
              .toFileSpec("${typesPackageName}.adapter")
              .writeTo(outputDir)
        }

    ast.fragmentTypes
        .forEach { fragmentType ->
          if (generateFragmentsAsInterfaces) {
            fragmentType
                .interfaceTypeSpec()
                .toFileSpec(fragmentsPackageName)
                .writeTo(outputDir)
          }

          if (generateFragmentImplementations || !generateFragmentsAsInterfaces) {
            fragmentType
                .implementationTypeSpec(generateFragmentsAsInterfaces)
                .toFileSpec(fragmentsPackageName)
                .writeTo(outputDir)

            fragmentType.variables.variablesAdapterTypeSpec(fragmentsPackageName, fragmentType.implementationType.name)
                .toFileSpec("${fragmentsPackageName}.adapter")
                .writeTo(outputDir)

            fragmentType
                .responseAdapterTypeSpec(generateFragmentsAsInterfaces)
                .toFileSpec("${fragmentsPackageName}.adapter")
                .writeTo(outputDir)
          }
        }

    ast.operationTypes.forEach { operationType ->
      operationType.typeSpec(
              targetPackage = operationType.packageName,
              generateFragmentsAsInterfaces = generateFragmentsAsInterfaces,
          )
          .let {
            if (generateFilterNotNull) {
              it.patchKotlinNativeOptionalArrayProperties()
            } else it
          }
          .toFileSpec(operationType.packageName)
          .writeTo(outputDir)

      operationType.variables.variablesAdapterTypeSpec(operationType.packageName, operationType.name)
          .toFileSpec("${operationType.packageName}.adapter")
          .writeTo(outputDir)
      operationType.responseAdapterTypeSpec(generateFragmentsAsInterfaces)
          .toFileSpec("${operationType.packageName}.adapter")
          .writeTo(outputDir)
    }
  }

  /**
   * Generates a file with the name of this type and the specified package name
   */
  private fun TypeSpec.toFileSpec(packageName: String) = fileSpecBuilder(packageName, name!!)
      .addType(this.internal(generateAsInternal))
      .build()

  private fun fileSpecBuilder(packageName: String, name: String): FileSpec.Builder =
      FileSpec
          .builder(packageName, name)
          .addComment("AUTO-GENERATED FILE. DO NOT MODIFY.\n\n" +
              "This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.\n" +
              "It should not be modified by hand.\n"
          )

  private fun TypeSpec.internal(generateAsInternal: Boolean): TypeSpec {
    return if (!generateAsInternal) {
      this
    } else {
      this.toBuilder().addModifiers(KModifier.INTERNAL).build()
    }
  }
}
