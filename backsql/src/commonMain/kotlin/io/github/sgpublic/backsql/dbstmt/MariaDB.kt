package io.github.sgpublic.backsql.dbstmt

import java.sql.Connection

class MariaDB(connection: Connection): MySQL(connection) {
    override val excludeDatabase: Set<String> = setOf(
        "mysql",
        "information_schema",
        "performance_schema",
        "sys",
    )
}