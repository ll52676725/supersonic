package com.tencent.supersonic.chat.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntityChainItem implements Serializable {

    private String entityName;

    private String entityType;

    private Long elementId;

    private int turnNum;

    private String queryText;

    private boolean isActive;
}
