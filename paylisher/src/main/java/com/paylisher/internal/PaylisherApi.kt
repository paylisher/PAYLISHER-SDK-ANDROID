package com.paylisher.internal

import com.paylisher.PaylisherConfig
import com.paylisher.PaylisherEvent
import com.paylisher.internal.error.DefaultErrorHandler
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.io.OutputStream

/**
 * The class that calls the Paylisher API
 * @property config the Config
 */
internal class PaylisherApi(
    private val config: PaylisherConfig,
    private val errorHandler: DefaultErrorHandler,
) {
    private val mediaType by lazy {
        try {
            // can throw IllegalArgumentException
            "application/json; charset=utf-8".toMediaType()
        } catch (ignored: Throwable) {
            null
        }
    }

    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(GzipRequestInterceptor(config))
            .build()

    private val theHost: String
        get() {
            return if (config.host.endsWith("/")) config.host.substring(
                0,
                config.host.length - 1
            ) else config.host
        }

    @Throws(PaylisherApiError::class, IOException::class)
    fun batch(events: List<PaylisherEvent>) {
        val batch = PaylisherBatchEvent(config.apiKey, events)

        val request =
            makeRequest("$theHost/batch") {
                batch.sentAt = config.dateProvider.currentDate()
                config.serializer.serialize(batch, it.bufferedWriter())
            }

        client.newCall(request).execute().use {
            if (!it.isSuccessful) {
                errorHandler.handleError((PaylisherApiError(it.code, it.message, it.body)), "batch")
                throw PaylisherApiError(it.code, it.message, it.body)
            }
        }
    }

    @Throws(PaylisherApiError::class, IOException::class)
    fun error(events: List<PaylisherEvent>) {
        val error = PaylisherBatchEvent(config.apiKey, events)

        val request =
            makeRequest("$theHost/error") {
                error.sentAt = config.dateProvider.currentDate()
                config.serializer.serialize(error, it.bufferedWriter())
            }

        client.newCall(request).execute().use {
            if (!it.isSuccessful) {
                errorHandler.handleError((PaylisherApiError(it.code, it.message, it.body)), "error")
                throw PaylisherApiError(it.code, it.message, it.body)
            }
        }
    }

    @Throws(PaylisherApiError::class, IOException::class)
    fun snapshot(events: List<PaylisherEvent>) {
        events.forEach {
            it.apiKey = config.apiKey
        }

//        // for easy debugging
//        config.serializer.serializeObject(events)?.let {
//            print("rrweb events: $it")
//        }

        // sent_at isn't supported by the snapshot endpoint
        val request =
            makeRequest("$theHost${config.snapshotEndpoint}") {
                config.serializer.serialize(events, it.bufferedWriter())
            }

        client.newCall(request).execute().use {
            if (!it.isSuccessful) {
                errorHandler.handleError(PaylisherApiError(it.code, it.message, it.body), "snapshot")
                throw PaylisherApiError(it.code, it.message, it.body)
            }
        }
    }

    private fun makeRequest(
        url: String,
        serializer: (outputStream: OutputStream) -> Unit,
    ): Request {
        val requestBody =
            object : RequestBody() {
                override fun contentType() = mediaType

                override fun writeTo(sink: BufferedSink) {
                    sink.outputStream().use {
                        serializer(it)
                    }
                }
            }

        return Request.Builder()
            .url(url)
            .header("User-Agent", config.userAgent)
            .post(requestBody)
            .build()
    }

    @Throws(PaylisherApiError::class, IOException::class)
    fun decide(
        distinctId: String,
        anonymousId: String?,
        groups: Map<String, String>?,
    ): PaylisherDecideResponse? {
        val decideRequest =
            PaylisherDecideRequest(config.apiKey, distinctId, anonymousId = anonymousId, groups)

        val request =
            makeRequest("$theHost/decide/?v=3") {
                config.serializer.serialize(decideRequest, it.bufferedWriter())
            }

        client.newCall(request).execute().use {
            if (!it.isSuccessful) throw PaylisherApiError(it.code, it.message, it.body)

            it.body?.let { body ->
                return config.serializer.deserialize(body.charStream().buffered())
            }
            return null
        }
    }
}
