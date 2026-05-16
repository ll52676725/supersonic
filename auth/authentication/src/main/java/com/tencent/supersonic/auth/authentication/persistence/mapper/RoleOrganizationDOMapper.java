package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.RoleOrganizationDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleOrganizationDOMapper extends BaseMapper<RoleOrganizationDO> {

    @Select("select organization_id from s2_role_organization where role_id = #{roleId}")
    List<Long> getOrganizationIdsByRoleId(@Param("roleId") Long roleId);

    @Select("select role_id from s2_role_organization where organization_id = #{organizationId}")
    List<Long> getRoleIdsByOrganizationId(@Param("organizationId") Long organizationId);

    @Delete("delete from s2_role_organization where role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("delete from s2_role_organization where organization_id = #{organizationId}")
    void deleteByOrganizationId(@Param("organizationId") Long organizationId);
}
