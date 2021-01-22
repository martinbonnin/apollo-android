// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.simple_fragment.fragment.adapter

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import com.example.simple_fragment.fragment.HeroDetailsImpl
import kotlin.Array
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
internal object HeroDetailsImpl_ResponseAdapter : ResponseAdapter<HeroDetailsImpl.Data> {
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
  override fun fromResponse(reader: ResponseReader, __typename: String?): HeroDetailsImpl.Data {
    val typename = __typename ?: reader.readString(ResponseField.Typename)
    return when(typename) {
      "Human" -> HumanData.fromResponse(reader, typename)
      else -> OtherData.fromResponse(reader, typename)
    }
  }

  override fun toResponse(writer: ResponseWriter, value: HeroDetailsImpl.Data) {
    when(value) {
      is HeroDetailsImpl.Data.HumanData -> HumanData.toResponse(writer, value)
      is HeroDetailsImpl.Data.OtherData -> OtherData.toResponse(writer, value)
    }
  }

  object HumanData : ResponseAdapter<HeroDetailsImpl.Data.HumanData> {
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
      )
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?):
        HeroDetailsImpl.Data.HumanData {
      return reader.run {
        var __typename: String? = __typename
        var name: String? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> __typename = readString(RESPONSE_FIELDS[0])
            1 -> name = readString(RESPONSE_FIELDS[1])
            else -> break
          }
        }
        HeroDetailsImpl.Data.HumanData(
          __typename = __typename!!,
          name = name!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: HeroDetailsImpl.Data.HumanData) {
      writer.writeString(RESPONSE_FIELDS[0], value.__typename)
      writer.writeString(RESPONSE_FIELDS[1], value.name)
    }
  }

  object OtherData : ResponseAdapter<HeroDetailsImpl.Data.OtherData> {
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
        HeroDetailsImpl.Data.OtherData {
      return reader.run {
        var __typename: String? = __typename
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> __typename = readString(RESPONSE_FIELDS[0])
            else -> break
          }
        }
        HeroDetailsImpl.Data.OtherData(
          __typename = __typename!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: HeroDetailsImpl.Data.OtherData) {
      writer.writeString(RESPONSE_FIELDS[0], value.__typename)
    }
  }
}
