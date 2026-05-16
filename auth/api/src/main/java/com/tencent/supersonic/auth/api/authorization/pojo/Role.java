package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Role implements Serializable {

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private Integer sort;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;
}
