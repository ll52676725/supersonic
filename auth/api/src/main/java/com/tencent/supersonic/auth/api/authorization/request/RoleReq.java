package com.tencent.supersonic.auth.api.authorization.request;

import lombok.Data;

import java.util.List;

@Data
public class RoleReq {

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private Integer sort;

    private List<Long> permissionIds;

    private List<Long> menuIds;
}
