package io.github.sgpublic.backsql.core

import io.github.sgpublic.backsql.dbstmt.DatabaseConnection
import io.github.sgpublic.backsql.dbstmt.wrapped
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.*

open class BackupAction(
    dbConfig: DBConfig, appConfig: AppConfig,
): DBConfig by dbConfig, AppConfig by appConfig, AutoCloseable {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }
    constructor(config: Config): this(config, config)

    private var connection: Connection? = null
    fun init(): Boolean {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:${dbType.name.lowercase()}://${dbHost}:${dbPort}/?zeroDateTimeBehavior=convertToNull",
                    dbUser, dbPass
            ).apply {
                autoCommit = false
            }
            return true
        } catch (e: Exception) {
            log.error("数据库连接失败", e)
            return false
        }
    }

    open fun run() {
        connection?.let { connection ->
            connection.beginRequest()
            connection.wrapped(dbType).realRun()
            connection.rollback()
        }
    }

    private fun DatabaseConnection.realRun() {
        val tmpDir = File(tmpDir, UUID.generateUUID().toString())
        try {
            val dbVersion = version()
            val date = SimpleDateFormat.getDateTimeInstance().format(Date())
            log.info("目标数据库类型：$dbType")
            log.info("目标数据库版本：$dbVersion")
            log.info("备份时间：$date")

            var sqlFile: File? = null
            if (singleFile) {
                sqlFile = File(tmpDir, "backsql.sql")
                sqlFile.outputStream().bufferedWriter().use {
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
                    sqlFile = File(tmpDir, "${database}.sql")
                }
                sqlFile?.parentFile?.mkdirs()
                sqlFile?.createNewFile()
                sqlFile?.outputStream()?.bufferedWriter()?.use {
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
                            USE DATABASE $database;
                            
                            """.trimIndent())
                    }
                    it.appendLine("""
                        SET NAMES $charset;
                        SET FOREIGN_KEY_CHECKS 0;
                        
                        DROP DATABASE IF EXISTS '${database}';
                        ${showCreateDatabase(database)}
                        
                        """.trimIndent())

                    for (table in showTables(database)) {
                        log.info("正在导出数据表 $database.$table")

                        it.appendLine("""
                            -- ----------------------------
                            -- Table structure for $table
                            -- ----------------------------
                            DROP TABLE IF EXISTS `$table`;
                            ${showCreateTable(database, table)}
                            
                            -- ----------------------------
                            -- Records of main_setting_items
                            -- ----------------------------
                            """.trimIndent())

                        val total = showTableRecordCount(database, table)
                        log.info("数据表 $database.$table 共 $total 条记录")
                        for (index in 0 until total) {
                            log.debug("正在导出数据表 ${database}.${table} 中第 $index 条记录")
                            it.appendLine(showInsertTable(database, table, index))
                        }
                    }

                    it.appendLine("""
                        SET FOREIGN_KEY_CHECKS 1;
                        """.trimIndent())
                }
            }
        } catch (e: Exception) {
            log.error("备份出错", e)
        } finally {
            log.info("清理临时文件...")
//            tmpDir.deleteRecursively()
        }
    }

    override fun close() {
        connection?.close()
    }
}