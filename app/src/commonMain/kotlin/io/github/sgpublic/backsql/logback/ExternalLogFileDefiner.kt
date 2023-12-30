package io.github.sgpublic.backsql.logback

import ch.qos.logback.core.PropertyDefinerBase
import io.github.sgpublic.backsql.App


class ExternalLogFileDefiner: PropertyDefinerBase() {
    override fun getPropertyValue(): String {
        return App.logDir.canonicalPath
    }
}