package com.paylisher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SignalService {
    private val _signalFlow = MutableSharedFlow<String>() // Signal source
    val signalFlow = _signalFlow.asSharedFlow() // Expose SharedFlow for subscribers

    // CoroutineScope for background work
    private val scope = CoroutineScope(Dispatchers.Default)

    // Emit signal asynchronously
    fun sendSignal(signal: String) {
        scope.launch {
            _signalFlow.emit(signal)
        }
    }
}

