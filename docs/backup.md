# 数据库备份与恢复

这里备份的是整个 PostgreSQL。它和项目 JSON 导出不是同一件事。

- JSON 导出是某一个项目的资料，方便阅读和迁走这一份项目。
- 数据库备份是库里所有表的一份拷贝，包括全部项目、阶段、任务、工作日志、动态和附件登记。

附件文件本身不在数据库里，而在应用运行目录下的 `data/attachments`。备份数据库时，要把这个文件夹一起复制走。换一个目录启动应用后，如果还用相对路径，列表里能看见附件，下载时会找不到文件。恢复后请从原来的目录启动，或把 `devcompass.storage.local-dir` 设成这份文件夹的绝对路径。

下面的命令按当前 `docker-compose.yml` 编写。数据库容器名是 `devcompass-postgres`，库名、用户名和密码都是 `devcompass`。先在项目根目录启动数据库：

```powershell
docker compose up -d postgres
```

## 导出备份

在项目根目录执行。备份文件会出现在 `backup\devcompass-backup.sql`。

```powershell
New-Item -ItemType Directory -Force backup | Out-Null
docker exec devcompass-postgres pg_dump -U devcompass -d devcompass -f /tmp/devcompass-backup.sql
docker cp devcompass-postgres:/tmp/devcompass-backup.sql .\backup\devcompass-backup.sql
Copy-Item -Recurse -Force .\data\attachments .\backup\attachments
```

如果 `data\attachments` 还不存在，说明还没有上传过文件，跳过最后一行即可。

## 恢复到一个空库并抽查

这一步新建 `devcompass_restore`，不改你正在用的 `devcompass` 库。

```powershell
docker cp .\backup\devcompass-backup.sql devcompass-postgres:/tmp/devcompass-backup.sql
docker exec devcompass-postgres psql -U devcompass -d postgres -c "CREATE DATABASE devcompass_restore OWNER devcompass;"
docker exec devcompass-postgres psql -U devcompass -d devcompass_restore -f /tmp/devcompass-backup.sql
docker exec devcompass-postgres psql -U devcompass -d devcompass_restore -c "SELECT id, name FROM project ORDER BY id;"
```

最后一条命令应能看到原来的项目名称。然后用这个库启动应用，并让附件目录指向刚才复制的文件夹：

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/devcompass_restore"
$env:DEVCOMPASS_STORAGE_LOCAL_DIR = (Resolve-Path .\backup\attachments).Path
mvn spring-boot:run
```

打开项目列表，确认项目还在，再试着下载一个以前上传的附件。

演练结束后删掉这个临时库。先停掉上面启动的应用，再执行：

```powershell
docker exec devcompass-postgres psql -U devcompass -d postgres -c "DROP DATABASE devcompass_restore;"
```

## 换回正在使用的库

只有在确定要用备份替换当前库时才做。先停止 Java 程序，否则数据库可能因为还有连接而删不掉。

```powershell
docker exec devcompass-postgres psql -U devcompass -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'devcompass' AND pid <> pg_backend_pid();"
docker exec devcompass-postgres psql -U devcompass -d postgres -c "DROP DATABASE devcompass;"
docker exec devcompass-postgres psql -U devcompass -d postgres -c "CREATE DATABASE devcompass OWNER devcompass;"
docker cp .\backup\devcompass-backup.sql devcompass-postgres:/tmp/devcompass-backup.sql
docker exec devcompass-postgres psql -U devcompass -d devcompass -f /tmp/devcompass-backup.sql
```

把 `backup\attachments` 复制回应用会读取的附件目录，再按平常的方式启动应用。
