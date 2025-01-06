package com.paylisher.internal

import com.google.gson.annotations.SerializedName
import com.paylisher.PaylisherEvent
import java.util.Date

/**
 * The batch data structure for calling the batch API
 * @property apiKey the Paylisher API Key
 * @property batch the events list
 * @property sentAt the timestamp of sending the event to calculate clock drifts
 */
internal data class PaylisherBatchEvent(
    @SerializedName("api_key")
    val apiKey: String,
    val batch: List<PaylisherEvent>,
    @SerializedName("sent_at")
    var sentAt: Date? = null,
)
