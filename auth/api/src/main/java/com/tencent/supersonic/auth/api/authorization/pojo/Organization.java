package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class Organization implements Serializable {

    private Long id;

    private String orgCode;

    private String orgName;

    private Long parentId;

    private String description;

    private Integer status;

    private Integer sort;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private List<Organization> children;
}
