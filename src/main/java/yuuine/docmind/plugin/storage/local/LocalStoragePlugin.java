package yuuine.docmind.plugin.storage.local;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import yuuine.docmind.common.plugin.StoragePlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RequiredArgsConstructor
public class LocalStoragePlugin implements StoragePlugin {

    private final LocalStorageProperties properties;

    @Override
    public String getName() {
        return "local";
    }

    @Override
    public String storeFile(String fileId, String filename, InputStream inputStream, String contentType) {
        log.info("LocalStoragePlugin.storeFile 被调用: fileId={}, filename={}, contentType={}", fileId, filename, contentType);
        try {
            Path storageDir = Paths.get(properties.getBasePath());
            log.info("存储目录: {}", storageDir.toAbsolutePath());
            if (!Files.exists(storageDir)) {
                log.info("创建存储目录: {}", storageDir);
                Files.createDirectories(storageDir);
            }

            Path filePath = storageDir.resolve(fileId);
            log.info("文件路径: {}", filePath.toAbsolutePath());

            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                inputStream.transferTo(outputStream);
            }

            log.info("文件存储成功: fileId={}, filename={}", fileId, filename);
            return fileId;
        } catch (IOException e) {
            log.error("文件存储失败: filename={}", filename, e);
            throw new RuntimeException("文件存储失败", e);
        }
    }

    @Override
    public InputStream retrieveFile(String fileId) {
        try {
            Path filePath = Paths.get(properties.getBasePath()).resolve(fileId);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + fileId);
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("文件读取失败: fileId={}", fileId, e);
            throw new RuntimeException("文件读取失败", e);
        }
    }

    @Override
    public void deleteFile(String fileId) {
        try {
            Path filePath = Paths.get(properties.getBasePath()).resolve(fileId);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: fileId={}", fileId);
        } catch (IOException e) {
            log.error("文件删除失败: fileId={}", fileId, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public boolean exists(String fileId) {
        Path filePath = Paths.get(properties.getBasePath()).resolve(fileId);
        return Files.exists(filePath);
    }
}
