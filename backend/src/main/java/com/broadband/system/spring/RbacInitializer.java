package com.broadband.system.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.SysUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RBAC 数据初始化。
 *
 * <p>data.sql 只插入用户骨架（password 留空），初始密码在这里用 BCrypt 写入，
 * 好处：① SQL 中不出现哈希；② 幂等 —— 已有密码（含用户自行改过的）不会被覆盖。</p>
 *
 * <p>默认初始密码：admin/admin123、liuwei/liuwei123、zhaomin/zhaomin123、wangfang/wangfang123；
 * 其余用户回落到「账号 + 123」。生产环境请务必首次登录后立即修改。</p>
 */
@Component
public class RbacInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RbacInitializer.class);

    private static final Map<String, String> DEFAULT_PASSWORDS = new LinkedHashMap<>();

    static {
        DEFAULT_PASSWORDS.put("admin", "admin123");
        DEFAULT_PASSWORDS.put("liuwei", "liuwei123");
        DEFAULT_PASSWORDS.put("zhaomin", "zhaomin123");
        DEFAULT_PASSWORDS.put("wangfang", "wangfang123");
    }

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<SysUser> users = userMapper.selectList(new QueryWrapper<>());
        int filled = 0;
        for (SysUser u : users) {
            if (u.password == null || u.password.isBlank()) {
                String plain = DEFAULT_PASSWORDS.getOrDefault(u.username, u.username + "123");
                // 仅更新 password 列，避免 updateById 用部分实体把 must_change_password 等列重置为默认值
                // （UPDATE sys_user SET password=?, must_change_password=0, ...），从而抹掉 T-02 首登改密门禁的种子标记。
                userMapper.update(null, new UpdateWrapper<SysUser>()
                        .eq("id", u.id)
                        .set("password", passwordEncoder.encode(plain)));
                filled++;
            }
        }
        if (filled > 0) {
            log.info("RBAC 初始化：为 {} 个账号写入初始密码（BCrypt）。账号 {}",
                    filled, DEFAULT_PASSWORDS.keySet());
        } else {
            log.info("RBAC 初始化：账号密码已就绪，跳过。");
        }
    }
}
