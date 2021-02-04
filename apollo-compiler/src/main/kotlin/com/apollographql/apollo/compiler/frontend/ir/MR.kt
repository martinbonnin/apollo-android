package com.apollographql.apollo.compiler.frontend.ir

class MRInterface(
    val typeCondition: String,
    val fields: List<MRField>,
    val conditionInterfaces: List<MRInterface>,
    //val fieldInterfaces: List<MRInterface>
)

data class MRField(
    val name: String,
    val alias: String?,
    // from the fieldDefinition
    val description: String?,
    // from the fieldDefinition
    val type: Type,
    // from the GQL directives
    val deprecationReason: String?,
    val arguments: List<Argument>,
    val condition: BooleanExpression,
    val mrInterface: MRInterface
)

class MR(
    val operation: List<MROperation>
)

data class MROperation(
    val name: String,
    val operationType: OperationType,
    val typeCondition: String,
    val variables: List<Variable>,
    val description: String?,
    val mrInterface: MRInterface,
    val sourceWithFragments: String,
)

