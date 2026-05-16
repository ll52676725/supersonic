package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.TimeExpressionParseConfig;
import com.tencent.supersonic.headless.chat.parser.llm.LLMBasedTimeExpressionParser;
import com.tencent.supersonic.headless.chat.parser.rule.RuleBasedTimeExpressionParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TimeExpressionParserFacade {

    @Autowired
    private RuleBasedTimeExpressionParser ruleParser;

    @Autowired
    private LLMBasedTimeExpressionParser llmParser;

    public DateConf parse(String queryText) {
        return parse(queryText, new TimeExpressionParseConfig());
    }

    public DateConf parse(String queryText, TimeExpressionParseConfig config) {
        if (queryText == null || queryText.isEmpty()) {
            return null;
        }

        DateConf result = null;

        switch (config.getParseMode()) {
            case RULE_ONLY:
                result = ruleParser.parse(queryText, config);
                break;
            case LLM_ONLY:
                result = llmParser.parse(queryText, config);
                break;
            case RULE_FIRST:
                result = ruleParser.parse(queryText, config);
                if (result == null) {
                    log.info("Rule parser failed, falling back to LLM parser for: {}", queryText);
                    result = llmParser.parse(queryText, config);
                }
                break;
            case LLM_FIRST:
                result = llmParser.parse(queryText, config);
                if (result == null) {
                    log.info("LLM parser failed, falling back to rule parser for: {}", queryText);
                    result = ruleParser.parse(queryText, config);
                }
                break;
            default:
                result = ruleParser.parse(queryText, config);
                break;
        }

        if (result != null) {
            log.info("Successfully parsed time expression: {} -> {} to {}", queryText,
                    result.getStartDate(), result.getEndDate());
        }

        return result;
    }

    public RuleBasedTimeExpressionParser getRuleParser() {
        return ruleParser;
    }

    public LLMBasedTimeExpressionParser getLlmParser() {
        return llmParser;
    }
}
