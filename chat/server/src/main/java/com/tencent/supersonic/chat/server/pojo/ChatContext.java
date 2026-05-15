package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.CoreferenceItem;
import com.tencent.supersonic.chat.api.pojo.EntityChainItem;
import com.tencent.supersonic.chat.api.pojo.FilterStackItem;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatContext {
    private Integer chatId;
    private Integer turnNum = 0;
    private String queryText;
    private String rewrittenText;
    private SemanticParseInfo parseInfo = new SemanticParseInfo();
    private String user;
    private List<EntityChainItem> entityChain = new ArrayList<>();
    private List<FilterStackItem> filterStack = new ArrayList<>();
    private List<CoreferenceItem> coreferenceInfo = new ArrayList<>();
    private String extData;
}
