package com.novabar.app.utils

import kotlinx.coroutines.flow.MutableStateFlow

object LuminanceManager {
    // Flow representing whether the content behind the overlay is dark (requires white icons/text)
    val isDarkBackground = MutableStateFlow(true)
}
