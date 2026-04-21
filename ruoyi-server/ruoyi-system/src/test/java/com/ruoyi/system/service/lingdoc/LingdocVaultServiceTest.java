package com.ruoyi.system.service.lingdoc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocFileVersion;
import com.ruoyi.system.mapper.lingdoc.LingdocDesensitizedFileMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFileAiMetaMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFileVersionMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocTagBindingMapper;
import com.ruoyi.system.service.lingdoc.ILingdocUserRepoService;

/**
 * Vault文件管理服务单元测试
 */
class LingdocVaultServiceTest
{
    @Mock
    private LingdocFileIndexMapper fileIndexMapper;

    @Mock
    private LingdocFileVersionMapper fileVersionMapper;

    @Mock
    private LingdocFileAiMetaMapper fileAiMetaMapper;

    @Mock
    private LingdocDesensitizedFileMapper desensitizedFileMapper;

    @Mock
    private LingdocTagBindingMapper tagBindingMapper;

    @Mock
    private ILingdocUserRepoService userRepoService;

    @InjectMocks
    private LingdocVaultServiceImpl vaultService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSelectLingdocFileIndexById()
    {
        LingdocFileIndex expected = new LingdocFileIndex();
        expected.setFileId("f_001");
        expected.setFileName("test.txt");
        when(fileIndexMapper.selectLingdocFileIndexById("f_001")).thenReturn(expected);

        LingdocFileIndex result = vaultService.selectLingdocFileIndexById("f_001");

        assertNotNull(result);
        assertEquals("f_001", result.getFileId());
        assertEquals("test.txt", result.getFileName());
        verify(fileIndexMapper).selectLingdocFileIndexById("f_001");
    }

