package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.api.pojo.CoreferenceItem;
import com.tencent.supersonic.chat.api.pojo.EntityChainItem;
import com.tencent.supersonic.chat.api.pojo.FilterStackItem;
import com.tencent.supersonic.chat.server.persistence.repository.ChatContextRepository;
import com.tencent.supersonic.chat.server.pojo.ChatContext;
import com.tencent.supersonic.chat.server.service.ChatContextService;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatContextServiceImpl implements ChatContextService {

    private static final List<String> PRONOUNS = Arrays.asList("它", "他", "她", "它们", "他们", "她们", "那",
            "那个", "那些", "这", "这个", "这些", "该", "该个", "其", "其中", "此");

    private static final List<String> OMISSION_PATTERNS =
            Arrays.asList("再看一下", "看看", "再查一下", "查询一下", "给我", "展示", "显示", "列出");

    private final ChatContextRepository chatContextRepository;

    public ChatContextServiceImpl(ChatContextRepository chatContextRepository) {
        this.chatContextRepository = chatContextRepository;
    }

    @Override
    public ChatContext getOrCreateContext(Integer chatId) {
        return chatContextRepository.getOrCreateContext(chatId);
    }

    @Override
    public void updateContext(ChatContext chatCtx) {
        log.debug("save ChatContext {}", chatCtx);
        chatContextRepository.updateContext(chatCtx);
    }

    @Override
    public ChatContext incrementTurn(Integer chatId, String queryText) {
        ChatContext context = getOrCreateContext(chatId);
        context.setTurnNum(context.getTurnNum() + 1);
        context.setQueryText(queryText);
        updateContext(context);
        return context;
    }

    @Override
    public ChatContext resolveCoreference(Integer chatId, String queryText) {
        ChatContext context = incrementTurn(chatId, queryText);
        List<CoreferenceItem> coreferenceItems = detectCoreferenceItems(queryText, context);

        if (!coreferenceItems.isEmpty()) {
            String rewrittenQuery = rewriteQueryWithCoreference(queryText, coreferenceItems);
            context.setRewrittenText(rewrittenQuery);
            context.setCoreferenceInfo(coreferenceItems);
            log.info("Coreference resolved: original='{}', rewritten='{}'", queryText,
                    rewrittenQuery);
        } else {
            context.setRewrittenText(queryText);
        }

        inheritContextFromHistory(chatId);
        updateContext(context);
        return context;
    }

    @Override
    public ChatContext inheritContextFromHistory(Integer chatId) {
        ChatContext context = getOrCreateContext(chatId);

        List<EntityChainItem> activeEntities = context.getEntityChain().stream()
                .filter(EntityChainItem::isActive).collect(Collectors.toList());

        List<FilterStackItem> inheritedFilters = context.getFilterStack().stream()
                .filter(FilterStackItem::isInherited).collect(Collectors.toList());

        log.debug("Inherited {} active entities and {} filters from history", activeEntities.size(),
                inheritedFilters.size());

        return context;
    }

    @Override
    public List<CoreferenceItem> detectCoreferenceItems(String currentQuery, ChatContext context) {
        List<CoreferenceItem> result = new ArrayList<>();
        int turnNum = context.getTurnNum();

        if (turnNum <= 1 || context.getEntityChain().isEmpty()) {
            return result;
        }

        Map<String, String> pronounResolutions = new HashMap<>();
        List<EntityChainItem> activeEntities = context.getEntityChain().stream()
                .filter(EntityChainItem::isActive).collect(Collectors.toList());

        if (!activeEntities.isEmpty()) {
            EntityChainItem lastEntity = activeEntities.get(activeEntities.size() - 1);
            for (String pronoun : PRONOUNS) {
                Pattern pattern = Pattern.compile(Pattern.quote(pronoun));
                Matcher matcher = pattern.matcher(currentQuery);
                while (matcher.find()) {
                    CoreferenceItem item = CoreferenceItem.builder().originalText(pronoun)
                            .resolvedText(lastEntity.getEntityName()).startPos(matcher.start())
                            .endPos(matcher.end()).type("PRONOUN")
                            .sourceTurn("Turn " + lastEntity.getTurnNum()).build();
                    result.add(item);
                    pronounResolutions.put(pronoun, lastEntity.getEntityName());
                }
            }
        }

        return result;
    }

    @Override
    public String rewriteQueryWithCoreference(String originalQuery,
            List<CoreferenceItem> coreferences) {
        if (coreferences.isEmpty()) {
            return originalQuery;
        }

        coreferences.sort((a, b) -> Integer.compare(b.getStartPos(), a.getStartPos()));

        StringBuilder rewritten = new StringBuilder(originalQuery);
        for (CoreferenceItem item : coreferences) {
            rewritten.replace(item.getStartPos(), item.getEndPos(), item.getResolvedText());
        }

        return rewritten.toString();
    }

    @Override
    public void updateEntityChain(Integer chatId, SemanticParseInfo parseInfo) {
        if (parseInfo == null || parseInfo.getElementMatches() == null) {
            return;
        }

        ChatContext context = getOrCreateContext(chatId);
        List<EntityChainItem> entityChain = new ArrayList<>(context.getEntityChain());
        int currentTurn = context.getTurnNum();

        for (SchemaElementMatch match : parseInfo.getElementMatches()) {
            EntityChainItem item = EntityChainItem.builder().entityName(match.getWord())
                    .entityType(match.getElement().getType().name())
                    .elementId(match.getElement().getId()).turnNum(currentTurn)
                    .queryText(context.getQueryText()).isActive(true).build();
            entityChain.add(item);
        }

        context.setEntityChain(entityChain);
        updateContext(context);
        log.info("Updated entity chain for chat {}: {} entities added", chatId,
                parseInfo.getElementMatches().size());
    }

    @Override
    public void updateFilterStack(Integer chatId, SemanticParseInfo parseInfo) {
        if (parseInfo == null || parseInfo.getDimensionFilters() == null) {
            return;
        }

        ChatContext context = getOrCreateContext(chatId);
        List<FilterStackItem> filterStack = new ArrayList<>(context.getFilterStack());
        int currentTurn = context.getTurnNum();

        parseInfo.getDimensionFilters().forEach(filter -> {
            FilterStackItem item = FilterStackItem.builder().dimensionName(filter.getName())
                    .dimensionBizName(filter.getBizName()).dimensionId(filter.getElementID())
                    .operator(filter.getOperator() != null ? filter.getOperator().name() : "")
                    .value(filter.getValue() != null ? filter.getValue().toString() : "")
                    .turnNum(currentTurn).isInherited(true).build();
            filterStack.add(item);
        });

        context.setFilterStack(filterStack);
        updateContext(context);
        log.info("Updated filter stack for chat {}: {} filters added", chatId,
                parseInfo.getDimensionFilters().size());
    }
}
