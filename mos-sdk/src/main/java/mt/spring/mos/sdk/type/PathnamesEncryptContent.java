package mt.spring.mos.sdk.type;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2022/9/4
 */
@Data
public class PathnamesEncryptContent implements EncryptContent {
    private List<String> pathnames;

    public PathnamesEncryptContent() {
    }

    public PathnamesEncryptContent(String pathname) {
        pathnames = new ArrayList<>();
        pathnames.add(pathname);
    }

    public PathnamesEncryptContent(List<String> pathnames) {
        this.pathnames = new ArrayList<>();
        this.pathnames.addAll(pathnames);
    }

}
