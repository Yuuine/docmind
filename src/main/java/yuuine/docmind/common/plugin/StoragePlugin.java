package yuuine.docmind.common.plugin;

import java.io.InputStream;

public interface StoragePlugin {
    String getName();
    String storeFile(String fileId, String filename, InputStream inputStream, String contentType);
    InputStream retrieveFile(String fileId);
    void deleteFile(String fileId);
    boolean exists(String fileId);
}
