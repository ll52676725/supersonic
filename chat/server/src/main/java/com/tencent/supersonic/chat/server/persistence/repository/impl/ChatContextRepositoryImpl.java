package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.supersonic.chat.api.pojo.CoreferenceItem;
import com.tencent.supersonic.chat.api.pojo.EntityChainItem;
import com.tencent.supersonic.chat.api.pojo.FilterStackItem;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatContextDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatContextMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatContextRepository;
import com.tencent.supersonic.chat.server.pojo.ChatContext;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Repository
@Primary
@Slf4j
public class ChatContextRepositoryImpl implements ChatContextRepository {

    private final ChatContextMapper chatContextMapper;
    private final Gson gson = new Gson();

    public ChatContextRepositoryImpl(ChatContextMapper chatContextMapper) {
        this.chatContextMapper = chatContextMapper;
    }

    @Override
    public ChatContext getOrCreateContext(Integer chatId) {
        ChatContextDO context = chatContextMapper.getContextByChatId(chatId);
        if (context == null) {
            ChatContext chatContext = new ChatContext();
            chatContext.setChatId(chatId);
            return chatContext;
        }
        return cast(context);
    }

    @Override
    public void updateContext(ChatContext chatCtx) {
        chatContextMapper.insertOrUpdate(cast(chatCtx));
    }

    private ChatContext cast(ChatContextDO contextDO) {
        ChatContext chatContext = new ChatContext();
        chatContext.setChatId(contextDO.getChatId());
        chatContext.setTurnNum(contextDO.getTurnNum());
        chatContext.setUser(contextDO.getQueryUser());
        chatContext.setQueryText(contextDO.getQueryText());
        chatContext.setRewrittenText(contextDO.getRewrittenText());
        chatContext.setExtData(contextDO.getExtData());

        if (contextDO.getSemanticParse() != null && !contextDO.getSemanticParse().isEmpty()) {
            SemanticParseInfo semanticParseInfo =
                    JsonUtil.toObject(contextDO.getSemanticParse(), SemanticParseInfo.class);
            chatContext.setParseInfo(semanticParseInfo);
        }

        Type listType = new TypeToken<List<EntityChainItem>>() {}.getType();
        if (contextDO.getEntityChain() != null && !contextDO.getEntityChain().isEmpty()) {
            List<EntityChainItem> entityChain = gson.fromJson(contextDO.getEntityChain(), listType);
            chatContext.setEntityChain(entityChain);
        }

        Type filterListType = new TypeToken<List<FilterStackItem>>() {}.getType();
        if (contextDO.getFilterStack() != null && !contextDO.getFilterStack().isEmpty()) {
            List<FilterStackItem> filterStack =
                    gson.fromJson(contextDO.getFilterStack(), filterListType);
            chatContext.setFilterStack(filterStack);
        }

        Type corefListType = new TypeToken<List<CoreferenceItem>>() {}.getType();
        if (contextDO.getCoreferenceInfo() != null && !contextDO.getCoreferenceInfo().isEmpty()) {
            List<CoreferenceItem> coreferenceInfo =
                    gson.fromJson(contextDO.getCoreferenceInfo(), corefListType);
            chatContext.setCoreferenceInfo(coreferenceInfo);
        }

        return chatContext;
    }

    private ChatContextDO cast(ChatContext chatContext) {
        ChatContextDO chatContextDO = new ChatContextDO();
        chatContextDO.setChatId(chatContext.getChatId());
        chatContextDO.setTurnNum(chatContext.getTurnNum());
        chatContextDO.setQueryText(chatContext.getQueryText());
        chatContextDO.setRewrittenText(chatContext.getRewrittenText());
        chatContextDO.setQueryUser(chatContext.getUser());
        chatContextDO.setExtData(chatContext.getExtData());

        if (chatContext.getParseInfo() != null) {
            chatContextDO.setSemanticParse(gson.toJson(chatContext.getParseInfo()));
        }
        if (chatContext.getEntityChain() != null && !chatContext.getEntityChain().isEmpty()) {
            chatContextDO.setEntityChain(gson.toJson(chatContext.getEntityChain()));
        }
        if (chatContext.getFilterStack() != null && !chatContext.getFilterStack().isEmpty()) {
            chatContextDO.setFilterStack(gson.toJson(chatContext.getFilterStack()));
        }
        if (chatContext.getCoreferenceInfo() != null
                && !chatContext.getCoreferenceInfo().isEmpty()) {
            chatContextDO.setCoreferenceInfo(gson.toJson(chatContext.getCoreferenceInfo()));
        }
        return chatContextDO;
    }
}
