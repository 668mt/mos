package mt.spring.mos.sdk.utils;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2022/9/4
 */
@Data
public class DirPathsEncryptContent implements EncryptContent {
    private List<String> paths = new ArrayList<>();

    public DirPathsEncryptContent() {
    }
    public DirPathsEncryptContent(String path) {
        paths.add(path);
    }

    public DirPathsEncryptContent(List<String> paths) {
        this.paths.addAll(paths);
    }
}
