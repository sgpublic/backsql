package io.github.sgpublic.backsql.dbstmt

import io.github.sgpublic.backsql.core.DBType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

abstract class DatabaseConnection(
    private val connection: Connection,
): Connection by connection {
    protected val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    fun <T: Any> executeQuery(database: String, sql: String, block: (ResultSet) -> T): T {
        createStatement().use {
            execute("use ${database};")
            return it.executeQuery(sql).use(block)
        }
    }
    fun <T: Any> executeQuery(sql: String, block: (ResultSet) -> T): T {
        createStatement().use {
            return it.executeQuery(sql).use(block)
        }
    }
    fun execute(sql: String) {
        createStatement().use {
            it.execute(sql)
        }
    }

    abstract fun version(): String
    abstract fun showDatabases(): Set<Database>
    open fun preCreateDatabase(database: String, charset: String): String? = null
    abstract fun showCreateDatabase(database: String): String
    open fun postCreateDatabase(database: String, charset: String): String? = null
    abstract fun showTables(database: String): Set<String>
    open fun preCreateTable(database: String, table: String): String? = null
    abstract fun showCreateTable(database: String, table: String): String
    open fun postCreateTable(database: String, table: String): String? = null
    abstract fun showTableRecordCount(database: String, table: String): Long
    open fun preInsertTable(database: String, table: String, row: Long): String? = null
    abstract fun showInsertTable(database: String, table: String, row: Long): String
    open fun postInsertTable(database: String, table: String, row: Long): String? = null
    open fun postFinalDatabase(database: String, charset: String): String? = null

    open fun Any?.asValueString(rawType: Int): String {
        return when (this) {
            is Number -> numberAsValueString(this, rawType)
            is Boolean -> booleanAsValueString(this, rawType)
            is InputStream -> inputStreamAsValueString(this, rawType)
            is LocalTime -> Time.valueOf(this).asValueString(rawType)
            is LocalDate -> Date.valueOf(this).asValueString(rawType)
            is LocalDateTime -> Timestamp.valueOf(this).asValueString(rawType)
            null -> nullAsValueString(rawType)
            else -> anyAsValueString(this, rawType)
        }
    }

    open fun numberAsValueString(value: Number, rawType: Int): String = value.toString()
    open fun booleanAsValueString(value: Boolean, rawType: Int): String = if (value) "1" else "0"
    @OptIn(ExperimentalStdlibApi::class)
    open fun inputStreamAsValueString(value: InputStream, rawType: Int): String {
        val byteStr = StringBuilder()
        var byte: Int
        while (value.read().also { byte = it } != -1) {
            val str = byte.toHexString(HexFormat.UpperCase)
            if (str.length == 1) {
                byteStr.append('0')
            }
            byteStr.append(str)
        }
        return "X'$byteStr'"
    }
    open fun nullAsValueString(rawType: Int, defaultNull: String = "NULL"): String {
        return when (rawType) {
            Types.DATE -> "'0000-00-00'"
            Types.TIME -> "'00:00:00'"
            Types.TIMESTAMP -> "'0000-00-00 00:00:00'"
            else -> defaultNull
        }
    }
    open fun anyAsValueString(value: Any, rawType: Int): String = "'${value.toString().replace("'", "''")}'"
}

data class Database(
    val name: String,
    val defaultCharsets: String,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Database) return false
        return other.defaultCharsets == defaultCharsets
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }
}

fun Connection.wrapped(type: DBType): DatabaseConnection {
    return when (type) {
        DBType.MariaDB -> MariaDB(this)
        DBType.MySQL -> MySQL(this)
    }
}
