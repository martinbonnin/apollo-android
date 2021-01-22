// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.recursive_selection

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.recursive_selection.adapter.TestQuery_ResponseAdapter
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery : Query<TestQuery.Data> {
  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun name(): String = OPERATION_NAME

  override fun adapter(): ResponseAdapter<Data> = TestQuery_ResponseAdapter
  override fun responseFields(): Array<ResponseField> = TestQuery_ResponseAdapter.RESPONSE_FIELDS
  /**
   * The query type, represents all of the entry points into our object graph
   */
  data class Data(
    val tree: Tree?
  ) : Operation.Data {
    /**
     * To test recursive structures
     */
    data class Tree(
      val name: String,
      val children: List<Child>,
      val parent: Parent?
    ) {
      /**
       * To test recursive structures
       */
      data class Child(
        val name: String
      )

      /**
       * To test recursive structures
       */
      data class Parent(
        val name: String
      )
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "0308cbb678ba65068f98c1e2db76c79bc46b6d4a171d6310a4bb5d98356651c5"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  tree {
          |    name
          |    children {
          |      name
          |    }
          |    parent {
          |      name
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
