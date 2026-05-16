package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.TimeExpressionParseConfig;

public interface TimeExpressionParser {

    DateConf parse(String queryText, TimeExpressionParseConfig config);

    String getParserName();
}
