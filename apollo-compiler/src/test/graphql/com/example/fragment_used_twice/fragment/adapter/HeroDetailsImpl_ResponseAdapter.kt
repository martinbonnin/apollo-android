// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_used_twice.fragment.adapter

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import com.example.fragment_used_twice.fragment.HeroDetailsImpl
import kotlin.Any
import kotlin.Array
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
object HeroDetailsImpl_ResponseAdapter : ResponseAdapter<HeroDetailsImpl.Data> {
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
      "Droid" -> CharacterData.fromResponse(reader, typename)
      "Human" -> CharacterData.fromResponse(reader, typename)
      else -> OtherData.fromResponse(reader, typename)
    }
  }

  override fun toResponse(writer: ResponseWriter, value: HeroDetailsImpl.Data) {
    when(value) {
      is HeroDetailsImpl.Data.CharacterData -> CharacterData.toResponse(writer, value)
      is HeroDetailsImpl.Data.OtherData -> OtherData.toResponse(writer, value)
    }
  }

  object CharacterData : ResponseAdapter<HeroDetailsImpl.Data.CharacterData> {
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
        type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("Date")),
        responseName = "birthDate",
        fieldName = "birthDate",
        arguments = emptyMap(),
        conditions = emptyList(),
        possibleFieldSets = emptyMap(),
      )
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?):
        HeroDetailsImpl.Data.CharacterData {
      return reader.run {
        var __typename: String? = __typename
        var name: String? = null
        var birthDate: Any? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> __typename = readString(RESPONSE_FIELDS[0])
            1 -> name = readString(RESPONSE_FIELDS[1])
            2 -> birthDate = readCustomScalar<Any>(RESPONSE_FIELDS[2])
            else -> break
          }
        }
        HeroDetailsImpl.Data.CharacterData(
          __typename = __typename!!,
          name = name!!,
          birthDate = birthDate!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: HeroDetailsImpl.Data.CharacterData) {
      writer.writeString(RESPONSE_FIELDS[0], value.__typename)
      writer.writeString(RESPONSE_FIELDS[1], value.name)
      writer.writeCustom(RESPONSE_FIELDS[2], value.birthDate)
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
        HeroDetailsImpl.Data.OtherData {
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
        HeroDetailsImpl.Data.OtherData(
          __typename = __typename!!,
          name = name!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: HeroDetailsImpl.Data.OtherData) {
      writer.writeString(RESPONSE_FIELDS[0], value.__typename)
      writer.writeString(RESPONSE_FIELDS[1], value.name)
    }
  }
}
