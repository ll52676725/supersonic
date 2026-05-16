package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_role_organization")
public class RoleOrganizationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long organizationId;

    private Date createdAt;

    private String createdBy;
}
