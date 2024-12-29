# Endpoint: /error

java/com/paylisher/internal/PaylisherApi.kt

````
fun error(events: List<PaylisherEvent>) {
````

java/com/paylisher/internal/PaylisherQueue.kt

````kotlin
PaylisherApiEndpoint.ERROR -> api.error(events)
````

java/com/paylisher/internal/PaylisherSendCachedEventsIntegration.kt

````
PaylisherApiEndpoint.ERROR -> api.error(events)
````

java/com/paylisher/Paylisher.kt

````
val errorQueue = PaylisherQueue(config, api, PaylisherApiEndpoint.ERROR, config.errorStoragePrefix, errorExecutor)
````

## NOTE: /error endpoint yerine Paylisher.capture üzerinden gidilmekte :p

java/com/paylisher/internal/error/DefaultErrorHandler.kt

````kotlin
Paylisher.capture("SDK_Error", properties = properties)
Paylisher.capture("Error", properties = properties)
````
