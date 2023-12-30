package io.github.sgpublic.backsql.core.task

import io.github.sgpublic.backsql.core.*

class CronTask(
    dbConfig: DBConfig, appConfig: AppConfig, taskConfig: TaskConfig,
): BackupAction(dbConfig, appConfig), TaskConfig by taskConfig {
    constructor(config: Config): this(config, config, config)
}