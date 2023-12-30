package io.github.sgpublic.backsql.core

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.pattern.DateConverter
import ch.qos.logback.classic.spi.LoggingEvent
import io.github.sgpublic.backsql.App

object BackFilenameEncoder: PatternLayout() {
    private val defaultMap = mutableMapOf(
        "d" to DateConverter::class.qualifiedName!!,
        "date" to DateConverter::class.qualifiedName!!,
    )

    override fun getDefaultConverterMap(): MutableMap<String, String> {
        return defaultMap
    }

    fun createFilename(): String {
        if (getContext() == null) {
            setContext(LoggerContext())
        }
        if (pattern == null) {
            pattern = App.filenamePattern
        }
        if (!isStarted) {
            start()
        }
        return doLayout(BackupFile())
    }
}

data class BackupFile(
    private val time: Long = System.currentTimeMillis()
): LoggingEvent() {
    override fun getTimeStamp(): Long {
        return time
    }
}
