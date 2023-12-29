package io.github.sgpublic.backsql

interface Config {
    val dbHost: String
    val dbPort: Int
    val duration: Long?
    val cron: String?
    val keepTime: Long
    val keepCount: Int
    val now: Boolean
    val once: Boolean
}