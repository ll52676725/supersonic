package com.tencent.supersonic.headless.chat.mapper;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.LLMSemanticResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.service.AiServices;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LLMSemanticMatchStrategy extends BaseMatchStrategy<LLMSemanticResult> {

    public static final String APP_KEY = "LLM_SEMANTIC_MATCH";

    @Autowired
    protected MapperConfig mapperConfig;

    public static final String INSTRUCTION =
            "#Role: You are a professional data analyst specializing in understanding natural language queries and matching them to database schema elements.\n"
                    + "#Task: Given a user's natural language query and a list of available schema elements (metrics, dimensions, values), analyze which schema elements the user is most likely referring to.\n"
                    + "#Rules:\n"
                    + "1. Understand the semantic meaning of the user's query, not just keyword matching\n"
                    + "2. Metrics are quantitative measures (e.g., sales, amount, count, average)\n"
                    + "3. Dimensions are categorical attributes used for grouping/filtering (e.g., date, region, department)\n"
                    + "4. Values are specific instances of dimensions (e.g., 'January' is a value of 'date' dimension)\n"
                    + "5. Consider synonyms and related concepts when matching\n"
                    + "6. Only return elements that are clearly mentioned or strongly implied in the query\n"
                    + "7. Output must be in JSON format as specified below\n" + "#Input:\n"
                    + "User Query: {{queryText}}\n" + "Available Metrics: {{metrics}}\n"
                    + "Available Dimensions: {{dimensions}}\n" + "#Output Format (JSON):\n" + "{\n"
                    + "  \"matchedMetrics\": [\n"
                    + "    {\"id\": 1, \"name\": \"metric_name\", \"reason\": \"why this metric matches\"}\n"
                    + "  ],\n" + "  \"matchedDimensions\": [\n"
                    + "    {\"id\": 2, \"name\": \"dimension_name\", \"reason\": \"why this dimension matches\"}\n"
                    + "  ],\n" + "  \"matchedValues\": [\n"
                    + "    {\"dimensionId\": 2, \"value\": \"specific_value\", \"reason\": \"why this value matches\"}\n"
                    + "  ]\n" + "}\n" + "#Notes:\n"
                    + "- If no elements match, return empty arrays\n"
                    + "- The 'reason' field should briefly explain why you think this element matches the query\n"
                    + "- Consider both explicit mentions and implicit references based on query context";

    public LLMSemanticMatchStrategy() {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("LLM语义匹配").appModule(AppModule.CHAT)
                        .description("通过大模型进行语义理解，匹配查询中的指标、维度和值").enable(true).build());
    }

    @Data
    public static class MatchedElement implements Serializable {
        private Long id;
        private String name;
        private String reason;
    }

    @Data
    public static class MatchedValue implements Serializable {
        private Long dimensionId;
        private String value;
        private String reason;
    }

    @Data
    public static class SemanticMatchResponse implements Serializable {
        @Description("List of matched metrics")
        private List<MatchedElement> matchedMetrics;

        @Description("List of matched dimensions")
        private List<MatchedElement> matchedDimensions;

        @Description("List of matched dimension values")
        private List<MatchedValue> matchedValues;
    }

    interface SemanticMatchExtractor {
        SemanticMatchResponse analyzeSemanticMatch(String text);
    }

    @Override
    public Map<MatchText, List<LLMSemanticResult>> match(ChatQueryContext chatQueryContext,
            List<S2Term> terms, Set<Long> detectDataSetIds) {

        String queryText = chatQueryContext.getRequest().getQueryText();
        Map<MatchText, List<LLMSemanticResult>> result = new HashMap<>();

        if (StringUtils.isBlank(queryText)) {
            log.warn("Query text is empty");
            return result;
        }

        boolean useLLMSemantic = Boolean.parseBoolean(
                mapperConfig.getParameterValue(MapperConfig.LLM_SEMANTIC_MATCHER_ENABLE));

        if (!useLLMSemantic) {
            log.info("LLM semantic matching is disabled");
            return result;
        }

        List<LLMSemanticResult> semanticResults = detect(chatQueryContext, terms, detectDataSetIds);

        if (CollectionUtils.isNotEmpty(semanticResults)) {
            result.put(MatchText.builder().regText(queryText).detectSegment(queryText).build(),
                    semanticResults);
        }

        return result;
    }

    @Override
    public List<LLMSemanticResult> detect(ChatQueryContext chatQueryContext, List<S2Term> terms,
            Set<Long> detectDataSetIds) {

        String queryText = chatQueryContext.getRequest().getQueryText();
        Set<LLMSemanticResult> results = new HashSet<>();

        try {
            Set<Long> effectiveDataSetIds =
                    getEffectiveDataSetIds(chatQueryContext, detectDataSetIds);

            if (CollectionUtils.isEmpty(effectiveDataSetIds)) {
                log.warn("No valid dataset IDs for LLM semantic matching");
                return new ArrayList<>();
            }

            for (Long dataSetId : effectiveDataSetIds) {
                List<SchemaElement> metrics =
                        chatQueryContext.getSemanticSchema().getMetrics(dataSetId);
                List<SchemaElement> dimensions =
                        chatQueryContext.getSemanticSchema().getDimensions(dataSetId);

                if (CollectionUtils.isEmpty(metrics) && CollectionUtils.isEmpty(dimensions)) {
                    log.debug("No metrics or dimensions for dataset: {}", dataSetId);
                    continue;
                }

                SemanticMatchResponse response =
                        callLLMForSemanticMatch(queryText, metrics, dimensions, chatQueryContext);

                if (response != null) {
                    results.addAll(convertToLLMSemanticResults(response, metrics, dimensions,
                            dataSetId, chatQueryContext));
                }
            }

        } catch (Exception e) {
            log.error("Error in LLM semantic detection for query: {}", queryText, e);
        }

        return new ArrayList<>(results);
    }

    private Set<Long> getEffectiveDataSetIds(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds) {
        Set<Long> effectiveIds = new HashSet<>();

        if (CollectionUtils.isNotEmpty(detectDataSetIds)) {
            effectiveIds.addAll(detectDataSetIds);
        } else if (chatQueryContext.getRequest() != null
                && CollectionUtils.isNotEmpty(chatQueryContext.getRequest().getDataSetIds())) {
            effectiveIds.addAll(chatQueryContext.getRequest().getDataSetIds());
        } else if (chatQueryContext.getSemanticSchema() != null
                && chatQueryContext.getSemanticSchema().getDataSets() != null) {
            effectiveIds.addAll(chatQueryContext.getSemanticSchema().getDataSets().stream()
                    .map(SchemaElement::getDataSetId).collect(Collectors.toSet()));
        }

        return effectiveIds;
    }

    private SemanticMatchResponse callLLMForSemanticMatch(String queryText,
            List<SchemaElement> metrics, List<SchemaElement> dimensions,
            ChatQueryContext chatQueryContext) {

        try {
            ChatApp chatApp = ChatAppManager.getChatApp(APP_KEY);
            if (chatApp == null) {
                log.warn("ChatApp not found for key: {}", APP_KEY);
                return null;
            }

            ChatModelConfig chatModelConfig = getChatModelConfig(chatQueryContext);
            if (chatModelConfig == null) {
                log.warn("No valid chat model config available");
                return null;
            }

            String metricsJson = buildElementsJson(metrics);
            String dimensionsJson = buildElementsJson(dimensions);

            Map<String, Object> variables = new HashMap<>();
            variables.put("queryText", queryText);
            variables.put("metrics", metricsJson);
            variables.put("dimensions", dimensionsJson);

            Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variables);

            ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(chatModelConfig);

            SemanticMatchExtractor extractor =
                    AiServices.create(SemanticMatchExtractor.class, chatLanguageModel);

            log.debug("LLM semantic match prompt: {}", prompt.text());

            SemanticMatchResponse response =
                    extractor.analyzeSemanticMatch(prompt.toUserMessage().singleText());

            log.info(
                    "LLM semantic match response for query '{}': matchedMetrics={}, matchedDimensions={}",
                    queryText,
                    response != null && response.getMatchedMetrics() != null
                            ? response.getMatchedMetrics().size()
                            : 0,
                    response != null && response.getMatchedDimensions() != null
                            ? response.getMatchedDimensions().size()
                            : 0);

            return response;

        } catch (Exception e) {
            log.error("Error calling LLM for semantic matching", e);
            return null;
        }
    }

    private ChatModelConfig getChatModelConfig(ChatQueryContext chatQueryContext) {
        if (chatQueryContext.getRequest() != null
                && chatQueryContext.getRequest().getChatAppConfig() != null && chatQueryContext
                        .getRequest().getChatAppConfig().containsKey("REWRITE_MULTI_TURN")) {
            return chatQueryContext.getRequest().getChatAppConfig().get("REWRITE_MULTI_TURN")
                    .getChatModelConfig();
        }
        return ModelProvider.DEMO_CHAT_MODEL;
    }

    private String buildElementsJson(List<SchemaElement> elements) {
        if (CollectionUtils.isEmpty(elements)) {
            return "[]";
        }

        List<Map<String, Object>> elementList = new ArrayList<>();
        for (SchemaElement element : elements) {
            Map<String, Object> elementMap = new HashMap<>();
            elementMap.put("id", element.getId());
            elementMap.put("name", element.getName());
            elementMap.put("bizName", element.getBizName());

            if (CollectionUtils.isNotEmpty(element.getAlias())) {
                elementMap.put("alias", element.getAlias());
            }
            if (StringUtils.isNotBlank(element.getDescription())) {
                elementMap.put("description", element.getDescription());
            }

            elementList.add(elementMap);
        }

        return JSONObject.toJSONString(elementList);
    }

    private List<LLMSemanticResult> convertToLLMSemanticResults(SemanticMatchResponse response,
            List<SchemaElement> metrics, List<SchemaElement> dimensions, Long dataSetId,
            ChatQueryContext chatQueryContext) {

        List<LLMSemanticResult> results = new ArrayList<>();
        Map<Long, SchemaElement> metricIdToElement = metrics.stream()
                .collect(Collectors.toMap(SchemaElement::getId, e -> e, (e1, e2) -> e1));
        Map<Long, SchemaElement> dimensionIdToElement = dimensions.stream()
                .collect(Collectors.toMap(SchemaElement::getId, e -> e, (e1, e2) -> e1));

        double similarityThreshold = Double.parseDouble(
                mapperConfig.getParameterValue(MapperConfig.LLM_SEMANTIC_MATCHER_THRESHOLD));

        if (response.getMatchedMetrics() != null) {
            for (MatchedElement matched : response.getMatchedMetrics()) {
                SchemaElement element = metricIdToElement.get(matched.getId());
                if (element != null) {
                    LLMSemanticResult result = buildLLMSemanticResult(element, dataSetId,
                            SchemaElementType.METRIC, matched.getReason(), similarityThreshold);
                    results.add(result);
                }
            }
        }

        if (response.getMatchedDimensions() != null) {
            for (MatchedElement matched : response.getMatchedDimensions()) {
                SchemaElement element = dimensionIdToElement.get(matched.getId());
                if (element != null) {
                    LLMSemanticResult result = buildLLMSemanticResult(element, dataSetId,
                            SchemaElementType.DIMENSION, matched.getReason(), similarityThreshold);
                    results.add(result);
                }
            }
        }

        if (response.getMatchedValues() != null) {
            for (MatchedValue matchedValue : response.getMatchedValues()) {
                SchemaElement dimension = dimensionIdToElement.get(matchedValue.getDimensionId());
                if (dimension != null) {
                    LLMSemanticResult result = buildLLMSemanticResultForValue(dimension, dataSetId,
                            matchedValue.getValue(), matchedValue.getReason(), similarityThreshold);
                    results.add(result);
                }
            }
        }

        return results;
    }

    private LLMSemanticResult buildLLMSemanticResult(SchemaElement element, Long dataSetId,
            SchemaElementType type, String reason, double similarity) {

        LLMSemanticResult result = new LLMSemanticResult();
        result.setId(element.getId() + "_" + type);
        result.setName(element.getName());
        result.setDataSetId(dataSetId);
        result.setElementType(type);
        result.setReason(reason);
        result.setSimilarity(similarity);
        result.setLlmMatched(true);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("id", String.valueOf(element.getId()));
        metadata.put("dataSetId", String.valueOf(dataSetId));
        metadata.put("type", type.name());
        metadata.put("name", element.getName());
        metadata.put("bizName", element.getBizName());
        if (StringUtils.isNotBlank(element.getDescription())) {
            metadata.put("description", element.getDescription());
        }
        result.setMetadata(metadata);

        return result;
    }

    private LLMSemanticResult buildLLMSemanticResultForValue(SchemaElement dimension,
            Long dataSetId, String value, String reason, double similarity) {

        LLMSemanticResult result = new LLMSemanticResult();
        result.setId(dimension.getId() + "_" + SchemaElementType.VALUE + "_" + value);
        result.setName(value);
        result.setDataSetId(dataSetId);
        result.setElementType(SchemaElementType.VALUE);
        result.setDimensionId(dimension.getId());
        result.setReason(reason);
        result.setSimilarity(similarity);
        result.setLlmMatched(true);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("id", String.valueOf(dimension.getId()));
        metadata.put("dataSetId", String.valueOf(dataSetId));
        metadata.put("type", SchemaElementType.VALUE.name());
        metadata.put("dimensionName", dimension.getName());
        metadata.put("value", value);
        result.setMetadata(metadata);

        return result;
    }
}
