// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.interface_on_interface

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.interface_on_interface.adapter.GetHuman_ResponseAdapter
import kotlin.Array
import kotlin.Double
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class GetHuman : Query<GetHuman.Data> {
  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun name(): String = OPERATION_NAME

  override fun adapter(): ResponseAdapter<Data> = GetHuman_ResponseAdapter
  override fun responseFields(): Array<ResponseField> = GetHuman_ResponseAdapter.RESPONSE_FIELDS
  data class Data(
    val human: Human,
    val node: Node
  ) : Operation.Data {
    data class Human(
      val id: String,
      val name: String,
      val height: Double
    )

    interface Node {
      val __typename: String

      interface Human : Node {
        override val __typename: String

        val height: Double
      }

      data class HumanNode(
        override val __typename: String,
        override val height: Double
      ) : Node, Human

      data class OtherNode(
        override val __typename: String
      ) : Node

      companion object {
        fun Node.asHuman(): Human? = this as? Human
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "b5ec6431463438b91c2cf1c33eb4b6a1f9e2580b51fcf5150ef3685b9108a12c"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query GetHuman {
          |  human {
          |    id
          |    name
          |    height
          |  }
          |  node {
          |    __typename
          |    ... on Human {
          |      height
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "GetHuman"
  }
}
