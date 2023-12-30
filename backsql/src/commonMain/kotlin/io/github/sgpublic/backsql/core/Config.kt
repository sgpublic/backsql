package io.github.sgpublic.backsql.core

import org.quartz.CronExpression
import java.io.File

interface Config: DBConfig, TaskConfig, AppConfig, LogConfig

interface DBConfig {
    val dbHost: String
    val dbPort: Int
    val dbUser: String
    val dbPass: String
    val dbType: DBType
}

enum class DBType(
    driverName: String,
) {
    MySQL("com.mysql.cj.jdbc.Driver"),
    MariaDB("org.mariadb.jdbc.Driver")
    ;
    init {
        Class.forName(driverName)
    }
}

interface TaskConfig {
    val duration: Long?
    val cron: CronExpression?
    val keepTime: Long
    val keepCount: Int
    val now: Boolean
}

interface AppConfig {
    val tmpDir: File
    val saveDir: File
    val singleFile: Boolean
}

interface LogConfig {
    val debug: Boolean

    val logDir: File
}