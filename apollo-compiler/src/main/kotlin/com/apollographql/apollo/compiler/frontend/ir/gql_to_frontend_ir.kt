package com.apollographql.apollo.compiler.frontend.ir

import com.apollographql.apollo.compiler.frontend.GQLArgument
import com.apollographql.apollo.compiler.frontend.GQLBooleanValue
import com.apollographql.apollo.compiler.frontend.GQLDirective
import com.apollographql.apollo.compiler.frontend.GQLField
import com.apollographql.apollo.compiler.frontend.GQLFieldDefinition
import com.apollographql.apollo.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo.compiler.frontend.GQLListType
import com.apollographql.apollo.compiler.frontend.GQLNamedType
import com.apollographql.apollo.compiler.frontend.GQLNonNullType
import com.apollographql.apollo.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo.compiler.frontend.GQLSelectionSet
import com.apollographql.apollo.compiler.frontend.GQLType
import com.apollographql.apollo.compiler.frontend.GQLTypeDefinition
import com.apollographql.apollo.compiler.frontend.GQLVariableDefinition
import com.apollographql.apollo.compiler.frontend.GQLVariableValue
import com.apollographql.apollo.compiler.frontend.Schema
import com.apollographql.apollo.compiler.frontend.definitionFromScope
import com.apollographql.apollo.compiler.frontend.findDeprecationReason
import com.apollographql.apollo.compiler.frontend.leafType
import com.apollographql.apollo.compiler.frontend.responseName
import com.apollographql.apollo.compiler.frontend.rootTypeDefinition
import com.apollographql.apollo.compiler.frontend.toUtf8
import com.apollographql.apollo.compiler.frontend.toUtf8WithIndents
import com.apollographql.apollo.compiler.frontend.usedFragmentNames
import com.apollographql.apollo.compiler.frontend.validateAndCoerce
import java.math.BigInteger

