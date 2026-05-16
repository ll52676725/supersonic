package com.tencent.supersonic.common.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class TimeExpressionParseConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private ParseMode parseMode = ParseMode.RULE_FIRST;

    private boolean enableHoliday = true;

    private boolean enableFuzzyParse = true;

    public enum ParseMode {
        RULE_ONLY, LLM_ONLY, RULE_FIRST, LLM_FIRST
    }
}
