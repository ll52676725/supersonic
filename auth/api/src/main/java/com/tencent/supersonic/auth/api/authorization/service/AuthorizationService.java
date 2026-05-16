package com.tencent.supersonic.auth.api.authorization.service;

import com.tencent.supersonic.auth.api.authorization.pojo.Menu;
import com.tencent.supersonic.auth.api.authorization.pojo.Organization;
import com.tencent.supersonic.auth.api.authorization.pojo.Permission;
import com.tencent.supersonic.auth.api.authorization.pojo.Role;
import com.tencent.supersonic.auth.api.authorization.request.MenuReq;
import com.tencent.supersonic.auth.api.authorization.request.OrganizationReq;
import com.tencent.supersonic.auth.api.authorization.request.PermissionReq;
import com.tencent.supersonic.auth.api.authorization.request.RoleReq;
import com.tencent.supersonic.auth.api.authorization.request.UserRoleReq;

import java.util.List;

public interface AuthorizationService {

    List<Role> getAllRoles();

    Role getRoleById(Long id);

    void createRole(RoleReq roleReq);

    void updateRole(RoleReq roleReq);

    void deleteRole(Long id);

    List<Permission> getAllPermissions();

    Permission getPermissionById(Long id);

    void createPermission(PermissionReq permissionReq);

    void updatePermission(PermissionReq permissionReq);

    void deletePermission(Long id);

    List<Menu> getAllMenus();

    List<Menu> getMenuTree();

    Menu getMenuById(Long id);

    void createMenu(MenuReq menuReq);

    void updateMenu(MenuReq menuReq);

    void deleteMenu(Long id);

    List<Long> getRolePermissionIds(Long roleId);

    List<Long> getRoleMenuIds(Long roleId);

    List<Long> getUserRoleIds(Long userId);

    void assignRolesToUser(UserRoleReq userRoleReq);

    List<Role> getUserRoles(Long userId);

    List<Menu> getUserMenus(Long userId);

    List<Permission> getUserPermissions(Long userId);

    List<Organization> getAllOrganizations();

    List<Organization> getOrganizationTree();

    Organization getOrganizationById(Long id);

    void createOrganization(OrganizationReq organizationReq);

    void updateOrganization(OrganizationReq organizationReq);

    void deleteOrganization(Long id);

    List<Long> getRoleOrganizationIds(Long roleId);

    List<Long> getOrganizationRoleIds(Long organizationId);

    void assignRolesToOrganization(OrganizationReq organizationReq);

    List<Role> getOrganizationRoles(Long organizationId);
}
