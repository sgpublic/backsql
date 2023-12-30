package io.github.sgpublic.backsql.dbstmt

import io.github.sgpublic.backsql.core.DBType
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet

abstract class DatabaseConnection(
    private val connection: Connection,
): Connection by connection {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

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
    abstract fun showCreateDatabase(database: String): String
    abstract fun showTables(database: String): Set<String>
    abstract fun showCreateTable(database: String, table: String): String
    abstract fun showTableRecordCount(database: String, table: String): Long
    abstract fun showInsertTable(database: String, table: String, row: Long): String
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
