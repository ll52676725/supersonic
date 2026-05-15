package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.CoreferenceItem;
import com.tencent.supersonic.chat.server.pojo.ChatContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;

import java.util.List;

public interface ChatContextService {

    ChatContext getOrCreateContext(Integer chatId);

    void updateContext(ChatContext chatCtx);

    ChatContext incrementTurn(Integer chatId, String queryText);

    ChatContext resolveCoreference(Integer chatId, String queryText);

    ChatContext inheritContextFromHistory(Integer chatId);

    List<CoreferenceItem> detectCoreferenceItems(String currentQuery, ChatContext context);

    String rewriteQueryWithCoreference(String originalQuery, List<CoreferenceItem> coreferences);

    void updateEntityChain(Integer chatId, SemanticParseInfo parseInfo);

    void updateFilterStack(Integer chatId, SemanticParseInfo parseInfo);
}
