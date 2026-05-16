package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class Menu implements Serializable {

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

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private List<Menu> children;
}
