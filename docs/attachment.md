# 项目附件

附件挂在项目上，用来保存设计图、说明和打包文件。文件内容放在本机目录，数据库只登记原文件名、类型、大小和存储编号。

接口说明见 [api.md](api.md) 的「项目附件」。

## 允许的文件

- 不能是空文件。
- 默认最大 10MB，配置项是 `devcompass.storage.max-size-bytes`。
- 文件名不能为空，也不能包含 `..`、斜杠或反斜杠。
- 只允许这些后缀：`png`、`jpg`、`jpeg`、`gif`、`webp`、`pdf`、`txt`、`md`、`zip`。类型只看后缀。

磁盘上的文件名是系统生成的编号，不用原文件名。两个同名文件不会互相覆盖。

## 保存位置

`devcompass.storage.type` 默认是 `local`。文件写到 `devcompass.storage.local-dir`，默认是应用运行目录下的 `data/attachments`。

上传、下载和删除都调用 `StorageService`。业务代码不自己拼磁盘路径。

## 以后换成 MinIO

配置里已经认识 `minio` 这个名字。现在写成 `minio` 时，应用启动会提示「MinIO 还没实现，请继续使用 local」，不会连接对象存储。

这一版不加入 MinIO 客户端，也不在 Docker Compose 里增加新容器。以后真要换成 MinIO，只改 `StorageService` 的保存、读取和删除方式。上传和下载接口可以保持不变。
