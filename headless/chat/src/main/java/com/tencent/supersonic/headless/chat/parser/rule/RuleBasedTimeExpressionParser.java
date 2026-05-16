package com.tencent.supersonic.headless.chat.parser.rule;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.TimeExpressionParseConfig;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.headless.chat.parser.TimeExpressionParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RuleBasedTimeExpressionParser implements TimeExpressionParser {

    private final List<TimeParseRule> rules = new ArrayList<>();

    public RuleBasedTimeExpressionParser() {
        initDefaultRules();
    }

    private void initDefaultRules() {
        TimeParseRule last7Days = new TimeParseRule();
        last7Days.setRuleName("近7天");
        last7Days.setPattern(".*?(前|近|最近|过去)\\s*?7\\s*?天.*");
        last7Days.setRuleType(TimeParseRule.RuleType.RELATIVE);
        last7Days.setPriority(10);
        TimeParseRule.TimeOffset offset1 = new TimeParseRule.TimeOffset();
        offset1.setOffsetType(TimeParseRule.TimeOffset.OffsetType.DAY);
        offset1.setOffsetValue(-7);
        offset1.setUnit(7);
        last7Days.setTimeOffset(offset1);
        rules.add(last7Days);

        TimeParseRule last30Days = new TimeParseRule();
        last30Days.setRuleName("近30天");
        last30Days.setPattern(".*?(前|近|最近|过去)\\s*?30\\s*?天.*");
        last30Days.setRuleType(TimeParseRule.RuleType.RELATIVE);
        last30Days.setPriority(10);
        TimeParseRule.TimeOffset offset2 = new TimeParseRule.TimeOffset();
        offset2.setOffsetType(TimeParseRule.TimeOffset.OffsetType.DAY);
        offset2.setOffsetValue(-30);
        offset2.setUnit(30);
        last30Days.setTimeOffset(offset2);
        rules.add(last30Days);

        TimeParseRule lastMonth = new TimeParseRule();
        lastMonth.setRuleName("上个月");
        lastMonth.setPattern(".*?(上|去)\\s*?个?月.*");
        lastMonth.setRuleType(TimeParseRule.RuleType.RELATIVE);
        lastMonth.setPriority(15);
        TimeParseRule.TimeOffset offset3 = new TimeParseRule.TimeOffset();
        offset3.setOffsetType(TimeParseRule.TimeOffset.OffsetType.MONTH);
        offset3.setOffsetValue(-1);
        offset3.setStartOfPeriod(true);
        offset3.setEndOfPeriod(true);
        offset3.setUnit(1);
        lastMonth.setTimeOffset(offset3);
        rules.add(lastMonth);

        TimeParseRule thisMonth = new TimeParseRule();
        thisMonth.setRuleName("本月");
        thisMonth.setPattern(".*?(本|这)\\s*?个?月.*");
        thisMonth.setRuleType(TimeParseRule.RuleType.RELATIVE);
        thisMonth.setPriority(15);
        TimeParseRule.TimeOffset offset4 = new TimeParseRule.TimeOffset();
        offset4.setOffsetType(TimeParseRule.TimeOffset.OffsetType.MONTH);
        offset4.setOffsetValue(0);
        offset4.setStartOfPeriod(true);
        offset4.setEndOfPeriod(false);
        offset4.setUnit(1);
        thisMonth.setTimeOffset(offset4);
        rules.add(thisMonth);

        TimeParseRule lastWeek = new TimeParseRule();
        lastWeek.setRuleName("上周");
        lastWeek.setPattern(".*?上\\s*?周.*");
        lastWeek.setRuleType(TimeParseRule.RuleType.RELATIVE);
        lastWeek.setPriority(15);
        TimeParseRule.TimeOffset offset5 = new TimeParseRule.TimeOffset();
        offset5.setOffsetType(TimeParseRule.TimeOffset.OffsetType.WEEK);
        offset5.setOffsetValue(-1);
        offset5.setStartOfPeriod(true);
        offset5.setEndOfPeriod(true);
        offset5.setUnit(1);
        lastWeek.setTimeOffset(offset5);
        rules.add(lastWeek);

        TimeParseRule thisWeek = new TimeParseRule();
        thisWeek.setRuleName("本周");
        thisWeek.setPattern(".*?(本|这)\\s*?周.*");
        thisWeek.setRuleType(TimeParseRule.RuleType.RELATIVE);
        thisWeek.setPriority(15);
        TimeParseRule.TimeOffset offset6 = new TimeParseRule.TimeOffset();
        offset6.setOffsetType(TimeParseRule.TimeOffset.OffsetType.WEEK);
        offset6.setOffsetValue(0);
        offset6.setStartOfPeriod(true);
        offset6.setEndOfPeriod(false);
        offset6.setUnit(1);
        thisWeek.setTimeOffset(offset6);
        rules.add(thisWeek);

        TimeParseRule lastYear = new TimeParseRule();
        lastYear.setRuleName("去年");
        lastYear.setPattern(".*?去\\s*?年.*");
        lastYear.setRuleType(TimeParseRule.RuleType.RELATIVE);
        lastYear.setPriority(15);
        TimeParseRule.TimeOffset offset7 = new TimeParseRule.TimeOffset();
        offset7.setOffsetType(TimeParseRule.TimeOffset.OffsetType.YEAR);
        offset7.setOffsetValue(-1);
        offset7.setStartOfPeriod(true);
        offset7.setEndOfPeriod(true);
        offset7.setUnit(1);
        lastYear.setTimeOffset(offset7);
        rules.add(lastYear);

        TimeParseRule thisYear = new TimeParseRule();
        thisYear.setRuleName("今年");
        thisYear.setPattern(".*?(今|本)\\s*?年.*");
        thisYear.setRuleType(TimeParseRule.RuleType.RELATIVE);
        thisYear.setPriority(15);
        TimeParseRule.TimeOffset offset8 = new TimeParseRule.TimeOffset();
        offset8.setOffsetType(TimeParseRule.TimeOffset.OffsetType.YEAR);
        offset8.setOffsetValue(0);
        offset8.setStartOfPeriod(true);
        offset8.setEndOfPeriod(false);
        offset8.setUnit(1);
        thisYear.setTimeOffset(offset8);
        rules.add(thisYear);

        TimeParseRule yesterday = new TimeParseRule();
        yesterday.setRuleName("昨天");
        yesterday.setPattern(".*?(昨|昨天).*");
        yesterday.setRuleType(TimeParseRule.RuleType.RELATIVE);
        yesterday.setPriority(20);
        TimeParseRule.TimeOffset offset9 = new TimeParseRule.TimeOffset();
        offset9.setOffsetType(TimeParseRule.TimeOffset.OffsetType.DAY);
        offset9.setOffsetValue(-1);
        offset9.setUnit(1);
        yesterday.setTimeOffset(offset9);
        rules.add(yesterday);

        TimeParseRule today = new TimeParseRule();
        today.setRuleName("今天");
        today.setPattern(".*?(今|今天).*");
        today.setRuleType(TimeParseRule.RuleType.RELATIVE);
        today.setPriority(20);
        TimeParseRule.TimeOffset offset10 = new TimeParseRule.TimeOffset();
        offset10.setOffsetType(TimeParseRule.TimeOffset.OffsetType.DAY);
        offset10.setOffsetValue(0);
        offset10.setUnit(1);
        today.setTimeOffset(offset10);
        rules.add(today);

        TimeParseRule ndaysBefore = new TimeParseRule();
        ndaysBefore.setRuleName("N天前");
        ndaysBefore.setPattern(".*?(?<num>\\d+)\\s*?天前.*");
        ndaysBefore.setRuleType(TimeParseRule.RuleType.RELATIVE);
        ndaysBefore.setPriority(10);
        rules.add(ndaysBefore);

        TimeParseRule nweeksBefore = new TimeParseRule();
        nweeksBefore.setRuleName("N周前");
        nweeksBefore.setPattern(".*?(?<num>\\d+)\\s*?周前.*");
        nweeksBefore.setRuleType(TimeParseRule.RuleType.RELATIVE);
        nweeksBefore.setPriority(10);
        rules.add(nweeksBefore);

        TimeParseRule nmonthsBefore = new TimeParseRule();
        nmonthsBefore.setRuleName("N月前");
        nmonthsBefore.setPattern(".*?(?<num>\\d+)\\s*?月前.*");
        nmonthsBefore.setRuleType(TimeParseRule.RuleType.RELATIVE);
        nmonthsBefore.setPriority(10);
        rules.add(nmonthsBefore);

        TimeParseRule dragonBoatFestival = new TimeParseRule();
        dragonBoatFestival.setRuleName("端午");
        dragonBoatFestival.setPattern(".*?端\\s*?午.*");
        dragonBoatFestival.setRuleType(TimeParseRule.RuleType.HOLIDAY);
        dragonBoatFestival.setPriority(20);
        rules.add(dragonBoatFestival);

        TimeParseRule springFestival = new TimeParseRule();
        springFestival.setRuleName("春节");
        springFestival.setPattern(".*?春\\s*?节.*");
        springFestival.setRuleType(TimeParseRule.RuleType.HOLIDAY);
        springFestival.setPriority(20);
        rules.add(springFestival);

        TimeParseRule midAutumnFestival = new TimeParseRule();
        midAutumnFestival.setRuleName("中秋");
        midAutumnFestival.setPattern(".*?中\\s*?秋.*");
        midAutumnFestival.setRuleType(TimeParseRule.RuleType.HOLIDAY);
        midAutumnFestival.setPriority(20);
        rules.add(midAutumnFestival);

        TimeParseRule nationalDay = new TimeParseRule();
        nationalDay.setRuleName("国庆");
        nationalDay.setPattern(".*?国\\s*?庆.*");
        nationalDay.setRuleType(TimeParseRule.RuleType.HOLIDAY);
        nationalDay.setPriority(20);
        rules.add(nationalDay);

        TimeParseRule laborDay = new TimeParseRule();
        laborDay.setRuleName("五一");
        laborDay.setPattern(".*?(五\\s*?一|劳\\s*?动\\s*?节).*");
        laborDay.setRuleType(TimeParseRule.RuleType.HOLIDAY);
        laborDay.setPriority(20);
        rules.add(laborDay);

        TimeParseRule newYear = new TimeParseRule();
        newYear.setRuleName("元旦");
        newYear.setPattern(".*?元\\s*?旦.*");
        newYear.setRuleType(TimeParseRule.RuleType.HOLIDAY);
        newYear.setPriority(20);
        rules.add(newYear);

        TimeParseRule qingmingFestival = new TimeParseRule();
        qingmingFestival.setRuleName("清明");
        qingmingFestival.setPattern(".*?清\\s*?明.*");
        qingmingFestival.setRuleType(TimeParseRule.RuleType.HOLIDAY);
        qingmingFestival.setPriority(20);
        rules.add(qingmingFestival);
    }

    @Override
    public DateConf parse(String queryText, TimeExpressionParseConfig config) {
        if (queryText == null || queryText.isEmpty()) {
            return null;
        }

        rules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        for (TimeParseRule rule : rules) {
            try {
                Pattern pattern = Pattern.compile(rule.getPattern());
                Matcher matcher = pattern.matcher(queryText);
                if (matcher.matches()) {
                    log.info("Matched time rule: {} for query: {}", rule.getRuleName(), queryText);
                    DateConf dateConf = applyRule(rule, matcher, queryText);
                    if (dateConf != null) {
                        return dateConf;
                    }
                }
            } catch (Exception e) {
                log.warn("Error applying rule {}: {}", rule.getRuleName(), e.getMessage());
            }
        }

        return null;
    }

    private DateConf applyRule(TimeParseRule rule, Matcher matcher, String queryText) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        switch (rule.getRuleType()) {
            case RELATIVE:
                return parseRelativeTime(rule, matcher, today, queryText);
            case HOLIDAY:
                return parseHoliday(rule, matcher, queryText);
            case ABSOLUTE:
                return parseAbsoluteTime(rule, matcher, queryText);
            default:
                return null;
        }
    }

    private DateConf parseRelativeTime(TimeParseRule rule, Matcher matcher, LocalDate today,
            String queryText) {
        TimeParseRule.TimeOffset offset = rule.getTimeOffset();

        if (offset == null) {
            int num = 1;
            try {
                String numStr = matcher.group("num");
                if (numStr != null) {
                    num = Integer.parseInt(numStr);
                }
            } catch (Exception e) {
                num = 1;
            }

            if (rule.getRuleName().contains("天前")) {
                LocalDate targetDate = today.minusDays(num);
                return createDateConf(targetDate, targetDate, queryText, DatePeriodEnum.DAY, 1);
            } else if (rule.getRuleName().contains("周前")) {
                LocalDate targetDate = today.minusWeeks(num);
                return createDateConf(targetDate, targetDate, queryText, DatePeriodEnum.WEEK, num);
            } else if (rule.getRuleName().contains("月前")) {
                LocalDate targetDate = today.minusMonths(num);
                return createDateConf(targetDate, targetDate, queryText, DatePeriodEnum.MONTH, num);
            }
            return null;
        }

        DatePeriodEnum period = convertPeriod(offset.getOffsetType());
        LocalDate start = today;
        LocalDate end = today;

        switch (offset.getOffsetType()) {
            case DAY:
                start = today.plusDays(offset.getOffsetValue());
                end = today.plusDays(offset.getOffsetValue());
                break;
            case WEEK:
                start = today.plusWeeks(offset.getOffsetValue());
                end = today.plusWeeks(offset.getOffsetValue());
                if (offset.isStartOfPeriod()) {
                    start = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                }
                if (offset.isEndOfPeriod()) {
                    end = end.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                }
                break;
            case MONTH:
                start = today.plusMonths(offset.getOffsetValue());
                end = today.plusMonths(offset.getOffsetValue());
                if (offset.isStartOfPeriod()) {
                    start = start.with(TemporalAdjusters.firstDayOfMonth());
                }
                if (offset.isEndOfPeriod()) {
                    end = end.with(TemporalAdjusters.lastDayOfMonth());
                }
                break;
            case YEAR:
                start = today.plusYears(offset.getOffsetValue());
                end = today.plusYears(offset.getOffsetValue());
                if (offset.isStartOfPeriod()) {
                    start = start.with(TemporalAdjusters.firstDayOfYear());
                }
                if (offset.isEndOfPeriod()) {
                    end = end.with(TemporalAdjusters.lastDayOfYear());
                }
                break;
            default:
                break;
        }

        return createDateConf(start, end, queryText, period, offset.getUnit());
    }

    private DateConf parseHoliday(TimeParseRule rule, Matcher matcher, String queryText) {
        int currentYear = LocalDate.now().getYear();
        LocalDate holiday = calculateHoliday(rule.getRuleName(), currentYear);

        if (holiday != null) {
            if (queryText.contains("去年") || queryText.contains("上一个")) {
                holiday = calculateHoliday(rule.getRuleName(), currentYear - 1);
            } else if (queryText.contains("明年") || queryText.contains("下一个")) {
                holiday = calculateHoliday(rule.getRuleName(), currentYear + 1);
            }
            return createDateConf(holiday, holiday, queryText, DatePeriodEnum.DAY, 1);
        }

        return null;
    }

    private LocalDate calculateHoliday(String holidayName, int year) {
        switch (holidayName) {
            case "元旦":
                return LocalDate.of(year, 1, 1);
            case "春节":
                return getSpringFestival(year);
            case "清明":
                return LocalDate.of(year, 4, 5);
            case "五一":
                return LocalDate.of(year, 5, 1);
            case "端午":
                return getDragonBoatFestival(year);
            case "中秋":
                return getMidAutumnFestival(year);
            case "国庆":
                return LocalDate.of(year, 10, 1);
            default:
                return null;
        }
    }

    private LocalDate getSpringFestival(int year) {
        int[][] springFestivalData = {{2020, 1, 25}, {2021, 2, 12}, {2022, 2, 1}, {2023, 1, 22},
                        {2024, 2, 10}, {2025, 1, 29}, {2026, 2, 17}, {2027, 2, 6}, {2028, 1, 26},
                        {2029, 2, 13}, {2030, 2, 3}};

        for (int[] data : springFestivalData) {
            if (data[0] == year) {
                return LocalDate.of(data[0], data[1], data[2]);
            }
        }

        return LocalDate.of(year, 2, 1);
    }

    private LocalDate getDragonBoatFestival(int year) {
        int[][] dragonBoatData = {{2020, 6, 25}, {2021, 6, 14}, {2022, 6, 3}, {2023, 6, 22},
                        {2024, 6, 10}, {2025, 5, 31}, {2026, 6, 19}, {2027, 6, 9}, {2028, 5, 28},
                        {2029, 6, 16}, {2030, 6, 5}};

        for (int[] data : dragonBoatData) {
            if (data[0] == year) {
                return LocalDate.of(data[0], data[1], data[2]);
            }
        }

        return LocalDate.of(year, 6, 15);
    }

    private LocalDate getMidAutumnFestival(int year) {
        int[][] midAutumnData = {{2020, 10, 1}, {2021, 9, 21}, {2022, 9, 10}, {2023, 9, 29},
                        {2024, 9, 17}, {2025, 10, 6}, {2026, 9, 25}, {2027, 9, 15}, {2028, 10, 3},
                        {2029, 9, 22}, {2030, 9, 12}};

        for (int[] data : midAutumnData) {
            if (data[0] == year) {
                return LocalDate.of(data[0], data[1], data[2]);
            }
        }

        return LocalDate.of(year, 9, 15);
    }

    private DateConf parseAbsoluteTime(TimeParseRule rule, Matcher matcher, String queryText) {
        return null;
    }

    private DateConf createDateConf(LocalDate startDate, LocalDate endDate, String detectWord,
            DatePeriodEnum period, int unit) {
        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.BETWEEN);
        dateConf.setStartDate(startDate.toString());
        dateConf.setEndDate(endDate.toString());
        dateConf.setPeriod(period);
        dateConf.setUnit(unit);
        dateConf.setDetectWord(detectWord);
        return dateConf;
    }

    private DatePeriodEnum convertPeriod(TimeParseRule.TimeOffset.OffsetType offsetType) {
        switch (offsetType) {
            case DAY:
                return DatePeriodEnum.DAY;
            case WEEK:
                return DatePeriodEnum.WEEK;
            case MONTH:
                return DatePeriodEnum.MONTH;
            case QUARTER:
                return DatePeriodEnum.QUARTER;
            case YEAR:
                return DatePeriodEnum.YEAR;
            default:
                return DatePeriodEnum.DAY;
        }
    }

    @Override
    public String getParserName() {
        return "RuleBasedTimeExpressionParser";
    }

    public void addRule(TimeParseRule rule) {
        rules.add(rule);
    }

    public void removeRule(String ruleName) {
        rules.removeIf(rule -> rule.getRuleName().equals(ruleName));
    }

    public List<TimeParseRule> getRules() {
        return new ArrayList<>(rules);
    }
}
