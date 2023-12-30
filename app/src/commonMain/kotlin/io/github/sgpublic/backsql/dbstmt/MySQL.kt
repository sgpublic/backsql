package io.github.sgpublic.backsql.dbstmt

import java.io.InputStream
import java.sql.Connection
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.util.StringJoiner

open class MySQL(connection: Connection): DatabaseConnection(connection) {
    protected open val SystemDatabase: Set<String> = setOf(
            "mysql",
            "information_schema",
            "performance_schema",
            "sys",
    )

    override fun version(): String {
        return executeQuery("select VERSION()") {
             if (!it.next()) {
                log.warn("无法查询数据库版本")
                "Unknown"
            } else {
                it.getString(1)
            }
        }
    }

    override fun showDatabases(): Set<Database> {
        val result = HashSet<String>()
        executeQuery("show databases;") {
            while (it.next()) {
                val databaseName = it.getString(1)
                if (SystemDatabase.contains(databaseName)) {
                    continue
                }
                result.add(databaseName)
            }
        }
        return result.map { database ->
            return@map executeQuery("""
                SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
                FROM information_schema.SCHEMATA
                WHERE SCHEMA_NAME = '${database}';
            """.trimIndent()) {
                if (!it.next()) {
                    throw IllegalStateException("未找到名为 $database 的数据库")
                }
                 Database(
                    name = database,
                    defaultCharsets = it.getString(1)
                )
            }
        }.toSet()
    }

    override fun showCreateDatabase(database: String): String {
        return executeQuery("show create database $database;") {
            if (!it.next()) {
                throw IllegalStateException("无法查询数据库 $database 创建语句")
            } else {
                it.getString(2)
            }
        }
    }

    override fun showTables(database: String): Set<String> {
        val result = HashSet<String>()
        executeQuery(database, "show tables;") {
            while (it.next()) {
                result.add(it.getString(1))
            }
        }
        return result
    }

    override fun showCreateTable(database: String, table: String): String {
        return executeQuery("show create database ${database};") {
            if (!it.next()) {
                throw IllegalStateException("无法查询数据表 ${database}.${table} 创建语句")
            } else {
                it.getString(2)
            }
        }
    }

    override fun showTableRecordCount(database: String, table: String): Long {
        return executeQuery(database, "select count(*) FROM ${table};") {
            if (!it.next()) {
                throw IllegalStateException("无法查询数据表 ${database}.${table} 中记录数量")
            } else {
                it.getLong(1)
            }
        }
    }
    @OptIn(ExperimentalStdlibApi::class)
    override fun showInsertTable(database: String, table: String, row: Long): String {
        return executeQuery(database, "SELECT * FROM $table LIMIT 1 OFFSET ${row};") {
            if (!it.next()) {
                throw IndexOutOfBoundsException("无法查询数据表 ${database}.${table} 中第 $row 条记录")
            }
            val result = StringJoiner(", ", "INSERT INTO `${table}` VALUES(", ");")
            for (column in 1 .. it.metaData.columnCount) {
                val data = it.getObject(column)
                if (column <= 1) {
                    log.debug("数据表 ${database}.${table} 中第 $row 条记录的第 $column 类型为 ${data::class}")
                }
                result.add(when (data) {
                    is Number -> data
                    is Boolean -> if (data) 1 else 0
                    is InputStream -> {
                        val byteStr = StringBuilder()
                        var byte: Int
                        while (data.read().also { byte = it } != -1) {
                            val str = byte.toHexString(HexFormat.UpperCase)
                            if (str.length == 1) {
                                byteStr.append('0')
                            }
                            byteStr.append(str)
                        }
                        "X'$byteStr'"
                    }
                    null -> {
                        when (it.metaData.getColumnType(column)) {
                            Types.DATE -> "'0000-00-00'"
                            Types.TIME -> "'00:00:00'"
                            Types.TIMESTAMP -> "'0000-00-00 00:00:00'"
                            else -> "NULL"
                        }
                    }
                    else -> "'${data}'"
                }.toString())
            }

            result.toString()
        }
    }
}