package com.apollographql.apollo.compiler.frontend.ir

internal class MRBuilder(val fir: FIR) {
  fun build(): MR
}