    @Test
    void testGetVaultTree()
    {
        List<String> subPaths = Arrays.asList(
            "学习资料/大三上/操作系统",
            "学习资料/大三上/计算机网络",
            "申请材料/奖学金"
        );
        when(fileIndexMapper.selectDistinctSubPathByUserId(1L)).thenReturn(subPaths);

        List<Map<String, Object>> tree = vaultService.getVaultTree(1L);

        assertNotNull(tree);
        assertEquals(2, tree.size()); // 学习资料, 申请材料
        
        Map<String, Object> studyNode = tree.get(0);
        assertEquals("学习资料", studyNode.get("label"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> studyChildren = (List<Map<String, Object>>) studyNode.get("children");
        assertEquals(1, studyChildren.size());
        assertEquals("大三上", studyChildren.get(0).get("label"));
    }

    @Test
    void testGetVaultTree_Empty()
    {
        when(fileIndexMapper.selectDistinctSubPathByUserId(1L)).thenReturn(new ArrayList<>());

        List<Map<String, Object>> tree = vaultService.getVaultTree(1L);

        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }

    @Test
    void testDeleteLingdocFileIndexById() throws IOException
    {
        LingdocFileIndex file = new LingdocFileIndex();
        file.setFileId("f_001");
        file.setUserId(1L);
        file.setAbsPath("/tmp/test.txt");
        when(fileIndexMapper.selectLingdocFileIndexById("f_001")).thenReturn(file);
        when(fileVersionMapper.deleteLingdocFileVersionByFileId("f_001")).thenReturn(1);
        when(fileAiMetaMapper.deleteLingdocFileAiMetaById("f_001")).thenReturn(1);
        when(desensitizedFileMapper.deleteLingdocDesensitizedFileByFileId("f_001")).thenReturn(1);
        when(tagBindingMapper.deleteLingdocTagBindingByTarget("F", "f_001")).thenReturn(1);
        when(fileIndexMapper.deleteLingdocFileIndexById("f_001")).thenReturn(1);

        int result = vaultService.deleteLingdocFileIndexById("f_001", 1L);

        assertEquals(1, result);
        verify(fileVersionMapper).deleteLingdocFileVersionByFileId("f_001");
        verify(fileAiMetaMapper).deleteLingdocFileAiMetaById("f_001");
        verify(desensitizedFileMapper).deleteLingdocDesensitizedFileByFileId("f_001");
        verify(tagBindingMapper).deleteLingdocTagBindingByTarget("F", "f_001");
        verify(fileIndexMapper).deleteLingdocFileIndexById("f_001");
    }

    @Test
    void testDeleteLingdocFileIndexById_NoPermission() throws IOException
    {
        LingdocFileIndex file = new LingdocFileIndex();
        file.setFileId("f_001");
        file.setUserId(2L); // 不同用户
        when(fileIndexMapper.selectLingdocFileIndexById("f_001")).thenReturn(file);

        assertThrows(RuntimeException.class, () -> {
            vaultService.deleteLingdocFileIndexById("f_001", 1L);
        });
    }

    @Test
    void testRecordFileVersion()
    {
        when(fileVersionMapper.selectMaxVersionNoByFileId("f_001")).thenReturn(2);
        when(fileVersionMapper.insertLingdocFileVersion(any(LingdocFileVersion.class))).thenReturn(1);

        int result = vaultService.recordFileVersion("f_001", "/path/old.txt", "0", 1L);

        assertEquals(1, result);
        verify(fileVersionMapper).insertLingdocFileVersion(argThat(v -> 
            v.getFileId().equals("f_001") && v.getVersionNo() == 3
        ));
    }

    @Test
    void testSyncVault(@TempDir Path tempDir) throws IOException
    {
        // 创建测试文件
        Path documents = tempDir.resolve("documents/学习资料");
        Files.createDirectories(documents);
        Path testFile = documents.resolve("test.txt");
        Files.writeString(testFile, "Hello Vault");

        LingdocFileIndex query = new LingdocFileIndex();
        query.setUserId(1L);
        when(fileIndexMapper.selectLingdocFileIndexList(argThat(q -> q.getUserId() != null && q.getUserId().equals(1L))))
            .thenReturn(new ArrayList<>());
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(eq(1L), anyString())).thenReturn(new ArrayList<>());
        when(fileIndexMapper.insertLingdocFileIndex(any(LingdocFileIndex.class))).thenReturn(1);

        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());

        Map<String, Integer> stats = vaultService.syncVault(1L);

        assertNotNull(stats);
        assertTrue(stats.get("added") >= 1);
        assertEquals(0, stats.get("duplicates"));
        verify(fileIndexMapper, atLeastOnce()).insertLingdocFileIndex(any(LingdocFileIndex.class));
    }

