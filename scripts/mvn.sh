#!/bin/sh
# Maven 构建包装脚本（指向一份干净的同版本 Maven 3.9.12）。
# 背景：环境原 Maven（/Users/mezo/Documents/localRepository/devRepository/maven）被两个版本
# 合并污染（maven-*/plexus/sisu/guice/resolver 等同时存在 3.6.3 与 3.9.12 两份 jar），
# 导致启动器与 Guice/Sisu 绑定双重失败。已下载一份干净的 3.9.12 到 /tmp 用于构建。
# 若 /tmp 被清理，请重新下载：
#   curl -fsSL -o /tmp/apache-maven-3.9.12-bin.tar.gz \
#     https://archive.apache.org/dist/maven/maven-3/3.9.12/binaries/apache-maven-3.9.12-bin.tar.gz
#   tar -xzf /tmp/apache-maven-3.9.12-bin.tar.gz -C /tmp
exec /tmp/apache-maven-3.9.12/bin/mvn "$@"
