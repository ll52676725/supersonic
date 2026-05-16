package com.tencent.supersonic.auth.api.authorization.request;

import lombok.Data;

@Data
public class PermissionReq {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String description;

    private String type;

    private Integer status;

    private Integer sort;
}
