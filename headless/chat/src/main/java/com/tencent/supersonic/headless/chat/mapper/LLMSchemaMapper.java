package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.LLMSemanticResult;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
import dev.langchain4j.store.embedding.Retrieval;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
public class LLMSchemaMapper extends BaseMapper {

    @Override
    public boolean accept(ChatQueryContext chatQueryContext) {
        boolean b0 = MapModeEnum.LOOSE.equals(chatQueryContext.getRequest().getMapModeEnum());
        boolean b1 = chatQueryContext.getRequest().getText2SQLType() == Text2SQLType.LLM_OR_RULE;
        return b0 || b1;
    }

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        if (semanticSchema == null) {
            log.warn("SemanticSchema is null, skipping LLM schema mapping");
            return;
        }

        LLMSemanticMatchStrategy matchStrategy =
                ContextUtils.getBean(LLMSemanticMatchStrategy.class);
        List<LLMSemanticResult> matchResults = getMatches(chatQueryContext, matchStrategy);

        if (CollectionUtils.isEmpty(matchResults)) {
            log.info("LLM semantic mapper found no matches for query: {}",
                    chatQueryContext.getRequest().getQueryText());
            return;
        }

        for (LLMSemanticResult matchResult : matchResults) {
            processMatchResult(matchResult, chatQueryContext);
        }

        logMatchResults(matchResults, chatQueryContext.getRequest().getQueryText());
    }

    private void processMatchResult(LLMSemanticResult matchResult,
            ChatQueryContext chatQueryContext) {
        Long dataSetId = matchResult.getDataSetId();
        if (Objects.isNull(dataSetId)) {
            log.warn("DataSetId is null for match result: {}", matchResult);
            return;
        }

        SchemaElementType elementType = matchResult.getElementType();
        if (Objects.isNull(elementType)) {
            log.warn("ElementType is null for match result: {}", matchResult);
            return;
        }

        SchemaElement schemaElement = null;
        String word = matchResult.getName();

        if (SchemaElementType.VALUE.equals(elementType)) {
            schemaElement = getSchemaElementForValue(matchResult,
                    chatQueryContext.getSemanticSchema(), dataSetId);
        } else {
            Long elementId = extractElementId(matchResult);
            if (elementId != null) {
                schemaElement = getSchemaElement(dataSetId, elementType, elementId,
                        chatQueryContext.getSemanticSchema());
            }
        }

        if (schemaElement == null) {
            log.debug("SchemaElement not found for match result: {}", matchResult);
            return;
        }

        SchemaElementMatch schemaElementMatch =
                buildSchemaElementMatch(schemaElement, matchResult, word);

        addToSchemaMap(chatQueryContext.getMapInfo(), dataSetId, schemaElementMatch);
    }

    private Long extractElementId(LLMSemanticResult matchResult) {
        String id = matchResult.getId();
        if (StringUtils.isBlank(id)) {
            return Retrieval.getLongId(matchResult.getMetadata().get("id"));
        }

        if (id.contains("_")) {
            String[] parts = id.split("_");
            if (parts.length > 0 && StringUtils.isNumeric(parts[0])) {
                return Long.parseLong(parts[0]);
            }
        }

        if (StringUtils.isNumeric(id)) {
            return Long.parseLong(id);
        }

        return Retrieval.getLongId(matchResult.getMetadata().get("id"));
    }

    private SchemaElement getSchemaElementForValue(LLMSemanticResult matchResult,
            SemanticSchema semanticSchema, Long dataSetId) {
        Long dimensionId = matchResult.getDimensionId();
        if (dimensionId == null && matchResult.getMetadata() != null) {
            dimensionId = Retrieval.getLongId(matchResult.getMetadata().get("id"));
        }

        if (dimensionId == null) {
            return null;
        }

        SchemaElement dimension = semanticSchema.getDimension(dimensionId);
        if (dimension == null) {
            dimension = getSchemaElement(dataSetId, SchemaElementType.DIMENSION, dimensionId,
                    semanticSchema);
        }

        return dimension;
    }

    private SchemaElementMatch buildSchemaElementMatch(SchemaElement schemaElement,
            LLMSemanticResult matchResult, String word) {

        return SchemaElementMatch.builder().element(schemaElement)
                .frequency(BaseWordBuilder.DEFAULT_FREQUENCY).word(word)
                .similarity(matchResult.getSimilarity()).detectWord(matchResult.getName())
                .llmMatched(true).build();
    }

    private void logMatchResults(List<LLMSemanticResult> matchResults, String queryText) {
        if (log.isInfoEnabled()) {
            for (LLMSemanticResult matchResult : matchResults) {
                log.info(
                        "LLM semantic match - name=[{}], type=[{}], dataSetId=[{}], "
                                + "similarity=[{}], reason=[{}], metadata=[{}]",
                        matchResult.getName(), matchResult.getElementType(),
                        matchResult.getDataSetId(), matchResult.getSimilarity(),
                        matchResult.getReason(), matchResult.getMetadata());
            }
        }
    }
}