internal class FrontendIrBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    metadataFragmentDefinitions: List<GQLFragmentDefinition>,
    fragmentDefinitions: List<GQLFragmentDefinition>
) {
  private val allGQLFragmentDefinitions = (metadataFragmentDefinitions + fragmentDefinitions).associateBy {
    it.name
  }

  private val irFragmentDefinitions = fragmentDefinitions.map {
    it.toIrNamedFragmentDefinition()
  }

  // For metadataFragments, we transform them to IR multiple times, in each module. This is a bit silly but
  // there's no real alternative as we still need the GQLFragmentDefinition to perform validation
  private val allFragmentDefinitions = (irFragmentDefinitions + metadataFragmentDefinitions.map {
    it.toIrNamedFragmentDefinition()
  }).associateBy { it.name }

  fun build(): FrontendIr {
    return FrontendIr(
        operations = operationDefinitions.map {
          it.toIrOperation()
        },
        fragmentDefinitions = irFragmentDefinitions,
        allFragmentDefinitions = allFragmentDefinitions
    )
  }

  private fun GQLOperationDefinition.toIrOperation(): FrontendIr.Operation {
    val typeDefinition = rootTypeDefinition(schema)
        ?: throw IllegalStateException("ApolloGraphql: cannot find root type for '$operationType'")

    val fragmentNames = usedFragmentNames(schema, allGQLFragmentDefinitions)

    return FrontendIr.Operation(
        name = name ?: throw IllegalStateException("Apollo doesn't support anonymous operation."),
        operationType = operationType.toIrOperationType(),
        variables = variableDefinitions.map { it.toIr() },
        typeDefinition = typeDefinition,
        dataField = buildField(
            gqlSelectionSets = listOf(selectionSet),
            name = "data",
            type = FrontendIr.Type.Named(typeDefinition)
        ),
        description = description,
        sourceWithFragments = (toUtf8WithIndents() + "\n" + fragmentNames.joinToString(
            separator = "\n"
        ) { fragmentName ->
          allGQLFragmentDefinitions[fragmentName]!!.toUtf8WithIndents()
        }).trimEnd('\n'),
        gqlOperationDefinition = this
    )
  }

  private fun GQLFragmentDefinition.toIrNamedFragmentDefinition(): FrontendIr.NamedFragmentDefinition {
    val typeDefinition = schema.typeDefinition(typeCondition.name)
    return FrontendIr.NamedFragmentDefinition(
        name = name,
        description = description,
        dataField = buildField(
            gqlSelectionSets = listOf(selectionSet),
            name = "data",
            type = FrontendIr.Type.Named(typeDefinition)
        ),
        typeCondition = typeDefinition,
        source = toUtf8WithIndents(),
        gqlFragmentDefinition = this,
        variables = selectionSet.inferredVariables(typeDefinition).map { FrontendIr.Variable(it.key, null, it.value.toIr()) }
    )
  }

  private fun GQLSelectionSet.inferredVariables(typeDefinitionInScope: GQLTypeDefinition): Map<String, GQLType> {
    return selections.fold(emptyMap()) { acc, selection ->
      acc + when (selection) {
        is GQLField -> selection.inferredVariables(typeDefinitionInScope)
        is GQLInlineFragment -> selection.inferredVariables()
        is GQLFragmentSpread -> selection.inferredVariables()
      }
    }
  }

  /*private fun createImplementationField(
      gqlField: GQLField,
      gqlFieldDefinition: GQLFieldDefinition,
  ) : FrontendIr.ImplementationField {
    return createImplementationField(
        gqlSelectionSets = gqlField.selectionSet,
        description = gqlFieldDefinition.description,
        deprecationReason = gqlFieldDefinition.directives.findDeprecationReason(),
    )
  }*/

  private fun buildField(
      gqlSelectionSets: List<GQLSelectionSet>,
      name: String,
      type: FrontendIr.Type,
      alias: String? = null,
      description: String? = null,
      deprecationReason: String? = null,
      canBeSkipped: Boolean = false,
      arguments: List<FrontendIr.Argument> = emptyList(),
      condition: BooleanExpression = BooleanExpression.True
  ) : FrontendIr.Field {

    val iface = buildInterfaceShapes(gqlSelectionSets, type.leafTypeDefinition.name)

    return FrontendIr.Field(
        fieldInfo = FrontendIr.FieldInfo(
            responseName = alias ?: name,
            description = description,
            deprecationReason =  deprecationReason,
            type = type,
            canBeSkipped = canBeSkipped
        ),
        name = name,
        alias = alias,
        arguments = arguments,
        condition = condition,
        interfaceShapes = iface,
        implementations = emptyList()
    )
  }

  private fun buildInterfaceShapes(
      gqlSelectionSets: List<GQLSelectionSet>,
      parentType: String
  ): FrontendIr.InterfaceShapes? {
    if (gqlSelectionSets.isEmpty()) {
      return null
    }
    val selections = gqlSelectionSets.flatMap { it.selections }
    val inlineFragments = selections.filterIsInstance<GQLInlineFragment>()
    val typeConditions = (listOf(parentType) + inlineFragments.map { it.typeCondition.name }).toSet()

    val variants = typeConditions.map { typeCondition ->
      val selfFields = selections.filterIsInstance<GQLField>()
      val inlineFragmentsFields = inlineFragments.filter { it.typeCondition.name == typeCondition }
          .flatMap { it.selectionSet.selections }
          .filterIsInstance<GQLField>()

      val fields = (selfFields + inlineFragmentsFields).groupBy { it.responseName() }.values.map { gqlFieldList ->
        val field = gqlFieldList.first()
        val gqlFieldDefinition = field.definitionFromScope(schema, schema.typeDefinition(typeCondition))!!
        FrontendIr.InterfaceField(
            info = FrontendIr.FieldInfo(
                description = gqlFieldDefinition.description,
                deprecationReason = gqlFieldDefinition.directives.findDeprecationReason(),
                responseName = field.alias ?: field.name,
                type = gqlFieldDefinition.type.toIr(),
                canBeSkipped = false
            ),
            iface = buildInterfaceShapes(gqlFieldList.mapNotNull { it.selectionSet }, gqlFieldDefinition.type.leafType().name)
        )
      }
      FrontendIr.InterfaceVariant(
          typeCondition,
          fields
      )
    }


    return FrontendIr.InterfaceShapes(
        variants = variants
    )
  }

  private fun GQLInlineFragment.inferredVariables() = selectionSet.inferredVariables(schema.typeDefinition(typeCondition.name))

  private fun GQLFragmentSpread.inferredVariables(): Map<String, GQLType> {
    val fragmentDefinition = allGQLFragmentDefinitions[name]!!

    return fragmentDefinition.selectionSet.inferredVariables(schema.typeDefinition(fragmentDefinition.typeCondition.name))
  }

  private fun GQLField.inferredVariables(typeDefinitionInScope: GQLTypeDefinition): Map<String, GQLType> {
    val fieldDefinition = definitionFromScope(schema, typeDefinitionInScope)!!

    return (arguments?.arguments?.mapNotNull { argument ->
      if (argument.value is GQLVariableValue) {
        val type = fieldDefinition.arguments.first { it.name == argument.name }.type
        argument.name to type
      } else {
        null
      }
    }?.toMap() ?: emptyMap()) + (selectionSet?.inferredVariables(schema.typeDefinition(fieldDefinition.type.leafType().name)) ?: emptyMap())
  }

  private fun GQLVariableDefinition.toIr(): FrontendIr.Variable {
    return FrontendIr.Variable(
        name = name,
        defaultValue = defaultValue?.validateAndCoerce(type, schema, null)?.orThrow(),
        type = type.toIr(),
    )
  }

  private fun GQLType.toIr(): FrontendIr.Type {
    return when (this) {
      is GQLNonNullType -> FrontendIr.Type.NonNull(ofType = type.toIr())
      is GQLListType -> FrontendIr.Type.List(ofType = type.toIr())
      is GQLNamedType -> FrontendIr.Type.Named(typeDefinition = schema.typeDefinition(name))
    }
  }

  /**
   * The result of collecting fields.
   *
   * @param baseType: the baseType of the object whose fields we will collect.
   * @param typeConditions: the map of type conditions used by the users in the query to the list of variables that make their shape change.
   * typeConditions might contain [baseType] if a user explicitly mentioned it
   */
  data class CollectionResult(
      val baseType: String,
      val collectedFields: List<CollectedField>,
      val collectedNamedFragments: List<CollectedNamedFragment>,
      val collectedInlineFragments: List<CollectedInlineFragment>,
      val typeConditions: Map<String, Set<String>>,
      val gqlSelectionSets: List<GQLSelectionSet>
  )

  data class CollectedField(
      val gqlField: GQLField,
      /**
       * the parent type of this field
       */
      val parentTypeDefinition: GQLTypeDefinition,
      /**
       * The definition of this field. This can also be computed from [gqlField] and [parentTypeDefinition]
       * It is added here for convenience
       */
      val fieldDefinition: GQLFieldDefinition,
      /**
       * Whether this field is nullable in the final model. This can happen if:
       * - the field has a non-trivial @include/@skip directive
       * - the field belongs to an inline fragment with a non-trivial @include/@skip directive
       */
      val canBeSkipped: Boolean,
      /**
       * The real condition for this field to be included in the response.
       * This is used by the cache to not over fetch.
       */
      val booleanExpression: BooleanExpression,
      /**
       * The condition for this field to be included in the model shape.
       * - for inline fragments, this only includes the type conditions as field can be made nullable later (cf [canBeSkipped])
       * - for named fragments, this also includes the @include/@skip directive on fragments
       * This is used by codegen to generate the models
       */
      val shapeBooleanExpression: BooleanExpression,
  )

  data class CollectedNamedFragment(
      val name: String,
      val booleanExpression: BooleanExpression
  )

  data class CollectedInlineFragment(
      // we identify inline fragments by their path, i.e. the typeconditions joined
      val path: String,
  )


  /**
   * A helper class to collect fields in a given object/interface
   *
   */
  private class CollectionScope(
      val schema: Schema,
      val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
      val gqlSelectionSets: List<GQLSelectionSet>,
      val baseType: String
  ) {
    private val collectedField = mutableListOf<CollectedField>()
    private val collectedNamedFragment = mutableListOf<CollectedNamedFragment>()
    private val collectedInlineFragment = mutableListOf<CollectedInlineFragment>()
    private val typeConditions = mutableMapOf<String, Set<String>>()

    fun collect(): CollectionResult {
      check(collectedField.isEmpty()) {
        "CollectionScope can only be used once, please create a new CollectionScope"
      }

      typeConditions[baseType] = emptySet()

      gqlSelectionSets.forEach {
        it.collect(BooleanExpression.True, BooleanExpression.True, baseType, false)
      }
      return CollectionResult(
          baseType = baseType,
          collectedFields = collectedField,
          collectedNamedFragments = collectedNamedFragment,
          collectedInlineFragments = collectedInlineFragment,
          typeConditions = typeConditions,
          gqlSelectionSets = gqlSelectionSets
      )
    }

    @Suppress("NAME_SHADOWING")
    private fun GQLSelectionSet.collect(
        condition: BooleanExpression,
        shapeCondition: BooleanExpression,
        parentType: String,
        canBeSkipped: Boolean
    ) {
      val condition = condition.and(BooleanExpression.Type(parentType))
      val shapeCondition = shapeCondition.and(BooleanExpression.Type(parentType))

      selections.forEach {
        when (it) {
          is GQLField -> {
            val typeDefinition = schema.typeDefinition(parentType)
            val fieldDefinition = it.definitionFromScope(schema, typeDefinition)
                ?: error("cannot find definition for ${it.name} in $parentType")

            /**
             * This is where we decide whether we will force a field as nullable
             */
            val localCondition = it.directives.toCondition()
            val nullable = canBeSkipped || localCondition != BooleanExpression.True

            collectedField.add(
                CollectedField(
                    gqlField = it,
                    parentTypeDefinition = typeDefinition,
                    fieldDefinition = fieldDefinition,
                    canBeSkipped = nullable,
                    booleanExpression = condition.and(localCondition),
                    shapeBooleanExpression = shapeCondition
                )
            )
          }
          is GQLInlineFragment -> {
            val directiveCondition = it.directives.toCondition()

            typeConditions.putIfAbsent(it.typeCondition.name, emptySet())
            it.selectionSet.collect(
                condition.and(directiveCondition),
                shapeCondition,
                it.typeCondition.name,
                directiveCondition != BooleanExpression.True
            )
          }
          is GQLFragmentSpread -> {
            val gqlFragmentDefinition = allGQLFragmentDefinitions[it.name]!!

            val directivesCondition = it.directives.toCondition()

            val fragmentCondition = condition.and(directivesCondition)
            val fragmentShapeCondition = shapeCondition.and(directivesCondition)

            collectedNamedFragment.add(CollectedNamedFragment(it.name, fragmentCondition))

            typeConditions.merge(gqlFragmentDefinition.typeCondition.name, directivesCondition.extractVariables()) { oldValue, newValue ->
              oldValue.union(newValue)
            }

            gqlFragmentDefinition.selectionSet.collect(
                fragmentCondition,
                fragmentShapeCondition,
                gqlFragmentDefinition.typeCondition.name,
                false
            )
          }
        }
      }
    }
  }

  /**
   * Given a list of selection sets sharing the same baseType, collect all fields and returns the CollectionResult
   */
  private fun List<GQLSelectionSet>.collectFields(baseType: String) = CollectionScope(schema, allGQLFragmentDefinitions, this, baseType).collect()

  /**
   * Transforms this [CollectionResult] to a list of [FrontendIr.FieldSet]. This is where the different shapes are created and deduped
   */


  private fun FrontendIr.FieldSetCondition.mergeWith(other: FrontendIr.FieldSetCondition): FrontendIr.FieldSetCondition? {
    val union = this.vars.union(other.vars)
    val intersection = this.vars.intersect(other.vars)

    if (union.subtract(intersection).size <= 1) {
      // 0 means they're the same condition
      // 1 means they can be combined: ( A & !B | A & B = A)
      return FrontendIr.FieldSetCondition(intersection)
    }
    return null
  }
  private fun GQLArgument.toIrArgument(fieldDefinition: GQLFieldDefinition): FrontendIr.Argument {
    val inputValueDefinition = fieldDefinition.arguments.first { it.name == name }

    return FrontendIr.Argument(
        name = name,
        value = value.validateAndCoerce(inputValueDefinition.type, schema, null).orThrow(),
        defaultValue = inputValueDefinition.defaultValue?.validateAndCoerce(inputValueDefinition.type, schema, null)?.orThrow(),
        type = inputValueDefinition.type.toIr(),

        )
  }

  private fun String.toIrOperationType(): OperationType {
    return when (this) {
      "query" -> OperationType.Query
      "mutation" -> OperationType.Mutation
      "subscription" -> OperationType.Subscription
      else -> throw IllegalStateException("ApolloGraphQL: unknown operationType $this.")
    }
  }

  enum class OperationType {
    Query,
    Mutation,
    Subscription
  }

  companion object {
    private fun List<GQLDirective>.toCondition(): BooleanExpression {
      val conditions = mapNotNull {
        it.toCondition()
      }
      return if (conditions.isEmpty()) {
        BooleanExpression.True
      } else {
        check(conditions.toSet().size == conditions.size) {
          "ApolloGraphQL: duplicate @skip/@include directives are not allowed"
        }
        // Having both @skip and @include is allowed
        // 3.13.2 In the case that both the @skip and @include directives are provided on the same field or fragment,
        // it must be queried only if the @skip condition is false and the @include condition is true.
        BooleanExpression.And(conditions.toSet())
      }
    }

    private fun GQLDirective.toCondition(): BooleanExpression? {
      if (setOf("skip", "include").contains(name).not()) {
        // not a condition directive
        return null
      }
      if (arguments?.arguments?.size != 1) {
        throw IllegalStateException("ApolloGraphQL: wrong number of arguments for '$name' directive: ${arguments?.arguments?.size}")
      }

      val argument = arguments.arguments.first()

      return when (val value = argument.value) {
        is GQLBooleanValue -> {
          if (value.value) BooleanExpression.True else BooleanExpression.False
        }
        is GQLVariableValue -> BooleanExpression.Variable(
            name = value.name,
        ).let {
          if (name == "skip") it.not() else it
        }
        else -> throw IllegalStateException("ApolloGraphQL: cannot pass ${value.toUtf8()} to '$name' directive")
      }
    }

    internal fun BooleanExpression.extractVariables(): Set<String> = when (this) {
      is BooleanExpression.Or -> booleanExpressions.flatMap { it.extractVariables() }.toSet()
      is BooleanExpression.And -> booleanExpressions.flatMap { it.extractVariables() }.toSet()
      is BooleanExpression.Variable -> setOf(this.name)
      else -> emptySet()
    }

    /**
     * Given a set of variable names, generate all possible variable values
     */
    private fun Set<String>.possibleVariableValues(): List<Set<String>> {
      val asList = toList()

      val pow = BigInteger.valueOf(2).pow(size).toInt()
      val list = mutableListOf<Set<String>>()
      for (i in 0.until(pow)) {
        val set = mutableSetOf<String>()
        for (j in 0.until(size)) {
          if (i.and(1.shl(j)) != 0) {
            set.add(asList[j])
          }
        }
        list.add(set)
      }

      return list
    }
  }
}
