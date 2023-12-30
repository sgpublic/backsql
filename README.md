# BackSQL

这是一个用于备份数据库的工具，支持定时任务备份。

## 食用方法

### 命令行运行

命令行参数如下：

```shell
madray@pve:~/backsql$ ./bin/backsql --help
Usage: backsql [<options>]

Options:
  --tmp-dir=<path>           缓存目录，默认 /var/tmp/backsql
  --filename-pattern=<text>  归档文件命名规则，默认 backsql_%d{yyyy-MM-dd_HH-mm-ss}
  --save-dir=<path>          备份文件保存目录，默认 ./backsql
  --single-file              将备份保存为单个 SQL 文件
  --debug / -d               启用 DEBUG 模式
  --log-dir=<path>           日志保存目录，默认 /var/log/backsql
  --db-host=<text>           数据库 IP
  --db-port=<int>            数据库端口，默认：3306
  --db-user=<text>           数据库用户
  --db-pass=<text>           数据库密码
  --db-type=(mysql|mariadb)  数据库类型
  --duration=<int>           备份任务间隔时间，单位：秒
  --cron=<text>              备份任务 cron 表达式
  --keep-time=<int>          备份文件保留时长，-1 表示保留所有文件，单位：秒
  --keep-count=<int>         备份文件保留数量，-1 表示保留所有文件
  --now                      立即执行一次备份任务
  -h, --help                 Show this message and exit
```

若传入 `--duration` 则按照指定间隔时间执行备份，否则若传入 `--cron` 则按照 cron 表达式执行备份，否则立即执行一次备份后退出。

所有参数都可以用环境变量覆盖，例如 `--tmp-dir` 可由环境变量 `BACKSQL_TMP_DIR` 覆盖、`--db-port` 可由环境变量 `BACKSQL_DB_PORT` 覆盖。

### Docker

docker-compose.yaml 示例：

```yaml
version: "3"
services:
  mariadb:
    image: mariadb:10.7.8
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: passwd
    volumes:
      - ./mariadb:/var/lib/mysql
  backsql:
    image: mhmzx/backsql:1.0.0-alpha01
    restart: always
    depends_on:
      - mariadb
    environment:
      BACKSQL_DB_HOST: mariadb
      BACKSQL_DB_PORT: 3306
      BACKSQL_DB_USER: root
      BACKSQL_DB_PASS: passwd
      BACKSQL_DB_CRON: "0 0 */6 * * ?"
      BACKSQL_NOW: true
    volumes:
      - ./backsql:/app/backsql
      - /var/log/backsql:/var/log/backsql
      - /var/tmp/backsql:/var/tmp/backsql
```
