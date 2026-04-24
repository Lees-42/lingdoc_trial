package com.ruoyi.web.controller.lingdoc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.mock.web.MockMultipartFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.RuoYiApplication;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.service.lingdoc.ILingdocVaultService;

/**
 * Vault文件浏览器 Controller 集成测试
 */
@SpringBootTest(classes = RuoYiApplication.class)
class LingdocVaultControllerIT
{
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private ILingdocVaultService vaultService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private LingdocFileIndex testFile;
    private UsernamePasswordAuthenticationToken auth;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        SysUser sysUser = new SysUser();
        sysUser.setUserId(TEST_USER_ID);
        sysUser.setDeptId(100L);
        sysUser.setUserName("admin");
        sysUser.setNickName("admin");
        sysUser.setPassword("N/A");

        Set<String> permissions = Set.of(
            "lingdoc:vault:list",
            "lingdoc:vault:download",
            "lingdoc:vault:edit",
            "lingdoc:vault:delete"
        );
        LoginUser loginUser = new LoginUser(TEST_USER_ID, 100L, sysUser, permissions);
        auth = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());

        testFile = new LingdocFileIndex();
        testFile.setFileId("f_test001");
        testFile.setUserId(TEST_USER_ID);
        testFile.setFileName("test.txt");
        testFile.setFileType("txt");
        testFile.setFileSize(1024L);
        testFile.setVaultPath("学习资料/test.txt");
        testFile.setAbsPath("/upload/vault/学习资料/test.txt");
        testFile.setSubPath("学习资料");
        testFile.setSourceType("0");
    }

    @Test
    void testGetVaultTree() throws Exception
    {
        Map<String, Object> node = new HashMap<>();
        node.put("label", "学习资料");
        node.put("value", "学习资料");
        node.put("children", Collections.emptyList());
        when(vaultService.getVaultTree(TEST_USER_ID)).thenReturn(Arrays.asList(node));

        mockMvc.perform(get("/lingdoc/vault/tree")
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].label").value("学习资料"));
    }

    @Test
    void testListVaultFiles() throws Exception
    {
        when(vaultService.selectLingdocFileIndexList(any(LingdocFileIndex.class)))
            .thenReturn(Arrays.asList(testFile));

        mockMvc.perform(get("/lingdoc/vault/files")
                .param("subPath", "学习资料")
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.rows[0].fileName").value("test.txt"));
    }

    @Test
    void testGetFile() throws Exception
    {
        when(vaultService.selectLingdocFileIndexById("f_test001")).thenReturn(testFile);

        mockMvc.perform(get("/lingdoc/vault/file/f_test001")
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.fileName").value("test.txt"));
    }

    @Test
    void testGetFile_NotFound() throws Exception
    {
        when(vaultService.selectLingdocFileIndexById("not_exist")).thenReturn(null);

        mockMvc.perform(get("/lingdoc/vault/file/not_exist")
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("文件不存在或无权限"));
    }

    @Test
    void testGetFileContent() throws Exception
    {
        when(vaultService.selectLingdocFileIndexById("f_test001")).thenReturn(testFile);
        Map<String, Object> contentData = new HashMap<>();
        contentData.put("content", "Hello World");
        contentData.put("encoding", "UTF-8");
        contentData.put("lineCount", 1);
        when(vaultService.getFileContent("f_test001", TEST_USER_ID))
            .thenReturn(AjaxResult.success(contentData));

        mockMvc.perform(get("/lingdoc/vault/file/f_test001/content")
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").value("Hello World"));
    }

    @Test
    void testRenameFile() throws Exception
    {
        when(vaultService.renameFile(eq("f_test001"), eq("newName.txt"), eq(TEST_USER_ID))).thenReturn(1);

        Map<String, String> body = new HashMap<>();
        body.put("newName", "newName.txt");

        mockMvc.perform(put("/lingdoc/vault/file/f_test001/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testMoveFile() throws Exception
    {
        when(vaultService.moveFile(eq("f_test001"), eq("目标目录"), eq(TEST_USER_ID))).thenReturn(1);

        Map<String, String> body = new HashMap<>();
        body.put("targetSubPath", "目标目录");

        mockMvc.perform(put("/lingdoc/vault/file/f_test001/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDeleteFile() throws Exception
    {
        when(vaultService.deleteLingdocFileIndexByIds(any(String[].class), eq(TEST_USER_ID))).thenReturn(1);

        mockMvc.perform(delete("/lingdoc/vault/file/f_test001")
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testCreateFolder() throws Exception
    {
        when(vaultService.createFolder(eq("学习资料/新文件夹"), eq(TEST_USER_ID))).thenReturn(1);

        Map<String, String> body = new HashMap<>();
        body.put("subPath", "学习资料/新文件夹");

        mockMvc.perform(post("/lingdoc/vault/folder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSyncVault() throws Exception
    {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("added", 5);
        stats.put("updated", 2);
        stats.put("deleted", 0);
        when(vaultService.syncVault(eq(TEST_USER_ID))).thenReturn(stats);

        mockMvc.perform(post("/lingdoc/vault/sync")
            .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.added").value(5))
            .andExpect(jsonPath("$.data.updated").value(2));
    }

    @Test
    void testDownloadFile() throws Exception
    {
        when(vaultService.selectLingdocFileIndexById("f_test001")).thenReturn(testFile);

        mockMvc.perform(get("/lingdoc/vault/file/f_test001/download")
            .with(authentication(auth)))
            .andExpect(status().isOk());
    }

    @Test
    void testUploadFile() throws Exception
    {
        LingdocFileIndex uploadedFile = new LingdocFileIndex();
        uploadedFile.setFileId("f_upload001");
        uploadedFile.setFileName("upload.txt");
        uploadedFile.setFileType("txt");
        uploadedFile.setFileSize(12L);
        uploadedFile.setSubPath("学习资料");
        when(vaultService.uploadFile(any(), eq("学习资料"), eq(TEST_USER_ID))).thenReturn(uploadedFile);

        MockMultipartFile file = new MockMultipartFile("file", "upload.txt", "text/plain", "Hello World".getBytes());

        mockMvc.perform(multipart("/lingdoc/vault/upload")
                .file(file)
                .param("subPath", "学习资料")
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.fileName").value("upload.txt"));
    }

    @Test
    void testUploadFile_NoPermission() throws Exception
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(2L);
        sysUser.setDeptId(100L);
        sysUser.setUserName("user");
        sysUser.setNickName("user");
        sysUser.setPassword("N/A");

        Set<String> noEditPerms = Set.of("lingdoc:vault:list");
        LoginUser loginUser = new LoginUser(2L, 100L, sysUser, noEditPerms);
        UsernamePasswordAuthenticationToken noEditAuth = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());

        MockMultipartFile file = new MockMultipartFile("file", "upload.txt", "text/plain", "Hello World".getBytes());

        mockMvc.perform(multipart("/lingdoc/vault/upload")
                .file(file)
                .with(authentication(noEditAuth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void testUploadFile_EmptyFile() throws Exception
    {
        when(vaultService.uploadFile(any(), any(), eq(TEST_USER_ID))).thenThrow(new RuntimeException("上传文件不能为空"));

        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/lingdoc/vault/upload")
                .file(file)
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("上传文件不能为空"));
    }
}
