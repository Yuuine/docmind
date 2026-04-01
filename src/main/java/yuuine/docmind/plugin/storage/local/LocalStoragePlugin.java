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
        log.info("Storing file: {} to {}", filename, properties.getBasePath());
        return "file-id-" + System.currentTimeMillis();
    }

    @Override
    public InputStream retrieveFile(String fileId) {
        log.info("Retrieving file: {}", fileId);
        return null;
    }

    @Override
    public void deleteFile(String fileId) {
        log.info("Deleting file: {}", fileId);
    }

    @Override
    public boolean exists(String fileId) {
        log.info("Checking file exists: {}", fileId);
        return false;
    }
}
