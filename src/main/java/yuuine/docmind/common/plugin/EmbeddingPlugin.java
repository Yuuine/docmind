package yuuine.docmind.common.plugin;

import java.util.List;

public interface EmbeddingPlugin {
    String getName();
    List<float[]> embed(List<String> texts);
    float[] embed(String text);
    int getDimension();
}
