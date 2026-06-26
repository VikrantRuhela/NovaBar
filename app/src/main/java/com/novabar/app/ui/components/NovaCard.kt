package com.novabar.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import com.novabar.app.ui.theme.DesignTokens
import androidx.compose.ui.Modifier

@Composable
fun NovaCard(
    content: @Composable () -> Unit
) {
    Card(
        shape = DesignTokens.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.elevationCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(DesignTokens.spacingMd)
    ) {
        Column(modifier = Modifier.padding(DesignTokens.spacingMd)) {
            content()
        }
    }
}
