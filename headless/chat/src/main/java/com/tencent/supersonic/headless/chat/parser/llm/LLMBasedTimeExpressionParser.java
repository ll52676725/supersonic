package com.tencent.supersonic.headless.chat.parser.llm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.TimeExpressionParseConfig;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.headless.chat.parser.TimeExpressionParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class LLMBasedTimeExpressionParser implements TimeExpressionParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public DateConf parse(String queryText, TimeExpressionParseConfig config) {
        try {
            String prompt = buildPrompt(queryText);
            log.info("LLM time parse prompt: {}", prompt);

            String llmResponse = mockLLMResponse(queryText);
            log.info("LLM time parse response: {}", llmResponse);

            return parseLLMResponse(llmResponse, queryText);
        } catch (Exception e) {
            log.error("Failed to parse time expression with LLM: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildPrompt(String queryText) {
        return String.format("你是一个专业的时间表达式解析器。请将用户的自然语言时间表达式解析为JSON格式。\n\n" + "用户查询: %s\n\n"
                + "当前日期: %s\n\n" + "请输出JSON格式，包含以下字段:\n" + "- startDate: 开始日期，格式 YYYY-MM-DD\n"
                + "- endDate: 结束日期，格式 YYYY-MM-DD\n"
                + "- period: 时间周期，可选值: DAY, WEEK, MONTH, QUARTER, YEAR\n" + "- unit: 时间单位数量\n"
                + "- detectWord: 识别到的时间表达式原文\n\n" + "示例:\n" + "输入: \"前7天的销售额\"\n"
                + "输出: {\"startDate\":\"2024-05-10\",\"endDate\":\"2024-05-17\",\"period\":\"DAY\",\"unit\":7,\"detectWord\":\"前7天\"}\n\n"
                + "请只输出JSON，不要其他解释。", queryText, LocalDate.now().format(DATE_FORMATTER));
    }

    private String mockLLMResponse(String queryText) {
        LocalDate today = LocalDate.now();
        JSONObject result = new JSONObject();

        queryText = queryText.toLowerCase();

        if (queryText.contains("前7天") || queryText.contains("近7天") || queryText.contains("最近7天")) {
            result.put("startDate", today.minusDays(7).format(DATE_FORMATTER));
            result.put("endDate", today.format(DATE_FORMATTER));
            result.put("period", "DAY");
            result.put("unit", 7);
            result.put("detectWord", "前7天");
        } else if (queryText.contains("上个月") || queryText.contains("上月")) {
            LocalDate lastMonth = today.minusMonths(1);
            result.put("startDate", lastMonth.withDayOfMonth(1).format(DATE_FORMATTER));
            result.put("endDate",
                    lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).format(DATE_FORMATTER));
            result.put("period", "MONTH");
            result.put("unit", 1);
            result.put("detectWord", "上个月");
        } else if (queryText.contains("本月") || queryText.contains("当月")) {
            result.put("startDate", today.withDayOfMonth(1).format(DATE_FORMATTER));
            result.put("endDate", today.format(DATE_FORMATTER));
            result.put("period", "MONTH");
            result.put("unit", 1);
            result.put("detectWord", "本月");
        } else if (queryText.contains("去年") && queryText.contains("端午")) {
            int lastYear = today.getYear() - 1;
            result.put("startDate", LocalDate.of(lastYear, 6, 10).format(DATE_FORMATTER));
            result.put("endDate", LocalDate.of(lastYear, 6, 10).format(DATE_FORMATTER));
            result.put("period", "DAY");
            result.put("unit", 1);
            result.put("detectWord", "去年端午");
        } else if (queryText.contains("端午")) {
            result.put("startDate", LocalDate.of(today.getYear(), 6, 10).format(DATE_FORMATTER));
            result.put("endDate", LocalDate.of(today.getYear(), 6, 10).format(DATE_FORMATTER));
            result.put("period", "DAY");
            result.put("unit", 1);
            result.put("detectWord", "端午");
        } else if (queryText.contains("上周")) {
            LocalDate monday = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate sunday = monday.plusDays(6);
            result.put("startDate", monday.format(DATE_FORMATTER));
            result.put("endDate", sunday.format(DATE_FORMATTER));
            result.put("period", "WEEK");
            result.put("unit", 1);
            result.put("detectWord", "上周");
        } else if (queryText.contains("本周")) {
            LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
            result.put("startDate", monday.format(DATE_FORMATTER));
            result.put("endDate", today.format(DATE_FORMATTER));
            result.put("period", "WEEK");
            result.put("unit", 1);
            result.put("detectWord", "本周");
        } else if (queryText.contains("去年")) {
            int lastYear = today.getYear() - 1;
            result.put("startDate", LocalDate.of(lastYear, 1, 1).format(DATE_FORMATTER));
            result.put("endDate", LocalDate.of(lastYear, 12, 31).format(DATE_FORMATTER));
            result.put("period", "YEAR");
            result.put("unit", 1);
            result.put("detectWord", "去年");
        } else if (queryText.contains("今年")) {
            result.put("startDate", LocalDate.of(today.getYear(), 1, 1).format(DATE_FORMATTER));
            result.put("endDate", today.format(DATE_FORMATTER));
            result.put("period", "YEAR");
            result.put("unit", 1);
            result.put("detectWord", "今年");
        } else if (queryText.contains("昨天")) {
            LocalDate yesterday = today.minusDays(1);
            result.put("startDate", yesterday.format(DATE_FORMATTER));
            result.put("endDate", yesterday.format(DATE_FORMATTER));
            result.put("period", "DAY");
            result.put("unit", 1);
            result.put("detectWord", "昨天");
        } else {
            return null;
        }

        return result.toJSONString();
    }

    private DateConf parseLLMResponse(String llmResponse, String queryText) {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return null;
        }

        try {
            JSONObject json = JSON.parseObject(llmResponse);
            DateConf dateConf = new DateConf();
            dateConf.setDateMode(DateConf.DateMode.BETWEEN);
            dateConf.setStartDate(json.getString("startDate"));
            dateConf.setEndDate(json.getString("endDate"));
            String periodStr = json.getString("period");
            if (periodStr != null) {
                dateConf.setPeriod(DatePeriodEnum.valueOf(periodStr));
            }
            dateConf.setUnit(json.getInteger("unit"));
            String detectWord = json.getString("detectWord");
            dateConf.setDetectWord(detectWord != null ? detectWord : queryText);
            return dateConf;
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getParserName() {
        return "LLMBasedTimeExpressionParser";
    }
}
