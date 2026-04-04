package yuuine.docmind.plugin.storage.local;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yuuine.docmind.common.plugin.StoragePlugin;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStoragePlugin implements StoragePlugin {

    private final LocalStorageProperties properties;

    @Override
    public String getName() {
        return "local";
    }

    @Override
    public String storeFile(String filename, InputStream inputStream, String contentType) {
        // TODO: 实现文件存储
        // 1. 使用配置的 basePath 作为存储根目录
        // 2. 生成唯一 fileId (UUID)，建立 fileId -> 实际文件路径的映射
        // 3. 将 inputStream 写入目标文件
        // 4. 可选：保存元数据 (filename, contentType, fileSize) 到映射文件或数据库
        // 5. 处理磁盘空间不足、IO异常等情况
        log.info("Storing file: {} to {}", filename, properties.getBasePath());
        return "file-id-" + System.currentTimeMillis();
    }

    @Override
    public InputStream retrieveFile(String fileId) {
        // TODO: 实现获取文件流
        // 1. 根据 fileId 查找实际文件路径
        // 2. 打开文件并返回 InputStream
        // 3. 处理文件不存在的情况，返回 null 或抛出异常
        log.info("Retrieving file: {}", fileId);
        return null;
    }

    @Override
    public void deleteFile(String fileId) {
        // TODO: 实现删除文件
        // 1. 根据 fileId 查找实际文件路径
        // 2. 删除文件和相关的元数据
        // 3. 处理文件不存在的情况
        log.info("Deleting file: {}", fileId);
    }

    @Override
    public boolean exists(String fileId) {
        // TODO: 实现检查文件存在
        // 1. 根据 fileId 查找实际文件路径
        // 2. 检查文件是否存在
        log.info("Checking file exists: {}", fileId);
        return false;
    }
}
