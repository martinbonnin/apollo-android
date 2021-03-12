package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.checkTestFixture
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.integration.interceptor.AllFilmsQuery
import com.apollographql.apollo3.internal.interceptor.ApolloServerInterceptor
import com.apollographql.apollo3.request.RequestHeaders
import com.google.common.base.Predicate
import com.google.common.truth.Truth
import junit.framework.Assert
import okhttp3.*
import okio.Buffer
import okio.Timeout
import org.junit.Test
import java.io.IOException
import java.lang.UnsupportedOperationException
import java.util.concurrent.TimeUnit

class ApolloServerInterceptorTest {
  private val serverUrl = HttpUrl.parse("http://google.com")!!
  private val query = AllFilmsQuery(after = Input.Present("some cursor"), before = Input.Absent, first = Input.Absent, last = Input.Present(100))

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCall() {
    val requestAssertPredicate = Predicate<Request?> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request)
      Truth.assertThat(request!!.url()).isEqualTo(serverUrl)
      Truth.assertThat(request.method()).isEqualTo("POST")
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertRequestBody(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ResponseAdapterCache.DEFAULT,
        ApolloLogger(null))
    interceptor.httpPostCall(query, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testCachedHttpCall() {
    val scalarTypeAdapters = ResponseAdapterCache.DEFAULT
    val cacheKey: String = ApolloServerInterceptor.cacheKey(query, scalarTypeAdapters)
    val requestAssertPredicate = Predicate<Request?> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request)
      Truth.assertThat(request!!.url()).isEqualTo(serverUrl)
      Truth.assertThat(request.method()).isEqualTo("POST")
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isEqualTo(cacheKey)
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isEqualTo("NETWORK_FIRST")
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isEqualTo("10000")
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isEqualTo("false")
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isEqualTo("false")
      Truth.assertThat(request.header(HttpCache.CACHE_DO_NOT_STORE)).isEqualTo("true")
      assertRequestBody(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate),
        HttpCachePolicy.NETWORK_FIRST.expireAfter(10, TimeUnit.SECONDS),
        false,
        scalarTypeAdapters,
        ApolloLogger(null))

    interceptor.httpPostCall(
        operation = query,
        cacheHeaders = CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build(),
        requestHeaders = RequestHeaders.NONE,
        writeQueryDocument = true,
        autoPersistQueries = false)
  }

  @Test
  @Throws(Exception::class)
  fun testAdditionalHeaders() {
    val testHeader1 = "TEST_HEADER_1"
    val testHeaderValue1 = "crappy_value"
    val testHeader2 = "TEST_HEADER_2"
    val testHeaderValue2 = "fantastic_value"
    val testHeader3 = "TEST_HEADER_3"
    val testHeaderValue3 = "awesome_value"
    val requestAssertPredicate = Predicate<Request?> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request)
      Truth.assertThat(request!!.url()).isEqualTo(serverUrl)
      Truth.assertThat(request.method()).isEqualTo("POST")
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      Truth.assertThat(request.header(testHeader1)).isEqualTo(testHeaderValue1)
      Truth.assertThat(request.header(testHeader2)).isEqualTo(testHeaderValue2)
      Truth.assertThat(request.header(testHeader3)).isEqualTo(testHeaderValue3)
      assertRequestBody(request)
      true
    }
    val requestHeaders: RequestHeaders = RequestHeaders.builder()
        .addHeader(testHeader1, testHeaderValue1)
        .addHeader(testHeader2, testHeaderValue2)
        .addHeader(testHeader3, testHeaderValue3)
        .build()
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ResponseAdapterCache.DEFAULT,
        ApolloLogger(null))
    interceptor.httpPostCall(query, CacheHeaders.NONE, requestHeaders, true, false)
  }

  @Test
  @Throws(IOException::class)
  fun testUseHttpGetForQueries() {
    val requestAssertPredicate = Predicate<Request?> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request)
      Truth.assertThat(request!!.method()).isEqualTo("GET")
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      Truth.assertThat(request.url().queryParameter("query")).isEqualTo(query.queryDocument().replace("\n", ""))
      Truth.assertThat(request.url().queryParameter("operationName")).isEqualTo(query.name())
      Truth.assertThat(request.url().queryParameter("variables")).isEqualTo("{\"after\":\"some cursor\",\"last\":100}")
      Truth.assertThat(request.url().queryParameter("extensions")).isEqualTo("{\"persistedQuery\":{\"version\":1," +
          "\"sha256Hash\":\"" + query.operationId() + "\"}}")
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ResponseAdapterCache.DEFAULT,
        ApolloLogger(null))
    interceptor.httpGetCall(query, CacheHeaders.NONE, RequestHeaders.NONE, true, true)
  }

  private fun assertDefaultRequestHeaders(request: Request?) {
    Truth.assertThat(request!!.header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE)).isEqualTo(ApolloServerInterceptor.JSON_CONTENT_TYPE)
    Truth.assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_ID)).isEqualTo(query.operationId())
    Truth.assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_NAME)).isEqualTo(query.name())
    Truth.assertThat(request.tag()).isEqualTo(query.operationId())
  }

  private fun assertRequestBody(request: Request?) {
    Truth.assertThat(request!!.body()!!.contentType().toString()).isEqualTo(ApolloServerInterceptor.JSON_CONTENT_TYPE)
    val bodyBuffer = Buffer()
    try {
      request.body()!!.writeTo(bodyBuffer)
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
    checkTestFixture(bodyBuffer.readUtf8(), "ApolloServerInterceptorTest/interceptorRequestBody.json")
  }

  private class AssertHttpCallFactory(val predicate: Predicate<Request?>) : Call.Factory {
    override fun newCall(request: Request): Call {
      if (!predicate.apply(request)) {
        Assert.fail("Assertion failed")
      }
      return NoOpCall()
    }
  }

  private class NoOpCall : Call {
    override fun request(): Request {
      throw UnsupportedOperationException()
    }

    override fun execute(): Response {
      throw UnsupportedOperationException()
    }

    override fun enqueue(responseCallback: Callback) {}
    override fun cancel() {}
    override fun isExecuted(): Boolean {
      return false
    }

    override fun isCanceled(): Boolean {
      return false
    }

    override fun clone(): Call {
      throw UnsupportedOperationException()
    }

    override fun timeout(): Timeout {
      throw UnsupportedOperationException()
    }
  }
}
