package com.novabar.app.domain

import com.novabar.app.R

data class DeviceContext(
    val hourOfDay: Int,
    val isCharging: Boolean,
    val batteryPercentage: Int,
    val isMusicPlaying: Boolean
)

interface ContextCondition {
    fun matches(context: DeviceContext): Boolean
}

data class ContextCategory(
    val name: String,
    val condition: ContextCondition
)

object ContextCategoryRegistry {
    private val categories = mutableListOf<ContextCategory>()

    init {
        // Battery Low Condition (Battery percentage <= 20% and not currently charging)
        categories.add(ContextCategory("BATTERY_LOW", object : ContextCondition {
            override fun matches(context: DeviceContext): Boolean {
                return context.batteryPercentage <= 20 && !context.isCharging
            }
        }))

        // Charging Condition
        categories.add(ContextCategory("CHARGING", object : ContextCondition {
            override fun matches(context: DeviceContext): Boolean {
                return context.isCharging
            }
        }))

        // Music Playing Condition
        categories.add(ContextCategory("MUSIC_PLAYING", object : ContextCondition {
            override fun matches(context: DeviceContext): Boolean {
                return context.isMusicPlaying
            }
        }))

        // Morning Condition (5:00 AM - 11:59 AM)
        categories.add(ContextCategory("MORNING", object : ContextCondition {
            override fun matches(context: DeviceContext): Boolean {
                return context.hourOfDay in 5..11
            }
        }))

        // Afternoon Condition (12:00 PM - 4:59 PM)
        categories.add(ContextCategory("AFTERNOON", object : ContextCondition {
            override fun matches(context: DeviceContext): Boolean {
                return context.hourOfDay in 12..16
            }
        }))

        // Evening Condition (5:00 PM - 8:59 PM)
        categories.add(ContextCategory("EVENING", object : ContextCondition {
            override fun matches(context: DeviceContext): Boolean {
                return context.hourOfDay in 17..20
            }
        }))

        // Late Night Condition (9:00 PM - 4:59 AM)
        categories.add(ContextCategory("LATE_NIGHT", object : ContextCondition {
            override fun matches(context: DeviceContext): Boolean {
                return context.hourOfDay in 21..23 || context.hourOfDay in 0..4
            }
        }))
    }

    fun getActiveCategories(context: DeviceContext): List<String> {
        return categories.filter { it.condition.matches(context) }.map { it.name }
    }
}

data class NovaGuyMessage(
    val textResId: Int,
    val contextCategory: String? = null,
    val subCategory: String? = null
)

