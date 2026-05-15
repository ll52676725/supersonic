package com.tencent.supersonic.headless.chat.mapper;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class LLMMetricRecallMapper extends BaseMapper {

    public static final String APP_KEY = "LLM_METRIC_RECALL";

    public static final String INSTRUCTION =
            "#Role: You are a professional data analyst specializing in understanding natural language queries and identifying relevant metrics.\n"
                    + "#Task: Given a user's natural language query and a list of all available metrics, analyze which metrics the user is most likely referring to, even if they are not explicitly mentioned.\n"
                    + "#Rules:\n"
                    + "1. Understand the semantic meaning and intent of the user's query\n"
                    + "2. Consider synonyms, related concepts, and implicit references\n"
                    + "3. Metrics are quantitative measures (e.g. sales, amount, count, average, revenue, profit)\n"
                    + "4. Prioritize metrics that directly answer the user's question\n"
                    + "5. Also consider related metrics that might provide valuable context\n"
                    + "6. Return at most {{topN}} metrics\n"
                    + "7. Output must be in JSON format as specified below\n" + "#Input:\n"
                    + "User Query: {{queryText}}\n"
                    + "Currently Matched Metrics: {{matchedMetrics}}\n"
                    + "All Available Metrics: {{allMetrics}}\n" + "#Output Format (JSON):\n" + "{\n"
                    + "  \"recalledMetrics\": [\n"
                    + "    {\"id\": 1, \"name\": \"metric_name\", \"reason\": \"why this metric is relevant\", \"score\": 0.95}\n"
                    + "  ]\n" + "}\n" + "#Notes:\n"
                    + "- The 'reason' field should briefly explain why you think this metric is relevant to the query\n"
                    + "- The 'score' field should be a value between 0 and 1 indicating your confidence in this match\n"
                    + "- Include both explicitly mentioned and implicitly relevant metrics\n"
                    + "- If no metrics are relevant, return an empty array";

    static {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("LLM指标补全召回").appModule(AppModule.CHAT)
                        .description("在规则匹配后通过LLM对指标列表进行重新补全召回").enable(true).build());
    }

    @Data
    public static class RecalledMetric implements Serializable {
        @Description("ID of the metric")
        private Long id;
        @Description("Name of the metric")
        private String name;
        @Description("Reason why this metric is relevant")
        private String reason;
        @Description("Confidence score")
        private Double score;
    }

    @Data
    public static class MetricRecallResponse implements Serializable {
        @Description("List of recalled metrics")
        private List<RecalledMetric> recalledMetrics;
    }

    interface MetricRecallExtractor {
        MetricRecallResponse analyzeMetricRecall(String text);
    }

    @Override
    public boolean accept(ChatQueryContext chatQueryContext) {
        boolean llmOrRule =
                chatQueryContext.getRequest().getText2SQLType() == Text2SQLType.LLM_OR_RULE;
        MapperConfig mapperConfig = ContextUtils.getBean(MapperConfig.class);
        boolean enableRecall = Boolean.parseBoolean(
                mapperConfig.getParameterValue(MapperConfig.LLM_METRIC_RECALL_ENABLE));
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
            log.info("No dataset matched for LLM metric recall");
            return;
        }

        for (Long dataSetId : dataSetIds) {
            recallMetricsForDataSet(chatQueryContext, dataSetId);
        }
    }

    private void recallMetricsForDataSet(ChatQueryContext chatQueryContext, Long dataSetId) {
        List<SchemaElementMatch> currentMatches =
                chatQueryContext.getMapInfo().getMatchedElements(dataSetId);

        Set<Long> alreadyMatchedMetricIds = currentMatches.stream()
                .filter(match -> SchemaElementType.METRIC.equals(match.getElement().getType()))
                .map(match -> match.getElement().getId()).collect(Collectors.toSet());

        List<SchemaElement> allMetrics = chatQueryContext.getSemanticSchema().getMetrics(dataSetId);
        if (CollectionUtils.isEmpty(allMetrics)) {
            log.debug("No metrics available for dataset: {}", dataSetId);
            return;
        }

        String queryText = chatQueryContext.getRequest().getQueryText();
        try {
            MetricRecallResponse response =
                    callLLMForMetricRecall(queryText, currentMatches, allMetrics, chatQueryContext);

            if (response != null && CollectionUtils.isNotEmpty(response.getRecalledMetrics())) {
                addRecalledMetricsToMapInfo(response, allMetrics, dataSetId,
                        alreadyMatchedMetricIds, chatQueryContext);
            }
        } catch (Exception e) {
            log.error("Error in LLM metric recall for dataset: {}, query: {}", dataSetId,
                    chatQueryContext.getRequest().getQueryText(), e);
        }
    }

    private MetricRecallResponse callLLMForMetricRecall(String queryText,
            List<SchemaElementMatch> matchedMetrics, List<SchemaElement> allMetrics,
            ChatQueryContext chatQueryContext) {

        try {
            Optional<ChatApp> chatAppOpt = ChatAppManager.getApp(APP_KEY);
            if (!chatAppOpt.isPresent()) {
                log.warn("ChatApp not found for key: {}", APP_KEY);
                return null;
            }
            ChatApp chatApp = chatAppOpt.get();

            ChatModelConfig chatModelConfig = getChatModelConfig(chatQueryContext);
            if (chatModelConfig == null) {
                log.warn("No valid chat model config available");
                return null;
            }

            MapperConfig mapperConfig = ContextUtils.getBean(MapperConfig.class);
            int topN = Integer
                    .parseInt(mapperConfig.getParameterValue(MapperConfig.LLM_METRIC_RECALL_TOP_N));

            String matchedMetricsJson = buildMatchedMetricsJson(matchedMetrics);
            String allMetricsJson = buildAllMetricsJson(allMetrics);

            Map<String, Object> variables = new HashMap<>();
            variables.put("queryText", queryText);
            variables.put("matchedMetrics", matchedMetricsJson);
            variables.put("allMetrics", allMetricsJson);
            variables.put("topN", topN);

            Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variables);

            ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(chatModelConfig);

            MetricRecallExtractor extractor =
                    AiServices.create(MetricRecallExtractor.class, chatLanguageModel);

            log.debug("LLM metric recall prompt: {}", prompt.text());

            MetricRecallResponse response =
                    extractor.analyzeMetricRecall(prompt.toUserMessage().singleText());

            log.info("LLM metric recall response for query '{}': recalledMetrics count = {}",
                    queryText,
                    response != null && response.getRecalledMetrics() != null
                            ? response.getRecalledMetrics().size()
                            : 0);

            return response;

        } catch (Exception e) {
            log.error("Error calling LLM for metric recall", e);
            return null;
        }
    }

    private void addRecalledMetricsToMapInfo(MetricRecallResponse response,
            List<SchemaElement> allMetrics, Long dataSetId, Set<Long> alreadyMatchedMetricIds,
            ChatQueryContext chatQueryContext) {

        Map<Long, SchemaElement> metricIdToElement = allMetrics.stream()
                .collect(Collectors.toMap(SchemaElement::getId, e -> e, (e1, e2) -> e1));

        int addedCount = 0;
        for (RecalledMetric recalled : response.getRecalledMetrics()) {
            if (recalled.getId() == null || alreadyMatchedMetricIds.contains(recalled.getId())) {
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
            addedCount++;
            alreadyMatchedMetricIds.add(recalled.getId());
        }

        if (addedCount > 0) {
            log.info("Added {} new metrics via LLM recall for dataset: {}", addedCount, dataSetId);
        }
    }

    private String buildMatchedMetricsJson(List<SchemaElementMatch> matchedMetrics) {
        if (CollectionUtils.isEmpty(matchedMetrics)) {
            return "[]";
        }

        List<Map<String, Object>> metricList = new ArrayList<>();
        for (SchemaElementMatch match : matchedMetrics) {
            if (SchemaElementType.METRIC.equals(match.getElement().getType())) {
                Map<String, Object> metricMap = new HashMap<>();
                metricMap.put("id", match.getElement().getId());
                metricMap.put("name", match.getElement().getName());
                metricMap.put("bizName", match.getElement().getBizName());
                if (StringUtils.isNotBlank(match.getElement().getDescription())) {
                    metricMap.put("description", match.getElement().getDescription());
                }
                metricList.add(metricMap);
            }
        }

        return JSONObject.toJSONString(metricList);
    }

    private String buildAllMetricsJson(List<SchemaElement> allMetrics) {
        if (CollectionUtils.isEmpty(allMetrics)) {
            return "[]";
        }

        List<Map<String, Object>> metricList = new ArrayList<>();
        for (SchemaElement metric : allMetrics) {
            Map<String, Object> metricMap = new HashMap<>();
            metricMap.put("id", metric.getId());
            metricMap.put("name", metric.getName());
            metricMap.put("bizName", metric.getBizName());
            if (StringUtils.isNotBlank(metric.getDescription())) {
                metricMap.put("description", metric.getDescription());
            }
            if (CollectionUtils.isNotEmpty(metric.getAlias())) {
                metricMap.put("alias", metric.getAlias());
            }
            metricList.add(metricMap);
        }

        return JSONObject.toJSONString(metricList);
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
}
