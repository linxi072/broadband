package com.broadband.install.model;

/**
 * 小区（安装覆盖点）。
 * 相邻性判定依赖 region / street / latitude / longitude：
 *  - 同街道直接视为相邻；
 *  - 经纬度距离 <= 阈值（默认 800m）视为相邻。
 */
public class Community {
    public String id;
    public String name;
    public String region;      // 所属片区，如 "南山"
    public String street;      // 所属街道，如 "科技园路"
    public String carrier;     // 覆盖运营商（PC 后台展示用，不参与派单算法）
    public double latitude;
    public double longitude;
    public boolean installable = true;
    public int portTotal = 0;
    public int portUsed = 0;

    public Community() {}

    public Community(String id, String name, String region, String street, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.street = street;
        this.latitude = latitude;
        this.longitude = longitude;
        this.installable = true;
    }
}
