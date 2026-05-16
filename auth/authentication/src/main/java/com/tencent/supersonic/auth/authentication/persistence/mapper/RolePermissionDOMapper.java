package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.RolePermissionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RolePermissionDOMapper extends BaseMapper<RolePermissionDO> {

    @Select("select permission_id from s2_role_permission where role_id = #{roleId}")
    List<Long> getPermissionIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("delete from s2_role_permission where role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
