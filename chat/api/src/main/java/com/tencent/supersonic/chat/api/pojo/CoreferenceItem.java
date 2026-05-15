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
public class CoreferenceItem implements Serializable {

    private String originalText;

    private String resolvedText;

    private int startPos;

    private int endPos;

    private String type;

    private String sourceTurn;
}
