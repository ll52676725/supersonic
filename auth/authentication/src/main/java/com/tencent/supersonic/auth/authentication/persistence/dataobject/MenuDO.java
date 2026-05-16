package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_menu")
public class MenuDO {

    @TableId(type = IdType.AUTO)
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
}
