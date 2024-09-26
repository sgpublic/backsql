package io.github.sgpublic.backsql.core

import io.github.sgpublic.backsql.App
import io.github.sgpublic.backsql.dbstmt.DatabaseConnection
import io.github.sgpublic.backsql.dbstmt.wrapped
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID
import org.codehaus.plexus.archiver.tar.TarGZipArchiver
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class BackupAction private constructor(
    dbConfig: DBConfig, appConfig: AppConfig, taskConfig: TaskConfig
): DBConfig by dbConfig, AppConfig by appConfig, TaskConfig by taskConfig, Job {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }
    constructor(): this(App, App, App)

    override fun execute(context: JobExecutionContext?) {
        if (running.get()) {
            log.info("任务运行中，跳过本次执行")
            return
        }
        synchronized(running) {
            if (running.get()) {
                return
            }
            try {
                running.set(true)
                log.info("备份任务开始")
                DriverManager.getConnection(
                        "jdbc:${dbType.name.lowercase()}://${dbHost}:${dbPort}/?zeroDateTimeBehavior=convertToNull",
                        dbUser, dbPass
                ).apply {
                    autoCommit = false
                }.use { connection ->
                    connection.beginRequest()
                    connection.wrapped(dbType).realRun()
                    connection.rollback()
                }
                log.info("备份任务结束")
            } catch (e: Exception) {
                throw IllegalStateException("数据库连接失败", e)
            } finally {
                running.set(false)
            }
        }
    }

    private fun DatabaseConnection.realRun() {
        val tmpDir = File(tmpDir, UUID.generateUUID().toString())
        try {
            val dbVersion = version()
            val originDate = Date()
            val date = SimpleDateFormat.getDateTimeInstance().format(originDate)
            log.info("目标数据库类型：$dbType")
            log.info("目标数据库版本：$dbVersion")
            log.info("备份时间：$date")

            var sqlFile: File? = null
            if (singleFile) {
                sqlFile = tmpDir.newChildFile("backsql.sql")
                sqlFile.appendOutputStream().bufferedWriter().use {
                    it.appendLine("""
                        /*
                         BackSQL Data Transfer
                         
                         Server Type: $dbType
                         Server Version: $dbVersion
                         
                         Date: $date
                        */
                        
                        """.trimIndent())
                }
            }
            for ((database, charset) in showDatabases()) {
                log.info("正在导出数据库 $database")
                if (!singleFile) {
                    sqlFile = tmpDir.newChildFile("${database}.sql")
                }
                sqlFile?.appendOutputStream()?.bufferedWriter()?.use {
                    if (!singleFile) {
                        it.appendLine("""
                            /*
                             BackSQL Data Transfer
                             
                             Server Type      : $dbType
                             Server Version   : $dbVersion
                             
                             Source Schema    : $database
                             
                             Date: $date
                            */
                            
                        """.trimIndent())
                    } else {
                        it.appendLine("""
                            -- ----------------------------
                            -- Database named '${database}'
                            -- ----------------------------
                        """.trimIndent())
                    }

                    preCreateDatabase(database, charset)?.let(it::appendLine)
                    it.appendLine(showCreateDatabase(database))
                    postCreateDatabase(database, charset)?.let(it::appendLine)
                    it.appendLine()

                    for (table in showTables(database)) {
                        log.info("正在导出数据表 $database.$table")

                        it.appendLine("""
                            -- ----------------------------
                            -- Table structure for ${"${database}.".takeIf { singleFile } ?: ""}$table
                            -- ----------------------------
                        """.trimIndent())
                        preCreateTable(database, table)?.let(it::appendLine)
                        it.appendLine(showCreateTable(database, table))
                        postCreateTable(database, table)?.let(it::appendLine)
                        it.appendLine()
                        it.appendLine("""
                            -- ----------------------------
                            -- Records of ${"${database}.".takeIf { singleFile } ?: ""}$table
                            -- ----------------------------
                        """.trimIndent())

                        val total = showTableRecordCount(database, table)
                        log.info("数据表 $database.$table 共 $total 条记录")
                        for (index in 0 until total) {
                            preInsertTable(database, table, index)?.let(it::appendLine)
                            it.appendLine(showInsertTable(database, table, index))
                            postInsertTable(database, table, index)?.let(it::appendLine)
                        }
                        it.appendLine()
                    }
                    postFinalDatabase(database, charset)?.let(it::appendLine)
                }
            }

            log.info("创建归档文件...")
            tmpDir.asZip(File(saveDir, "${BackFilenameEncoder.createFilename()}.tar.gz"))
        } catch (e: Exception) {
            log.error("备份出错", e)
        }
        try {
            log.info("清理临时文件...")
            tmpDir.deleteRecursively()

            if (keepCount < 0 && keepTime < 0) {
                return
            }
            val fileList = saveDir.listFiles()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return
            log.info("清理过期备份文件...")
            fileList.sortByDescending {
                it.lastModified()
            }
            fileList.forEachIndexed { index, file ->
                val fileExpired = file.lastModified() - System.currentTimeMillis()
                if ((keepCount in 1..index) || (keepTime >= 0 && fileExpired >= keepTime * 1000)) {
                    file.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            log.warn("清理执行失败", e)
        }
    }

    private fun File.asZip(output: File) {
        TarGZipArchiver().also { targz ->
            targz.destFile = output
            listFiles()?.forEach {
                targz.addFile(it, it.name)
            }
        }.createArchive()
    }

    private fun File.newChildFile(name: String): File {
        return File(this, name).also { newFile ->
            newFile.parentFile?.mkdirs()
            newFile.createNewFile()
        }
    }

    private fun File.appendOutputStream(append: Boolean = true): FileOutputStream {
        return FileOutputStream(this, append)
    }

    companion object {
        private val running = AtomicBoolean()
    }
}