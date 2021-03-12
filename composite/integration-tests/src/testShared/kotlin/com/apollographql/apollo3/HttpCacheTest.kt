package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders.Companion.builder
import com.apollographql.apollo3.cache.http.ApolloHttpCache
import com.apollographql.apollo3.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo3.cache.http.internal.FileSystem
import com.apollographql.apollo3.coroutines.await
import com.apollographql.apollo3.coroutines.toDeferred
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.DroidDetailsQuery
import com.apollographql.apollo3.integration.httpcache.type.CustomScalars
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HttpCacheTest {
  private lateinit var apolloClient: ApolloClient

  private var lastHttRequest: Request? = null
  private var lastHttResponse: Response? = null
  private lateinit var cacheStore: MockHttpCacheStore
  private lateinit var okHttpClient: OkHttpClient

  val server = MockWebServer()
  val inMemoryFileSystem = InMemoryFileSystem()

  @Before
  fun setUp() {
    val dateCustomScalarAdapter: ResponseAdapter<Date> = object : ResponseAdapter<Date> {
      private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

      override fun fromResponse(reader: JsonReader, responseAdapterCache: ResponseAdapterCache): Date {
        return DATE_FORMAT.parse(reader.nextString())
      }

      override fun toResponse(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache, value: Date) {
        writer.value(DATE_FORMAT.format(value))
      }
    }

    cacheStore = MockHttpCacheStore()
    cacheStore.delegate = DiskLruHttpCacheStore(inMemoryFileSystem, File("/cache/"), Int.MAX_VALUE.toLong())
    val cache: HttpCache = ApolloHttpCache(cacheStore, null)
    okHttpClient = OkHttpClient.Builder()
        .addInterceptor(TrackingInterceptor())
        .addInterceptor(cache.interceptor())
        .dispatcher(Dispatcher(immediateExecutorService()))
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .dispatcher(immediateExecutor())
        .addCustomScalarAdapter(CustomScalars.Date, dateCustomScalarAdapter)
        .httpCache(cache)
        .build()
  }

  @After
  fun tearDown() {
    try {
      apolloClient.clearHttpCache()
      server.shutdown()
    } catch (ignore: Exception) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun prematureDisconnect() {
    val mockResponse = mockResponse("/HttpCacheTestAllPlanets.json")
    val truncatedBody = Buffer()
    truncatedBody.write(mockResponse.body, 16)
    mockResponse.body = truncatedBody
    server.enqueue(mockResponse)
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY))
        .test()
        .assertError(ApolloException::class.java)
    checkNoCachedResponse()
  }

  @Test
  @Throws(Exception::class)
  fun cacheDefault() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun cacheSeveralResponses() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    enqueueResponse("/HttpCacheTestDroidDetails.json")
    Rx2Apollo.from(apolloClient
        .query(DroidDetailsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestDroidDetails.json")
    enqueueResponse("/HttpCacheTestAllFilms.json")
    Rx2Apollo.from(apolloClient
        .query(AllFilmsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestAllFilms.json")
  }

  @Test
  @Throws(Exception::class)
  fun noCacheStore() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    val apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(OkHttpClient.Builder()
            .addInterceptor(TrackingInterceptor())
            .dispatcher(Dispatcher(immediateExecutorService()))
            .build())
        .dispatcher(immediateExecutor())
        .build()
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkNoCachedResponse()
  }

  @Test
  @Throws(Exception::class)
  fun networkOnly() = runBlocking {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    apolloClient.query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)
        .toDeferred()
        .await()

    Truth.assertThat(server.requestCount).isEqualTo(1)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun networkOnly_DoNotStore() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)
        .cacheHeaders(builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    Truth.assertThat(server.requestCount).isEqualTo(1)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    checkNoCachedResponse()
  }

  @Test
  @Throws(Exception::class)
  fun networkOnly_responseWithGraphError_noCached() {
    enqueueResponse("/ResponseError.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()).httpCachePolicy(HttpCachePolicy.NETWORK_ONLY))
        .test()
        .assertValue { response -> response.hasErrors() }
    Truth.assertThat(server.requestCount).isEqualTo(1)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    checkNoCachedResponse()
  }

  @Test
  @Throws(Exception::class)
  fun cacheOnlyHit() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    Truth.assertThat(server.takeRequest()).isNotNull()
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_ONLY))
        .test()
        .assertValue { response -> !response.hasErrors() }
    Truth.assertThat(server.requestCount).isEqualTo(1)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNotNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun cacheOnlyMiss() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_ONLY))
        .test()
        .assertError(ApolloHttpException::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun cacheNonStale() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    Truth.assertThat(server.takeRequest()).isNotNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(1)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNotNull()
  }

  @Test
  @Throws(Exception::class)
  fun cacheAfterDelete() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    cacheStore.delegate?.delete()
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
        .test()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    Truth.assertThat(server.requestCount).isEqualTo(2)
  }

  @Test
  @Throws(Exception::class)
  fun cacheStale() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(1)
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(2)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun cacheStaleBeforeNetwork() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(1)
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()).httpCachePolicy(HttpCachePolicy.NETWORK_FIRST))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(2)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun cacheStaleBeforeNetworkError() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(1)
    server.enqueue(MockResponse().setResponseCode(504).setBody(""))
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_FIRST))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(2)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNotNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun cacheUpdate() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(1)
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    enqueueResponse("/HttpCacheTestAllPlanets2.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(2)
    checkCachedResponse("/HttpCacheTestAllPlanets2.json")
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    enqueueResponse("/HttpCacheTestAllPlanets2.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(2)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNotNull()
    checkCachedResponse("/HttpCacheTestAllPlanets2.json")
  }

  @Test
  @Throws(IOException::class, ApolloException::class)
  fun fileSystemUnavailable() {
    cacheStore.delegate = DiskLruHttpCacheStore(NoFileSystem(), File("/cache/"), Int.MAX_VALUE.toLong())
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkNoCachedResponse()
  }

  @Test
  @Throws(IOException::class, ApolloException::class)
  fun fileSystemWriteFailure() {
    val faultyCacheStore = FaultyHttpCacheStore(FileSystem.SYSTEM)
    cacheStore.delegate = faultyCacheStore
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_HEADER_WRITE)
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkNoCachedResponse()
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_BODY_WRITE)
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkNoCachedResponse()
  }

  @Test
  @Throws(IOException::class, ApolloException::class)
  fun fileSystemReadFailure() {
    val faultyCacheStore = FaultyHttpCacheStore(inMemoryFileSystem)
    cacheStore.delegate = faultyCacheStore
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_HEADER_READ)
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
        .test()
        .assertValue { response -> !response.hasErrors() }
    Truth.assertThat(server.requestCount).isEqualTo(2)
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_BODY_READ)
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
        .test()
        .assertError(Exception::class.java)
    Truth.assertThat(server.requestCount).isEqualTo(2)
  }

  @Test
  @Throws(IOException::class, ApolloException::class)
  fun expireAfterRead() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_ONLY.expireAfterRead()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkNoCachedResponse()
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_ONLY))
        .test()
        .assertError(Exception::class.java)
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient.query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(IOException::class, ApolloException::class)
  fun cacheNetworkError() {
    server.enqueue(MockResponse().setResponseCode(504).setBody(""))
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertError(Exception::class.java)
    checkNoCachedResponse()
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery()))
        .test()
        .assertValue { response -> !response.hasErrors() }
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_ONLY))
        .test()
        .assertValue { response -> !response.hasErrors() }
  }

  @Test
  @Throws(Exception::class)
  fun networkFirst() = runBlocking {
    enqueueResponse("/HttpCacheTestAllPlanets.json")

    val response = apolloClient.query(AllPlanetsQuery()).await()
    Truth.assertThat(response.hasErrors()).isFalse()

    Truth.assertThat(server.requestCount).isEqualTo(1)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_FIRST))
        .test()
        .assertValue { response -> !response.hasErrors() }
    Truth.assertThat(server.requestCount).isEqualTo(2)
    Truth.assertThat(lastHttResponse!!.networkResponse()).isNotNull()
    Truth.assertThat(lastHttResponse!!.cacheResponse()).isNull()
    checkCachedResponse("/HttpCacheTestAllPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun fromCacheFlag() {
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_FIRST))
        .test()
        .assertValue { response -> !response.hasErrors() && !response.isFromCache }
    enqueueResponse("/HttpCacheTestAllPlanets.json")
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY))
        .test()
        .assertValue { response -> !response.hasErrors() && !response.isFromCache }
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_ONLY))
        .test()
        .assertValue { response -> !response.hasErrors() && response.isFromCache }
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
        .test()
        .assertValue { response -> !response.hasErrors() && response.isFromCache }
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
        .test()
        .assertValue { response -> !response.hasErrors() && response.isFromCache }
    Rx2Apollo.from(apolloClient
        .query(AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.NETWORK_FIRST))
        .test()
        .assertValue { response -> !response.hasErrors() && response.isFromCache }
  }

  @Throws(IOException::class)
  private fun enqueueResponse(fileName: String) {
    server.enqueue(mockResponse(fileName))
  }

  @Throws(IOException::class)
  private fun checkCachedResponse(fileName: String) {
    val cacheKey = lastHttRequest!!.headers(HttpCache.CACHE_KEY_HEADER)[0]
    val response = apolloClient.cachedHttpResponse(cacheKey)
    Truth.assertThat(response).isNotNull()
    Truth.assertThat(response!!.body()!!.source().readUtf8()).isEqualTo(readFileToString(javaClass, fileName))
    response.body()!!.source().close()
  }

  @Throws(IOException::class)
  private fun checkNoCachedResponse() {
    val cacheKey = lastHttRequest!!.header(HttpCache.CACHE_KEY_HEADER)
    val cachedResponse = apolloClient.cachedHttpResponse(cacheKey)
    Truth.assertThat(cachedResponse).isNull()
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, fileName), 32)
  }

  private inner class TrackingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
      lastHttRequest = chain.request()
      lastHttResponse = chain.proceed(lastHttRequest)
      return lastHttResponse!!
    }
  }
}
