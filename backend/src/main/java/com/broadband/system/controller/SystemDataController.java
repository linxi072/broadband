package com.broadband.system.controller;

import com.broadband.system.service.SystemDataService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.util.Map;

/**
 * 权限管理数据导入 / 导出（CSV）。
 *
 * <p>导出：UTF-8 BOM + CSV，浏览器可直接用 Excel 打开。
 * 导入：上传 CSV（multipart），批量创建；已存在的账号 / 角色标识自动跳过，返回汇总。</p>
 *
 * <p>本类只做权限校验、HTTP 响应头与流的写入，CSV 组装逻辑见 {@link SystemDataService}。</p>
 */
@RestController
@RequestMapping("/api/system")
public class SystemDataController {

    @Autowired private SystemDataService systemDataService;

    // ==================================================================== 用户

    @GetMapping("/users/export")
    @PreAuthorize("hasAuthority('system:user')")
    public void exportUsers(HttpServletResponse resp) throws Exception {
        writeCsv(resp, systemDataService.exportUsers());
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:user')")
    public Map<String, Object> importUsers(@RequestParam("file") MultipartFile file) throws Exception {
        return systemDataService.importUsers(file);
    }

    // ==================================================================== 角色

    @GetMapping("/roles/export")
    @PreAuthorize("hasAuthority('system:role')")
    public void exportRoles(HttpServletResponse resp) throws Exception {
        writeCsv(resp, systemDataService.exportRoles());
    }

    @PostMapping(value = "/roles/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:role')")
    public Map<String, Object> importRoles(@RequestParam("file") MultipartFile file) throws Exception {
        return systemDataService.importRoles(file);
    }

    // ==================================================================== 菜单

    @GetMapping("/menus/export")
    @PreAuthorize("hasAuthority('system:menu')")
    public void exportMenus(HttpServletResponse resp) throws Exception {
        writeCsv(resp, systemDataService.exportMenus());
    }

    // ==================================================================== 工具

    private void writeCsv(HttpServletResponse resp, SystemDataService.CsvDownload download) throws Exception {
        resp.setContentType("text/csv;charset=utf-8");
        String enc = URLEncoder.encode(download.filename(), "UTF-8").replace("+", "%20");
        resp.setHeader("Content-Disposition",
                "attachment; filename=\"" + download.filename() + "\"; filename*=UTF-8''" + enc);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(download.content());
        resp.getWriter().flush();
    }
}
