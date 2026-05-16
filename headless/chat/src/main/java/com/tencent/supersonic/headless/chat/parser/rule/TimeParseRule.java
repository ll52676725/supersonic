package com.tencent.supersonic.headless.chat.parser.rule;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TimeParseRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ruleName;

    private String pattern;

    private RuleType ruleType;

    private Integer priority = 0;

    private List<String> keywords;

    private TimeOffset timeOffset;

    public enum RuleType {
        RELATIVE, ABSOLUTE, HOLIDAY, RANGE
    }

    @Data
    public static class TimeOffset implements Serializable {
        private static final long serialVersionUID = 1L;

        private OffsetType offsetType = OffsetType.DAY;
        private Integer offsetValue = 0;
        private Integer unit = 1;
        private boolean isStartOfPeriod = false;
        private boolean isEndOfPeriod = false;

        public enum OffsetType {
            DAY, WEEK, MONTH, QUARTER, YEAR, HOLIDAY
        }
    }
}
