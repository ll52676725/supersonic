package com.tencent.supersonic.headless.chat.knowledge;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
public class LLMSemanticResult extends MapResult {

    private String id;
    private Long dataSetId;
    private SchemaElementType elementType;
    private Long dimensionId;
    private String reason;
    private Map<String, String> metadata;
    private boolean llmMatched;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LLMSemanticResult that = (LLMSemanticResult) o;
        return Objects.equal(id, that.id) && Objects.equal(dataSetId, that.dataSetId)
                && Objects.equal(elementType, that.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, dataSetId, elementType);
    }

    @Override
    public String getMapKey() {
        return this.getName() + Constants.UNDERLINE + this.getId() + Constants.UNDERLINE
                + this.getDataSetId();
    }
}
