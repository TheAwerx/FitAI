package com.fitai.app.analytics

class LogAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) {
        println(if (params.isEmpty()) "[analytics] $event" else "[analytics] $event $params")
    }
}
