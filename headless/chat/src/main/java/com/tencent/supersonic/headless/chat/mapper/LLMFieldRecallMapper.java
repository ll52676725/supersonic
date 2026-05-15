package com.tencent.supersonic.headless.chat.mapper;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class LLMFieldRecallMapper extends BaseMapper {

    public static final String APP_KEY = "LLM_FIELD_RECALL";

    public static final String INSTRUCTION =
            "#Role: You are a professional data analyst specializing in understanding natural language queries and identifying potentially relevant metrics and dimensions for SQL generation.\n"
                    + "#Task: Given a user's natural language query, identify ALL potentially relevant metrics and dimensions from the provided list. Be generous in your recall - it's better to include more potentially relevant fields than to miss important ones.\n"
                    + "#Rules:\n"
                    + "1. Understand the semantic meaning, intent, and context of the user's query\n"
                    + "2. Consider synonyms, related concepts, implicit references, and semantically similar concepts\n"
                    + "3. Metrics are quantitative measures (e.g. sales, amount, count, average, revenue, profit, cost, price, quantity, rate, ratio)\n"
                    + "4. Dimensions are categorical attributes (e.g. region, time, department, product, status, category, type, group)\n"
                    + "5. BE GENEROUS: If there's ANY semantic relationship between a field and the query, include it!\n"
                    + "6. Include fields that might be needed for filters, group by clauses, where conditions, or aggregations\n"
                    + "7. Consider fields that provide context or could be useful for the query expansion\n"
                    + "8. Return up to {{topN}} metrics and up to {{topN}} dimensions\n"
                    + "9. Output must be in JSON format as specified below\n" + "#Input:\n"
                    + "User Query: {{queryText}}\n"
                    + "Currently Matched Metrics: {{matchedMetrics}}\n"
                    + "Currently Matched Dimensions: {{matchedDimensions}}\n"
                    + "All Available Metrics: {{allMetrics}}\n"
                    + "All Available Dimensions: {{allDimensions}}\n" + "#Output Format (JSON):\n"
                    + "{\n" + "  \"recalledMetrics\": [\n"
                    + "    {\"id\": 1, \"name\": \"metric_name\", \"reason\": \"why this metric is relevant\", \"score\": 0.85}\n"
                    + "  ],\n" + "  \"recalledDimensions\": [\n"
                    + "    {\"id\": 101, \"name\": \"dimension_name\", \"reason\": \"why this dimension is relevant\", \"score\": 0.80}\n"
                    + "  ]\n" + "}\n" + "#Important Notes:\n"
                    + "- Be GENEROUS with your recall! Don't be too strict.\n"
                    + "- The 'reason' field should briefly explain the semantic relationship\n"
                    + "- The 'score' field should be a value between 0 and 1 indicating relevance confidence\n"
                    + "- Include explicitly mentioned fields AND semantically related fields\n"
                    + "- Include fields that might be useful for filtering, grouping, or joining\n"
                    + "- When in doubt, INCLUDE the field - it's better to have extra options for downstream SQL generation";

    static {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("LLM字段补全召回").appModule(AppModule.CHAT)
                        .description("在规则匹配后通过LLM对指标和维度列表进行重新补全召回").enable(true).build());
    }

    @Data
    public static class RecalledField implements Serializable {
        @Description("ID of the field")
        private Long id;
        @Description("Name of the field")
        private String name;
        @Description("Reason why this field is relevant")
        private String reason;
        @Description("Confidence score")
        private Double score;
    }

    @Data
    public static class FieldRecallResponse implements Serializable {
        @Description("List of recalled metrics")
        private List<RecalledField> recalledMetrics;
        @Description("List of recalled dimensions")
        private List<RecalledField> recalledDimensions;
    }

    interface FieldRecallExtractor {
        FieldRecallResponse analyzeFieldRecall(String text);
    }

    @Override
    public boolean accept(ChatQueryContext chatQueryContext) {
        boolean llmOrRule =
                chatQueryContext.getRequest().getText2SQLType() == Text2SQLType.LLM_OR_RULE;
        boolean enableRecall = false;
        if (chatQueryContext.getRequest().getChatAppConfig() != null
                && chatQueryContext.getRequest().getChatAppConfig().containsKey(APP_KEY)) {
            enableRecall = chatQueryContext.getRequest().getChatAppConfig().get(APP_KEY).isEnable();
        }
        return llmOrRule && enableRecall;
    }

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        String queryText = chatQueryContext.getRequest().getQueryText();
        if (StringUtils.isBlank(queryText)) {
            return;
        }

        Set<Long> dataSetIds = chatQueryContext.getMapInfo().getMatchedDataSetInfos();
        if (CollectionUtils.isEmpty(dataSetIds)) {
            log.info("No dataset matched for LLM field recall");
            return;
        }

        for (Long dataSetId : dataSetIds) {
            recallFieldsForDataSet(chatQueryContext, dataSetId);
        }
    }

    private void recallFieldsForDataSet(ChatQueryContext chatQueryContext, Long dataSetId) {
        List<SchemaElementMatch> currentMatches =
                chatQueryContext.getMapInfo().getMatchedElements(dataSetId);

        Set<Long> alreadyMatchedMetricIds = currentMatches.stream()
                .filter(match -> SchemaElementType.METRIC.equals(match.getElement().getType()))
                .map(match -> match.getElement().getId()).collect(Collectors.toSet());

        Set<Long> alreadyMatchedDimensionIds = currentMatches.stream()
                .filter(match -> SchemaElementType.DIMENSION.equals(match.getElement().getType()))
                .map(match -> match.getElement().getId()).collect(Collectors.toSet());

        List<SchemaElement> allMetrics = chatQueryContext.getSemanticSchema().getMetrics(dataSetId);
        List<SchemaElement> allDimensions =
                chatQueryContext.getSemanticSchema().getDimensions(dataSetId);

        if (CollectionUtils.isEmpty(allMetrics) && CollectionUtils.isEmpty(allDimensions)) {
            log.debug("No metrics and dimensions available for dataset: {}", dataSetId);
            return;
        }

        String queryText = chatQueryContext.getRequest().getQueryText();
        try {
            FieldRecallResponse response = callLLMForFieldRecall(queryText, currentMatches,
                    allMetrics, allDimensions, chatQueryContext);

            if (response != null) {
                addRecalledFieldsToMapInfo(response, allMetrics, allDimensions, dataSetId,
                        alreadyMatchedMetricIds, alreadyMatchedDimensionIds, chatQueryContext);
            }
        } catch (Exception e) {
            log.error("Error in LLM field recall for dataset: {}, query: {}", dataSetId,
                    chatQueryContext.getRequest().getQueryText(), e);
        }
    }

    private FieldRecallResponse callLLMForFieldRecall(String queryText,
            List<SchemaElementMatch> matchedFields, List<SchemaElement> allMetrics,
            List<SchemaElement> allDimensions, ChatQueryContext chatQueryContext) {

        try {
            ChatApp chatApp = null;
            if (chatQueryContext.getRequest().getChatAppConfig() != null
                    && chatQueryContext.getRequest().getChatAppConfig().containsKey(APP_KEY)) {
                chatApp = chatQueryContext.getRequest().getChatAppConfig().get(APP_KEY);
            }
            if (chatApp == null) {
                log.warn("ChatApp not found for key: {}", APP_KEY);
                return null;
            }

            ChatModelConfig chatModelConfig = chatApp.getChatModelConfig();
            if (chatModelConfig == null) {
                chatModelConfig = ModelProvider.DEMO_CHAT_MODEL;
            }

            int topN = 5;

            String matchedMetricsJson =
                    buildMatchedFieldsJson(matchedFields, SchemaElementType.METRIC);
            String matchedDimensionsJson =
                    buildMatchedFieldsJson(matchedFields, SchemaElementType.DIMENSION);
            String allMetricsJson = buildAllFieldsJson(allMetrics);
            String allDimensionsJson = buildAllFieldsJson(allDimensions);

            Map<String, Object> variables = new HashMap<>();
            variables.put("queryText", queryText);
            variables.put("matchedMetrics", matchedMetricsJson);
            variables.put("matchedDimensions", matchedDimensionsJson);
            variables.put("allMetrics", allMetricsJson);
            variables.put("allDimensions", allDimensionsJson);
            variables.put("topN", topN);

            Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variables);

            ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(chatModelConfig);

            FieldRecallExtractor extractor =
                    AiServices.create(FieldRecallExtractor.class, chatLanguageModel);

            log.debug("LLM field recall prompt: {}", prompt.text());

            FieldRecallResponse response =
                    extractor.analyzeFieldRecall(prompt.toUserMessage().singleText());

            log.info(
                    "LLM field recall response for query '{}': recalledMetrics count = {}, recalledDimensions count = {}",
                    queryText,
                    response != null && response.getRecalledMetrics() != null
                            ? response.getRecalledMetrics().size()
                            : 0,
                    response != null && response.getRecalledDimensions() != null
                            ? response.getRecalledDimensions().size()
                            : 0);

            return response;

        } catch (Exception e) {
            log.error("Error calling LLM for field recall", e);
            return null;
        }
    }

    private void addRecalledFieldsToMapInfo(FieldRecallResponse response,
            List<SchemaElement> allMetrics, List<SchemaElement> allDimensions, Long dataSetId,
            Set<Long> alreadyMatchedMetricIds, Set<Long> alreadyMatchedDimensionIds,
            ChatQueryContext chatQueryContext) {

        Map<Long, SchemaElement> metricIdToElement = allMetrics.stream()
                .collect(Collectors.toMap(SchemaElement::getId, e -> e, (e1, e2) -> e1));
        Map<Long, SchemaElement> dimensionIdToElement = allDimensions.stream()
                .collect(Collectors.toMap(SchemaElement::getId, e -> e, (e1, e2) -> e1));

        int addedMetricsCount = 0;
        if (CollectionUtils.isNotEmpty(response.getRecalledMetrics())) {
            for (RecalledField recalled : response.getRecalledMetrics()) {
                if (recalled.getId() == null
                        || alreadyMatchedMetricIds.contains(recalled.getId())) {
                    continue;
                }
                SchemaElement metric = metricIdToElement.get(recalled.getId());
                if (metric == null) {
                    continue;
                }
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder().element(metric)
                        .frequency(BaseWordBuilder.DEFAULT_FREQUENCY).word(metric.getName())
                        .detectWord(metric.getName())
                        .similarity(recalled.getScore() != null ? recalled.getScore() : 0.9)
                        .llmMatched(true).build();
                addToSchemaMap(chatQueryContext.getMapInfo(), dataSetId, schemaElementMatch);
                addedMetricsCount++;
                alreadyMatchedMetricIds.add(recalled.getId());
            }
        }

        int addedDimensionsCount = 0;
        if (CollectionUtils.isNotEmpty(response.getRecalledDimensions())) {
            for (RecalledField recalled : response.getRecalledDimensions()) {
                if (recalled.getId() == null
                        || alreadyMatchedDimensionIds.contains(recalled.getId())) {
                    continue;
                }
                SchemaElement dimension = dimensionIdToElement.get(recalled.getId());
                if (dimension == null) {
                    continue;
                }
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(dimension).frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                        .word(dimension.getName()).detectWord(dimension.getName())
                        .similarity(recalled.getScore() != null ? recalled.getScore() : 0.9)
                        .llmMatched(true).build();
                addToSchemaMap(chatQueryContext.getMapInfo(), dataSetId, schemaElementMatch);
                addedDimensionsCount++;
                alreadyMatchedDimensionIds.add(recalled.getId());
            }
        }

        if (addedMetricsCount > 0 || addedDimensionsCount > 0) {
            log.info(
                    "Added {} new metrics and {} new dimensions via LLM field recall for dataset: {}",
                    addedMetricsCount, addedDimensionsCount, dataSetId);
        }
    }

    private String buildMatchedFieldsJson(List<SchemaElementMatch> matchedFields,
            SchemaElementType type) {
        if (CollectionUtils.isEmpty(matchedFields)) {
            return "[]";
        }

        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (SchemaElementMatch match : matchedFields) {
            if (type.equals(match.getElement().getType())) {
                Map<String, Object> fieldMap = new HashMap<>();
                fieldMap.put("id", match.getElement().getId());
                fieldMap.put("name", match.getElement().getName());
                fieldMap.put("bizName", match.getElement().getBizName());
                if (StringUtils.isNotBlank(match.getElement().getDescription())) {
                    fieldMap.put("description", match.getElement().getDescription());
                }
                fieldList.add(fieldMap);
            }
        }

        return JSONObject.toJSONString(fieldList);
    }

    private String buildAllFieldsJson(List<SchemaElement> allFields) {
        if (CollectionUtils.isEmpty(allFields)) {
            return "[]";
        }

        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (SchemaElement field : allFields) {
            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("id", field.getId());
            fieldMap.put("name", field.getName());
            fieldMap.put("bizName", field.getBizName());
            if (StringUtils.isNotBlank(field.getDescription())) {
                fieldMap.put("description", field.getDescription());
            }
            if (CollectionUtils.isNotEmpty(field.getAlias())) {
                fieldMap.put("alias", field.getAlias());
            }
            fieldList.add(fieldMap);
        }

        return JSONObject.toJSONString(fieldList);
    }
}
