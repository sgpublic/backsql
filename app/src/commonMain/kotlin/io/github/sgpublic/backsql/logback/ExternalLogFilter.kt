package io.github.sgpublic.backsql.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.spi.FilterReply
import io.github.sgpublic.backsql.App

class ExternalLogFilter: LevelFilter() {
    override fun decide(event: ILoggingEvent?): FilterReply {
        return if (App.debug) {
            FilterReply.ACCEPT
        } else if (event!!.level.isGreaterOrEqual(Level.INFO)) {
            FilterReply.ACCEPT
        } else {
            FilterReply.NEUTRAL
        }
    }

    override fun setLevel(level: Level?) {
        throw UnsupportedOperationException("ExternalLogFilter#setLevel")
    }
}