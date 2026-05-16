package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.TimeExpressionParseConfig;
import com.tencent.supersonic.headless.chat.parser.rule.RuleBasedTimeExpressionParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TimeExpressionParserTest {

    private final RuleBasedTimeExpressionParser ruleParser = new RuleBasedTimeExpressionParser();
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Test
    public void testRecentDaysParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("前7天的销售额", config);
        assertNotNull(result);
        log.info("前7天解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());
        assertEquals(LocalDate.now().minusDays(7).format(DATE_FORMATTER), result.getStartDate());
        assertEquals(LocalDate.now().format(DATE_FORMATTER), result.getEndDate());
    }

    @Test
    public void testLastMonthParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("上个月的订单量", config);
        assertNotNull(result);
        log.info("上个月解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());

        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        assertEquals(lastMonth.withDayOfMonth(1).format(DATE_FORMATTER), result.getStartDate());
        assertEquals(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).format(DATE_FORMATTER),
                result.getEndDate());
    }

    @Test
    public void testThisMonthParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("本月的访问量", config);
        assertNotNull(result);
        log.info("本月解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());

        LocalDate today = LocalDate.now();
        assertEquals(today.withDayOfMonth(1).format(DATE_FORMATTER), result.getStartDate());
        assertEquals(today.format(DATE_FORMATTER), result.getEndDate());
    }

    @Test
    public void testHolidayParsing_DragonBoat() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("去年端午的活动数据", config);
        assertNotNull(result);
        log.info("去年端午解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());

        int lastYear = LocalDate.now().getYear() - 1;
        assertTrue(result.getStartDate().startsWith(String.valueOf(lastYear)));
    }

    @Test
    public void testLastWeekParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("上周的用户增长", config);
        assertNotNull(result);
        log.info("上周解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());
    }

    @Test
    public void testThisWeekParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("本周的活跃用户", config);
        assertNotNull(result);
        log.info("本周解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());
    }

    @Test
    public void testYesterdayParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("昨天的订单", config);
        assertNotNull(result);
        log.info("昨天解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());

        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertEquals(yesterday.format(DATE_FORMATTER), result.getStartDate());
        assertEquals(yesterday.format(DATE_FORMATTER), result.getEndDate());
    }

    @Test
    public void testNDaysAgoParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("30天前的历史数据", config);
        assertNotNull(result);
        log.info("30天前解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());

        LocalDate expectedDate = LocalDate.now().minusDays(30);
        assertEquals(expectedDate.format(DATE_FORMATTER), result.getStartDate());
    }

    @Test
    public void testSpringFestivalParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("今年春节期间的消费", config);
        assertNotNull(result);
        log.info("春节解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());
    }

    @Test
    public void testNationalDayParsing() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        DateConf result = ruleParser.parse("国庆的旅游数据", config);
        assertNotNull(result);
        log.info("国庆解析结果: start={}, end={}", result.getStartDate(), result.getEndDate());
    }

    @Test
    public void testAllTimeExpressions() {
        TimeExpressionParseConfig config = new TimeExpressionParseConfig();
        config.setParseMode(TimeExpressionParseConfig.ParseMode.RULE_ONLY);

        String[] testCases = {"前7天的销售额", "近30天的用户数", "上个月的订单量", "本月的访问量", "上周的活跃用户", "本周的新增用户",
                        "去年的全年收入", "今年的目标完成情况", "昨天的数据统计", "3天前的历史记录", "2周前的数据", "1个月前的订单",
                        "去年端午的活动数据", "今年春节的消费情况", "中秋的节日活动数据", "国庆的旅游统计", "五一的销售数据", "清明的活动数据",
                        "元旦的新年活动"};

        for (String testCase : testCases) {
            DateConf result = ruleParser.parse(testCase, config);
            if (result != null) {
                log.info("✅ 解析成功: {} -> {} ~ {}", testCase, result.getStartDate(),
                        result.getEndDate());
            } else {
                log.warn("❌ 解析失败: {}", testCase);
            }
        }
    }
}
