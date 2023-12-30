/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package io.github.sgpublic.backsql

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.*
import io.github.sgpublic.backsql.core.BackupAction
import io.github.sgpublic.backsql.core.Config
import io.github.sgpublic.backsql.core.DBType
import io.github.sgpublic.backsql.core.task.CronTask
import io.github.sgpublic.backsql.core.task.DurationTask
import org.quartz.CronExpression
import org.slf4j.LoggerFactory
import java.io.File

object App: CliktCommand(), Config {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override val tmpDir: File by option("--tmp-dir",
            help = "缓存目录",
            envvar = "BACKSQL_TMP_DIR")
            .file(
                mustExist = true,
                canBeFile = false,
                mustBeWritable = true,
                mustBeReadable = true,
            )
            .default(File("/var/tmp/backsql"))
    override val saveDir: File by option("--save-dir",
            help = "备份文件保存目录",
            envvar = "BACKSQL_SAVE_DIR")
            .file(
                mustExist = true,
                canBeFile = false,
                mustBeWritable = true,
                mustBeReadable = true,
            )
            .default(File("./backsql"))
    override val singleFile: Boolean by option("--single-file",
            help = "将备份保存为单个 SQL 文件",
            envvar = "BACKSQL_SINGLE_FILE")
            .boolean()
            .default(false)

    override val debug: Boolean by option("-d", "--debug",
            help = "启用 DEBUG 模式",
            envvar = "BACKSQL_DEBUG")
            .boolean()
            .default(false)
    override val logDir: File by option("--log-dir",
            help = "日志保存目录",
            envvar = "BACKSQL_LOG_DIR")
            .file(
                mustExist = true,
                canBeFile = false,
                mustBeWritable = true,
                mustBeReadable = true,
            )
            .default(File("/var/log/backsql"))

    override val dbHost: String by option("--db-host",
            help = "数据库 IP",
            envvar = "BACKSQL_DB_HOST")
            .required()
            .check("数据库地址不能为空") {
                it.isNotBlank()
            }
    override val dbPort: Int by option("--db-port",
            help = "数据库端口",
            envvar = "BACKSQL_DB_PORT")
            .int()
            .default(3306)
            .check("数据库端口不合法") {
                it in 0 .. 65535
            }
    override val dbUser: String by option("--db-user",
            help = "数据库用户",
            envvar = "BACKSQL_DB_USER")
            .required()
            .check("数据库用户名不能为空") {
                it.isNotBlank()
            }
    override val dbPass: String by option("--db-pass",
            help = "数据库密码",
            envvar = "BACKSQL_DB_PASS")
            .default("")
            .check("数据库用户名不能为空") {
                it.isNotBlank()
            }
    override val dbType: DBType by option("--db-type",
            help = "数据库类型",
            envvar = "BACKSQL_DB_DRIVER")
            .enum<DBType> {
                it.name.lowercase()
            }
            .default(DBType.MySQL)


    override val duration: Long? by option("--duration",
            help = "备份任务间隔时间，单位：秒",
            envvar = "BACKSQL_DURATION")
            .long()
    override val cron: CronExpression? by lazy {
        rawCron?.let { CronExpression(it) }
    }
    private val rawCron: String? by option("--cron",
            help = "备份任务 cron 表达式",
            envvar = "BACKSQL_CRON")
            .check("cron 表达式不合法") {
                try {
                    CronExpression(it)
                    return@check true
                } catch (e: Exception) {
                    return@check false
                }
            }

    override val keepTime: Long by option("--keep-time",
            help = "备份文件保留时长，单位：秒",
            envvar = "BACKSQL_KEEP_TIME")
            .long()
            .default(-1)
    override val keepCount: Int by option("--keep-count",
            help = "备份文件保留数量",
            envvar = "BACKSQL_KEEP_COUNT")
            .int()
            .default(-1)

    override val now: Boolean by option("--now",
            help = "立即执行一次备份任务")
            .boolean()
            .default(false)

    override fun run() {
        log.info("备份启动")
        val action = when {
            duration != null -> DurationTask(this)
            cron != null -> CronTask(this)
            else -> BackupAction(this)
        }
        if (!action.init()) {
            log.error("初始化失败，退出程序...")
            return
        }
        action.use {
            it.run()
        }
        log.info("备份结束")
    }
}

fun main(args: Array<String>) {
    App.main(args)
}
