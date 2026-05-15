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
public class FilterStackItem implements Serializable {

    private String dimensionName;

    private String dimensionBizName;

    private Long dimensionId;

    private String operator;

    private String value;

    private int turnNum;

    private boolean isInherited;
}
