package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Permission implements Serializable {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String description;

    private String type;

    private Integer status;

    private Integer sort;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;
}
