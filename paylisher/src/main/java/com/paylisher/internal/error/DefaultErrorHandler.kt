package com.paylisher.internal.error

import com.paylisher.Paylisher
import com.paylisher.PaylisherConfig
//import com.paylisher.PaylisherEvent
//import com.paylisher.internal.PaylisherApi
import com.paylisher.internal.PaylisherApiError
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

internal class DefaultErrorHandler(
    private val config: PaylisherConfig,
//    private val api: PaylisherApi,
) : ErrorHandler {

    override fun handleError(throwable: Throwable, thread: Thread) {
        // Capture stack trace as a string
        val writer: Writer = StringWriter()
        val pWriter = PrintWriter(writer)
        throwable.printStackTrace(pWriter)
        val stackTrace = writer.toString()

        // Gather exception details
        val exceptionType: String = throwable.javaClass.name // Use 'name' instead of 'getName'
        val message: String = throwable.message ?: "No message available" // Provide default message
        val threadName: String = thread.name // Use 'name' instead of 'getName'

        // Construct data string with relevant information
        val data = buildString {
            append("# Type of exception: $exceptionType\n")
            append("# Exception message: $message\n")
            append("# Thread name: $threadName\n")
            append("# Stacktrace: $stackTrace")
        }

        // Limit the data length to 8192 characters
        val truncatedData = if (data.length > 8192) data.substring(0, 8192) else data

        // Create properties map with the error details, ensuring value type is Any
        val properties: Map<String, Any> = mapOf(
            "exceptionType" to exceptionType,
            "message" to message,
            "threadName" to threadName,
            "stackTrace" to truncatedData
        )

//        // Create and send the error event
//        val errorEvent = PaylisherEvent(
//            event = "Error Occurred",
//            properties = properties // Pass the properties map directly
//        )
//        sendErrorToApi(listOf(errorEvent))

        Paylisher.capture("Error", properties = properties)
    }

    override fun handleError(error: PaylisherApiError, context: String) {
        // Safely map error details, handling null values
        val properties: Map<String, Any> = mapOf(
            "context" to context,
            "message" to error.message,
            "statusCode" to error.statusCode,
            "stackTrace" to error.stackTraceToString() // Capture the full stack trace
        )

        // Capture the error as an event in Paylisher with relevant properties
        Paylisher.capture("SDK_Error", properties = properties)
    }


    override fun handleError(exception: Exception, context: String) {
        val statusCode = getStatusCode(exception)
        val message = exception.message ?: "Unknown error occurred"
        val stackTrace = exception.stackTraceToString() // Get the stack trace as a string
        val exceptionType: String = exception.javaClass.name // Use 'name' instead of 'getName'
        val apiError = PaylisherApiError(statusCode, message, null)

        // Log the error with additional context
        config.logger.log("Error in context '$context': ${apiError.message} (Status Code: $statusCode)")

        // Construct data string with relevant information
        val data = buildString {
            append("# Type of exception: $exceptionType\n")
            append("# Exception message: $message\n")
            append("# Status Code: $statusCode\n")
            append("# Stacktrace: $stackTrace")
        }

        // Limit the data length to 8192 characters
        val truncatedData = if (data.length > 8192) data.substring(0, 8192) else data

        // Create properties map with the error details, ensuring value type is Any
        val properties: Map<String, Any> = mapOf(
            "exceptionType" to exceptionType,
            "message" to message,
            "statusCode" to statusCode,
            "stackTrace" to truncatedData
        )

        Paylisher.capture("SDK_Error", properties = properties)
    }

    private fun getStatusCode(e: Exception): Int {
        return when (e) {
            is PaylisherApiError -> e.statusCode
            else -> 500 // Generic server error
        }
    }

//    private fun sendErrorToApi(events: List<PaylisherEvent>) {
//        // Implementation for sending error events to your API
//        // For example, you could use the API object to log the error
//        try {
//            api.error(events)  // Assuming api.error is the method to send errors
//        } catch (e: Exception) {
//            // Log any errors that occur while trying to send the error report
//            config.logger.log("Failed to send error report: ${e.message}")
//        }
//    }
}
