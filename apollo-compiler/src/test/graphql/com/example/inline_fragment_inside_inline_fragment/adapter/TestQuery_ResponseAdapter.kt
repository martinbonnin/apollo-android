// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.inline_fragment_inside_inline_fragment.adapter

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import com.example.inline_fragment_inside_inline_fragment.TestQuery
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
object TestQuery_ResponseAdapter : ResponseAdapter<TestQuery.Data> {
  val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
    ResponseField(
      type = ResponseField.Type.List(ResponseField.Type.Named.Object("SearchResult")),
      responseName = "search",
      fieldName = "search",
      arguments = mapOf<String, Any?>(
        "text" to "bla-bla"),
      conditions = emptyList(),
      possibleFieldSets = mapOf(
        "Droid" to Search.CharacterDroidSearch.RESPONSE_FIELDS,
        "Human" to Search.CharacterHumanSearch.RESPONSE_FIELDS,
        "" to Search.OtherSearch.RESPONSE_FIELDS,
      ),
    )
  )

  override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data {
    return reader.run {
      var search: List<TestQuery.Data.Search?>? = null
      while(true) {
        when (selectField(RESPONSE_FIELDS)) {
          0 -> search = readList<TestQuery.Data.Search>(RESPONSE_FIELDS[0]) { reader ->
            reader.readObject<TestQuery.Data.Search> { reader ->
              Search.fromResponse(reader)
            }
          }
          else -> break
        }
      }
      TestQuery.Data(
        search = search
      )
    }
  }

  override fun toResponse(writer: ResponseWriter, value: TestQuery.Data) {
    writer.writeList(RESPONSE_FIELDS[0], value.search) { value, listItemWriter ->
      listItemWriter.writeObject { writer ->
        Search.toResponse(writer, value)
      }
    }
  }

  object Search : ResponseAdapter<TestQuery.Data.Search> {
<<<<<<< HEAD
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
        responseName = "__typename",
        fieldName = "__typename",
        arguments = emptyMap(),
        conditions = emptyList(),
      )
    )

=======
>>>>>>> e374eee24... forward subfields
    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data.Search {
      val typename = __typename ?: reader.readString(ResponseField.Typename)
      return when(typename) {
        "Droid" -> CharacterDroidSearch.fromResponse(reader, typename)
        "Human" -> CharacterHumanSearch.fromResponse(reader, typename)
        else -> OtherSearch.fromResponse(reader, typename)
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Data.Search) {
      when(value) {
        is TestQuery.Data.Search.CharacterDroidSearch -> CharacterDroidSearch.toResponse(writer, value)
        is TestQuery.Data.Search.CharacterHumanSearch -> CharacterHumanSearch.toResponse(writer, value)
        is TestQuery.Data.Search.OtherSearch -> OtherSearch.toResponse(writer, value)
      }
    }

    object CharacterDroidSearch : ResponseAdapter<TestQuery.Data.Search.CharacterDroidSearch> {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          responseName = "__typename",
          fieldName = "__typename",
          arguments = emptyMap(),
          conditions = emptyList(),
          possibleFieldSets = emptyMap(),
        ),
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          responseName = "name",
          fieldName = "name",
          arguments = emptyMap(),
          conditions = emptyList(),
          possibleFieldSets = emptyMap(),
        ),
        ResponseField(
          type = ResponseField.Type.Named.Other("String"),
          responseName = "primaryFunction",
          fieldName = "primaryFunction",
          arguments = emptyMap(),
          conditions = emptyList(),
          possibleFieldSets = emptyMap(),
        )
      )

      override fun fromResponse(reader: ResponseReader, __typename: String?):
          TestQuery.Data.Search.CharacterDroidSearch {
        return reader.run {
          var __typename: String? = __typename
          var name: String? = null
          var primaryFunction: String? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              1 -> name = readString(RESPONSE_FIELDS[1])
              2 -> primaryFunction = readString(RESPONSE_FIELDS[2])
              else -> break
            }
          }
          TestQuery.Data.Search.CharacterDroidSearch(
            __typename = __typename!!,
            name = name!!,
            primaryFunction = primaryFunction
          )
        }
      }

      override fun toResponse(writer: ResponseWriter,
          value: TestQuery.Data.Search.CharacterDroidSearch) {
        writer.writeString(RESPONSE_FIELDS[0], value.__typename)
        writer.writeString(RESPONSE_FIELDS[1], value.name)
        writer.writeString(RESPONSE_FIELDS[2], value.primaryFunction)
      }
    }

    object CharacterHumanSearch : ResponseAdapter<TestQuery.Data.Search.CharacterHumanSearch> {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          responseName = "__typename",
          fieldName = "__typename",
          arguments = emptyMap(),
          conditions = emptyList(),
          possibleFieldSets = emptyMap(),
        ),
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          responseName = "name",
          fieldName = "name",
          arguments = emptyMap(),
          conditions = emptyList(),
          possibleFieldSets = emptyMap(),
        ),
        ResponseField(
          type = ResponseField.Type.Named.Other("String"),
          responseName = "homePlanet",
          fieldName = "homePlanet",
          arguments = emptyMap(),
          conditions = emptyList(),
          possibleFieldSets = emptyMap(),
        )
      )

      override fun fromResponse(reader: ResponseReader, __typename: String?):
          TestQuery.Data.Search.CharacterHumanSearch {
        return reader.run {
          var __typename: String? = __typename
          var name: String? = null
          var homePlanet: String? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              1 -> name = readString(RESPONSE_FIELDS[1])
              2 -> homePlanet = readString(RESPONSE_FIELDS[2])
              else -> break
            }
          }
          TestQuery.Data.Search.CharacterHumanSearch(
            __typename = __typename!!,
            name = name!!,
            homePlanet = homePlanet
          )
        }
      }

      override fun toResponse(writer: ResponseWriter,
          value: TestQuery.Data.Search.CharacterHumanSearch) {
        writer.writeString(RESPONSE_FIELDS[0], value.__typename)
        writer.writeString(RESPONSE_FIELDS[1], value.name)
        writer.writeString(RESPONSE_FIELDS[2], value.homePlanet)
      }
    }

    object OtherSearch : ResponseAdapter<TestQuery.Data.Search.OtherSearch> {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          responseName = "__typename",
          fieldName = "__typename",
          arguments = emptyMap(),
          conditions = emptyList(),
          possibleFieldSets = emptyMap(),
        )
      )

      override fun fromResponse(reader: ResponseReader, __typename: String?):
          TestQuery.Data.Search.OtherSearch {
        return reader.run {
          var __typename: String? = __typename
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              else -> break
            }
          }
          TestQuery.Data.Search.OtherSearch(
            __typename = __typename!!
          )
        }
      }

      override fun toResponse(writer: ResponseWriter, value: TestQuery.Data.Search.OtherSearch) {
        writer.writeString(RESPONSE_FIELDS[0], value.__typename)
      }
    }
  }
}
