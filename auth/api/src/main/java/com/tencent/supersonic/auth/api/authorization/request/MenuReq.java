package com.tencent.supersonic.auth.api.authorization.request;

import lombok.Data;

@Data
public class MenuReq {

    private Long id;

    private String menuCode;

    private String menuName;

    private Long parentId;

    private String path;

    private String icon;

    private String component;

    private String description;

    private String type;

    private Integer status;

    private Integer sort;
}
