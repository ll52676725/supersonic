package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserRoleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleDOMapper extends BaseMapper<UserRoleDO> {

    @Select("select role_id from s2_user_role where user_id = #{userId}")
    List<Long> getRoleIdsByUserId(@Param("userId") Long userId);

    @Delete("delete from s2_user_role where user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
