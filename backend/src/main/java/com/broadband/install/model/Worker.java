package com.broadband.install.model;

/**
 * 安装师傅。region 用于同片区优先派单；skillLevel 可用于容量微调（本实现以容量配置为准）。
 */
public class Worker {
    public String id;
    public String name;
    public String region;     // 负责片区，如 "南山"
    public int skillLevel = 1;

    public Worker() {}

    public Worker(String id, String name, String region) {
        this.id = id;
        this.name = name;
        this.region = region;
    }
}