    @Test
    void testSyncVault_WithDuplicates(@TempDir Path tempDir) throws IOException
    {
        // 创建两个内容相同的文件（不同路径）
        Path documents = tempDir.resolve("documents");
        Path dir1 = documents.resolve("folderA");
        Path dir2 = documents.resolve("folderB");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Path fileA = dir1.resolve("same.txt");
        Path fileB = dir2.resolve("same.txt");
        Files.writeString(fileA, "Duplicate Content");
        Files.writeString(fileB, "Duplicate Content");

        LingdocFileIndex query = new LingdocFileIndex();
        query.setUserId(1L);
        when(fileIndexMapper.selectLingdocFileIndexList(argThat(q -> q.getUserId() != null && q.getUserId().equals(1L))))
            .thenReturn(new ArrayList<>());
        AtomicInteger checksumCallCount = new AtomicInteger(0);
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(eq(1L), anyString())).thenAnswer(inv -> {
            if (checksumCallCount.incrementAndGet() == 1) {
                return new ArrayList<>(); // 第一个文件：数据库中没有相同checksum
            }
            LingdocFileIndex existing = new LingdocFileIndex();
            existing.setFileId("f_first");
            return Arrays.asList(existing); // 第二个文件：数据库中已有相同checksum
        });
        when(fileIndexMapper.insertLingdocFileIndex(any(LingdocFileIndex.class))).thenReturn(1);

        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());

        Map<String, Integer> stats = vaultService.syncVault(1L);

        assertNotNull(stats);
        assertEquals(2, stats.get("added"));
        assertEquals(1, stats.get("duplicates")); // 第二个文件检测到重复checksum
        assertEquals(0, stats.get("deleted"));
        verify(fileIndexMapper, times(2)).insertLingdocFileIndex(any(LingdocFileIndex.class));
    }

    @Test
    void testGetDuplicateFiles()
    {
        when(fileIndexMapper.selectDuplicateChecksums(1L)).thenReturn(Arrays.asList("abc123", "def456"));

        LingdocFileIndex file1 = new LingdocFileIndex();
        file1.setFileId("f_001");
        file1.setFileName("a.txt");
        file1.setVaultPath("folderA/a.txt");
        LingdocFileIndex file2 = new LingdocFileIndex();
        file2.setFileId("f_002");
        file2.setFileName("a.txt");
        file2.setVaultPath("folderB/a.txt");

        when(fileIndexMapper.selectLingdocFileIndexByChecksum(1L, "abc123")).thenReturn(Arrays.asList(file1, file2));
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(1L, "def456")).thenReturn(Arrays.asList(file1));

        List<Map<String, Object>> result = vaultService.getDuplicateFiles(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("abc123", result.get(0).get("checksum"));
        assertEquals(2, result.get(0).get("count"));
        @SuppressWarnings("unchecked")
        List<LingdocFileIndex> files = (List<LingdocFileIndex>) result.get(0).get("files");
        assertEquals(2, files.size());
    }

    @Test
    void testSyncVault_ExistingDuplicate(@TempDir Path tempDir) throws IOException
    {
        // 数据库中已有一个文件，物理仓库新增一个内容相同的文件
        Path documents = tempDir.resolve("documents");
        Path dir1 = documents.resolve("folderA");
        Path dir2 = documents.resolve("folderB");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Path fileA = dir1.resolve("same.txt");
        Path fileB = dir2.resolve("same.txt");
        Files.writeString(fileA, "Same Content");
        Files.writeString(fileB, "Same Content");

        LingdocFileIndex existingFile = new LingdocFileIndex();
        existingFile.setFileId("f_old");
        existingFile.setUserId(1L);
        existingFile.setAbsPath(fileA.toString());
        existingFile.setFileName("same.txt");
        existingFile.setFileType("txt");
        existingFile.setChecksum("d6b4e84ee7f31d0f3c2e5a7b9c8d1f4e5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0");

        when(fileIndexMapper.selectLingdocFileIndexList(argThat(q -> q.getUserId() != null && q.getUserId().equals(1L))))
            .thenReturn(Arrays.asList(existingFile));
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(eq(1L), anyString())).thenReturn(Arrays.asList(existingFile));
        when(fileIndexMapper.insertLingdocFileIndex(any(LingdocFileIndex.class))).thenReturn(1);

        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());

        Map<String, Integer> stats = vaultService.syncVault(1L);

        assertNotNull(stats);
        assertEquals(1, stats.get("added")); // fileB 是新文件（按absPath）
        assertEquals(1, stats.get("duplicates")); // fileB 的checksum与existingFile重复
        assertEquals(0, stats.get("deleted")); // fileA 仍存在于物理仓库
    }

    @Test
    void testCreateFolder(@TempDir Path tempDir)
    {
        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());

        int result = vaultService.createFolder("大四/毕业论文", 1L);

        assertEquals(1, result);
        assertTrue(Files.exists(tempDir.resolve("documents/大四/毕业论文")));
    }

    @Test
    void testCreateFolder_Exists(@TempDir Path tempDir) throws IOException
    {
        Files.createDirectories(tempDir.resolve("documents/已有文件夹"));
        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());

        int result = vaultService.createFolder("已有文件夹", 1L);

        assertEquals(0, result);
    }

    @Test
    void testUploadFile_Success(@TempDir Path tempDir) throws IOException
    {
        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(eq(1L), anyString())).thenReturn(new ArrayList<>());
        when(fileIndexMapper.insertLingdocFileIndex(any(LingdocFileIndex.class))).thenReturn(1);

        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockFile.getSize()).thenReturn(11L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn("Hello Vault".getBytes());
        doAnswer(inv -> {
            java.io.File target = inv.getArgument(0);
            Files.write(target.toPath(), "Hello Vault".getBytes());
            return null;
        }).when(mockFile).transferTo(any(java.io.File.class));

        LingdocFileIndex result = vaultService.uploadFile(mockFile, "", 1L);

        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        assertEquals("txt", result.getFileType());
        assertEquals(1L, result.getUserId());
        assertEquals("0", result.getSourceType());
        assertNotNull(result.getFileId());
        assertNotNull(result.getChecksum());
        assertTrue(Files.exists(tempDir.resolve("documents/test.txt")));
        verify(fileIndexMapper).insertLingdocFileIndex(any(LingdocFileIndex.class));
    }

    @Test
    void testUploadFile_WithSubPath(@TempDir Path tempDir) throws IOException
    {
        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(eq(1L), anyString())).thenReturn(new ArrayList<>());
        when(fileIndexMapper.insertLingdocFileIndex(any(LingdocFileIndex.class))).thenReturn(1);

        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("report.pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn("PDF content".getBytes());
        doAnswer(inv -> {
            java.io.File target = inv.getArgument(0);
            Files.write(target.toPath(), "PDF content".getBytes());
            return null;
        }).when(mockFile).transferTo(any(java.io.File.class));

        LingdocFileIndex result = vaultService.uploadFile(mockFile, "学习资料/大三上", 1L);

        assertNotNull(result);
        assertEquals("report.pdf", result.getFileName());
        assertEquals("pdf", result.getFileType());
        assertEquals("学习资料/大三上", result.getSubPath());
        assertTrue(Files.exists(tempDir.resolve("documents/学习资料/大三上/report.pdf")));
    }

    @Test
    void testUploadFile_Duplicate(@TempDir Path tempDir) throws IOException
    {
        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());

        LingdocFileIndex existing = new LingdocFileIndex();
        existing.setFileId("f_old");
        existing.setFileName("existing.txt");
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(eq(1L), anyString())).thenReturn(Arrays.asList(existing));

        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("duplicate.txt");
        when(mockFile.getSize()).thenReturn(11L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn("Hello Vault".getBytes());
        doAnswer(inv -> {
            Path target = inv.getArgument(0);
            Files.write(target, "Hello Vault".getBytes());
            return null;
        }).when(mockFile).transferTo(any(java.io.File.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            vaultService.uploadFile(mockFile, "", 1L);
        });
        assertNotNull(exception.getMessage());
    }

    @Test
    void testUploadFile_BinaryFile(@TempDir Path tempDir) throws IOException
    {
        when(userRepoService.getUserRepoPath(1L)).thenReturn(tempDir.toString());
        when(fileIndexMapper.selectLingdocFileIndexByChecksum(eq(1L), anyString())).thenReturn(new ArrayList<>());
        when(fileIndexMapper.insertLingdocFileIndex(any(LingdocFileIndex.class))).thenReturn(1);

        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("image.png");
        when(mockFile.getSize()).thenReturn(2048L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn(new byte[] { (byte)0x89, 0x50, 0x4E, 0x47 });
        doAnswer(inv -> {
            java.io.File target = inv.getArgument(0);
            Files.write(target.toPath(), new byte[] { (byte)0x89, 0x50, 0x4E, 0x47 });
            return null;
        }).when(mockFile).transferTo(any(java.io.File.class));

        LingdocFileIndex result = vaultService.uploadFile(mockFile, "", 1L);

        assertNotNull(result);
        assertEquals("image.png", result.getFileName());
        assertEquals("png", result.getFileType());
        // 二进制文件不应读取内容到 fileContent
        assertNull(result.getFileContent());
    }
}