object NovaGuyMessageProvider {
    val messages = listOf(
        // General Pool - Greetings
        NovaGuyMessage(R.string.novaguy_msg_greet_1, subCategory = "GREETINGS"),
        NovaGuyMessage(R.string.novaguy_msg_greet_2, subCategory = "GREETINGS"),
        NovaGuyMessage(R.string.novaguy_msg_greet_3, subCategory = "GREETINGS"),
        NovaGuyMessage(R.string.novaguy_msg_greet_4, subCategory = "GREETINGS"),

        // General Pool - Friendly
        NovaGuyMessage(R.string.novaguy_msg_friend_1, subCategory = "FRIENDLY"),
        NovaGuyMessage(R.string.novaguy_msg_friend_2, subCategory = "FRIENDLY"),
        NovaGuyMessage(R.string.novaguy_msg_friend_3, subCategory = "FRIENDLY"),
        NovaGuyMessage(R.string.novaguy_msg_friend_4, subCategory = "FRIENDLY"),

        // General Pool - Motivation
        NovaGuyMessage(R.string.novaguy_msg_motiv_1, subCategory = "MOTIVATION"),
        NovaGuyMessage(R.string.novaguy_msg_motiv_2, subCategory = "MOTIVATION"),
        NovaGuyMessage(R.string.novaguy_msg_motiv_3, subCategory = "MOTIVATION"),
        NovaGuyMessage(R.string.novaguy_msg_motiv_4, subCategory = "MOTIVATION"),

        // General Pool - Wellness
        NovaGuyMessage(R.string.novaguy_msg_well_1, subCategory = "WELLNESS"),
        NovaGuyMessage(R.string.novaguy_msg_well_2, subCategory = "WELLNESS"),
        NovaGuyMessage(R.string.novaguy_msg_well_3, subCategory = "WELLNESS"),
        NovaGuyMessage(R.string.novaguy_msg_well_4, subCategory = "WELLNESS"),

        // General Pool - Fun
        NovaGuyMessage(R.string.novaguy_msg_fun_1, subCategory = "FUN"),
        NovaGuyMessage(R.string.novaguy_msg_fun_2, subCategory = "FUN"),
        NovaGuyMessage(R.string.novaguy_msg_fun_3, subCategory = "FUN"),
        NovaGuyMessage(R.string.novaguy_msg_fun_4, subCategory = "FUN"),

        // Context Specific - Morning
        NovaGuyMessage(R.string.novaguy_msg_morning_1, contextCategory = "MORNING"),
        NovaGuyMessage(R.string.novaguy_msg_morning_2, contextCategory = "MORNING"),
        NovaGuyMessage(R.string.novaguy_msg_morning_3, contextCategory = "MORNING"),

        // Context Specific - Afternoon
        NovaGuyMessage(R.string.novaguy_msg_afternoon_1, contextCategory = "AFTERNOON"),
        NovaGuyMessage(R.string.novaguy_msg_afternoon_2, contextCategory = "AFTERNOON"),

        // Context Specific - Evening
        NovaGuyMessage(R.string.novaguy_msg_evening_1, contextCategory = "EVENING"),
        NovaGuyMessage(R.string.novaguy_msg_evening_2, contextCategory = "EVENING"),

        // Context Specific - Late Night
        NovaGuyMessage(R.string.novaguy_msg_latenight_1, contextCategory = "LATE_NIGHT"),
        NovaGuyMessage(R.string.novaguy_msg_latenight_2, contextCategory = "LATE_NIGHT"),

        // Context Specific - Charging
        NovaGuyMessage(R.string.novaguy_msg_charging_1, contextCategory = "CHARGING"),
        NovaGuyMessage(R.string.novaguy_msg_charging_2, contextCategory = "CHARGING"),

        // Context Specific - Battery Low
        NovaGuyMessage(R.string.novaguy_msg_batterylow_1, contextCategory = "BATTERY_LOW"),
        NovaGuyMessage(R.string.novaguy_msg_batterylow_2, contextCategory = "BATTERY_LOW"),

        // Context Specific - Music Playing
        NovaGuyMessage(R.string.novaguy_msg_music_1, contextCategory = "MUSIC_PLAYING"),
        NovaGuyMessage(R.string.novaguy_msg_music_2, contextCategory = "MUSIC_PLAYING")
    )

    private var lastMessage: NovaGuyMessage? = null

    /**
     * Selects a random message based on active device contexts.
     */
    fun selectRandomMessage(deviceContext: DeviceContext, contextAwareEnabled: Boolean): NovaGuyMessage {
        val pool = if (contextAwareEnabled) {
            val activeCategories = ContextCategoryRegistry.getActiveCategories(deviceContext)
            val contextPool = messages.filter { it.contextCategory in activeCategories }
            if (contextPool.isNotEmpty()) {
                contextPool
            } else {
                messages.filter { it.contextCategory == null }
            }
        } else {
            messages.filter { it.contextCategory == null }
        }

        if (pool.isEmpty()) {
            return messages.random()
        }

        val lastMsg = lastMessage
        val filtered = pool.filter { msg ->
            msg != lastMsg &&
            (lastMsg == null ||
             (msg.contextCategory != lastMsg.contextCategory || msg.contextCategory == null) &&
             (msg.subCategory != lastMsg.subCategory || msg.subCategory == null))
        }

        val selected = if (filtered.isNotEmpty()) {
            filtered.random()
        } else {
            val fallbackFiltered = pool.filter { it != lastMsg }
            if (fallbackFiltered.isNotEmpty()) {
                fallbackFiltered.random()
            } else {
                pool.random()
            }
        }

        lastMessage = selected
        return selected
    }
}
