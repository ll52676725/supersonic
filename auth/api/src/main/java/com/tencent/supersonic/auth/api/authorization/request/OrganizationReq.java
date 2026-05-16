package com.tencent.supersonic.auth.api.authorization.request;

import lombok.Data;

import java.util.List;

@Data
public class OrganizationReq {

    private Long id;

    private String orgCode;

    private String orgName;

    private Long parentId;

    private String description;

    private Integer status;

    private Integer sort;

    private List<Long> roleIds;
}
