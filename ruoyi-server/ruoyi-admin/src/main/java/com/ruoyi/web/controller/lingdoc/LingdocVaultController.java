package com.ruoyi.web.controller.lingdoc;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocFileVersion;
import com.ruoyi.system.domain.lingdoc.LingdocUserRepo;
import com.ruoyi.system.service.lingdoc.ILingdocUserRepoService;
import com.ruoyi.system.service.lingdoc.ILingdocVaultService;

/**
 * Vault文件浏览器 Controller
 * 
 * @author lingdoc
 */
@RestController
@RequestMapping("/lingdoc/vault")
public class LingdocVaultController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(LingdocVaultController.class);

    @Autowired
    private ILingdocVaultService vaultService;

    @Autowired
    private ILingdocUserRepoService userRepoService;

    /**
     * 获取当前用户仓库配置
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:list')")
    @GetMapping("/repo")
    public AjaxResult getRepo()
    {
        LingdocUserRepo repo = userRepoService.getOrInitUserRepo(getUserId());
        return AjaxResult.success(repo);
    }

    /**
     * 创建/切换到新仓库
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @Log(title = "仓库管理", businessType = BusinessType.UPDATE)
    @PostMapping("/repo")
    public AjaxResult createRepo(@RequestBody Map<String, String> body)
    {
        String repoPath = body.get("repoPath");
        String repoName = body.get("repoName");
        if (StringUtils.isEmpty(repoPath))
        {
            return AjaxResult.error("仓库路径不能为空");
        }
        LingdocUserRepo repo = userRepoService.createRepo(getUserId(), repoPath, repoName);
        return AjaxResult.success(repo);
    }

    /**
     * 迁移仓库到新路径
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @Log(title = "仓库管理", businessType = BusinessType.UPDATE)
    @PutMapping("/repo/migrate")
    public AjaxResult migrateRepo(@RequestBody Map<String, String> body)
    {
        String newRepoPath = body.get("newRepoPath");
        if (StringUtils.isEmpty(newRepoPath))
        {
            return AjaxResult.error("新仓库路径不能为空");
        }
        try
        {
            userRepoService.migrateRepo(getUserId(), newRepoPath);
            return AjaxResult.success("仓库迁移成功");
        }
        catch (IOException e)
        {
            log.error("仓库迁移失败", e);
            return AjaxResult.error("仓库迁移失败: " + e.getMessage());
        }
        catch (RuntimeException e)
        {
            log.error("仓库迁移失败", e);
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 上传文件到Vault
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @Log(title = "Vault文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "subPath", required = false) String subPath)
    {
        try
        {
            LingdocFileIndex result = vaultService.uploadFile(file, subPath, getUserId());
            return AjaxResult.success(result);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        catch (IOException e)
        {
            log.error("上传文件失败", e);
            return AjaxResult.error("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取目录树
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:list')")
    @GetMapping("/tree")
    public AjaxResult tree()
    {
        List<Map<String, Object>> tree = vaultService.getVaultTree(getUserId());
        return AjaxResult.success(tree);
    }

    /**
     * 分页查询文件列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:list')")
    @GetMapping("/files")
    public TableDataInfo files(LingdocFileIndex query)
    {
        query.setUserId(getUserId());
        startPage();
        List<LingdocFileIndex> list = vaultService.selectLingdocFileIndexList(query);
        return getDataTable(list);
    }

    /**
     * 获取单个文件详情
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:list')")
    @GetMapping("/file/{fileId}")
    public AjaxResult getFile(@PathVariable("fileId") String fileId)
    {
        LingdocFileIndex file = vaultService.selectLingdocFileIndexById(fileId);
        if (file == null || !getUserId().equals(file.getUserId()))
        {
            return AjaxResult.error("文件不存在或无权限");
        }
        // 安全处理：不返回完整 file_content
        if (StringUtils.isNotEmpty(file.getFileContent()) && file.getFileContent().length() > 500)
        {
            file.setFileContent(file.getFileContent().substring(0, 500) + "...");
        }
        return AjaxResult.success(file);
    }

    /**
     * 获取文本文件内容
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:list')")
    @GetMapping("/file/{fileId}/content")
    public AjaxResult getFileContent(@PathVariable("fileId") String fileId)
    {
        try
        {
            return vaultService.getFileContent(fileId, getUserId());
        }
        catch (IOException e)
        {
            log.error("读取文件内容失败", e);
            return AjaxResult.error("读取文件内容失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:download')")
    @GetMapping("/file/{fileId}/download")
    public void download(@PathVariable("fileId") String fileId, HttpServletResponse response)
    {
        try
        {
            LingdocFileIndex file = vaultService.selectLingdocFileIndexById(fileId);
            if (file == null || !getUserId().equals(file.getUserId()))
            {
                response.sendError(404, "文件不存在或无权限");
                return;
            }
            response.setContentType("application/octet-stream");
            FileUtils.setAttachmentResponseHeader(response, file.getFileName());
            FileUtils.writeBytes(file.getAbsPath(), response.getOutputStream());
        }
        catch (IOException e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 重命名文件
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @Log(title = "Vault文件管理", businessType = BusinessType.UPDATE)
    @PutMapping("/file/{fileId}/rename")
    public AjaxResult rename(@PathVariable("fileId") String fileId, @RequestBody Map<String, String> body)
    {
        try
        {
            String newName = body.get("newName");
            if (StringUtils.isEmpty(newName))
            {
                return AjaxResult.error("新文件名不能为空");
            }
            int result = vaultService.renameFile(fileId, newName, getUserId());
            return toAjax(result);
        }
        catch (Exception e)
        {
            log.error("重命名文件失败", e);
            return AjaxResult.error("重命名失败: " + e.getMessage());
        }
    }

    /**
     * 移动文件
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @Log(title = "Vault文件管理", businessType = BusinessType.UPDATE)
    @PutMapping("/file/{fileId}/move")
    public AjaxResult move(@PathVariable("fileId") String fileId, @RequestBody Map<String, String> body)
    {
        try
        {
            String targetSubPath = body.get("targetSubPath");
            if (StringUtils.isEmpty(targetSubPath))
            {
                return AjaxResult.error("目标路径不能为空");
            }
            int result = vaultService.moveFile(fileId, targetSubPath, getUserId());
            return toAjax(result);
        }
        catch (Exception e)
        {
            log.error("移动文件失败", e);
            return AjaxResult.error("移动失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:delete')")
    @Log(title = "Vault文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/file/{fileIds}")
    public AjaxResult remove(@PathVariable String[] fileIds)
    {
        try
        {
            int result = vaultService.deleteLingdocFileIndexByIds(fileIds, getUserId());
            return toAjax(result);
        }
        catch (Exception e)
        {
            log.error("删除文件失败", e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 新建文件夹
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @Log(title = "Vault文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/folder")
    public AjaxResult createFolder(@RequestBody Map<String, String> body)
    {
        String subPath = body.get("subPath");
        if (StringUtils.isEmpty(subPath))
        {
            return AjaxResult.error("路径不能为空");
        }
        int result = vaultService.createFolder(subPath, getUserId());
        return toAjax(result);
    }

    /**
     * 手动触发Vault扫描同步
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @Log(title = "Vault文件管理", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public AjaxResult sync()
    {
        Map<String, Integer> stats = vaultService.syncVault(getUserId());
        return AjaxResult.success(stats);
    }

    /**
     * 查询重复文件列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:list')")
    @GetMapping("/duplicates")
    public AjaxResult duplicates()
    {
        List<Map<String, Object>> list = vaultService.getDuplicateFiles(getUserId());
        return AjaxResult.success(list);
    }

    /**
     * 查询文件版本列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:list')")
    @GetMapping("/file/{fileId}/versions")
    public AjaxResult versions(@PathVariable("fileId") String fileId)
    {
        LingdocFileIndex file = vaultService.selectLingdocFileIndexById(fileId);
        if (file == null || !getUserId().equals(file.getUserId()))
        {
            return AjaxResult.error("文件不存在或无权限");
        }
        List<LingdocFileVersion> list = vaultService.selectFileVersionList(fileId);
        return AjaxResult.success(list);
    }
}
