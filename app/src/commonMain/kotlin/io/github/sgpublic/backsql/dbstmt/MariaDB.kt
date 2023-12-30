package io.github.sgpublic.backsql.dbstmt

import java.sql.Connection

class MariaDB(connection: Connection): MySQL(connection) {
    override val SystemDatabase: Set<String> = setOf(
            "mysql",
            "information_schema",
            "performance_schema",
            "aria_log",
            "aria_log_control",
            "sys",
    )
}