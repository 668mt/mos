package mt.spring.mos.sdk.type;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2022/9/6
 */
@Data
@NoArgsConstructor
public class ResourceIdsEncryptContent implements EncryptContent {
    private List<Long> resourceIds = new ArrayList<>();

    public ResourceIdsEncryptContent(Long resourceId) {
        resourceIds.add(resourceId);
    }

    public ResourceIdsEncryptContent(List<Long> resourceIds) {
        this.resourceIds.addAll(resourceIds);
    }
}
