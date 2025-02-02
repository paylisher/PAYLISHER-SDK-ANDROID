package com.paylisher.internal

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.paylisher.PaylisherConfig
import com.paylisher.PaylisherInternal
import com.paylisher.internal.replay.RREventType
import com.paylisher.internal.replay.RRIncrementalSource
import com.paylisher.internal.replay.RRMouseInteraction
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.util.Date

/**
 * The JSON serializer using Gson
 * @property config the Config
 */
@PaylisherInternal
public class PaylisherSerializer(private val config: PaylisherConfig) {
    public val gson: Gson =
        GsonBuilder().apply {
            setObjectToNumberStrategy(GsonNumberPolicy())
            registerTypeAdapter(Date::class.java, GsonDateTypeAdapter(config))
                .setLenient()
            registerTypeAdapter(RREventType::class.java, GsonRREventTypeSerializer(config))
            registerTypeAdapter(RRIncrementalSource::class.java, GsonRRIncrementalSourceSerializer(config))
            registerTypeAdapter(RRMouseInteraction::class.java, GsonRRMouseInteractionsSerializer(config))
        }.create()

    @Throws(JsonIOException::class, IOException::class)
    public inline fun <reified T> serialize(
        value: T,
        writer: Writer,
    ) {
        gson.toJson(value, object : TypeToken<T>() {}.type, writer)
        writer.flush()
    }

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    public inline fun <reified T> deserialize(reader: Reader): T {
        return gson.fromJson(reader, object : TypeToken<T>() {}.type)
    }

    public fun deserializeString(json: String): Any? {
        return gson.fromJson(json, Any::class.java)
    }

    public fun serializeObject(value: Any): String? {
        return gson.toJson(value, Any::class.java)
    }
}
