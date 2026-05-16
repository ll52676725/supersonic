package com.tencent.supersonic.auth.authorization.rest;

import com.tencent.supersonic.auth.api.authorization.pojo.Menu;
import com.tencent.supersonic.auth.api.authorization.pojo.Organization;
import com.tencent.supersonic.auth.api.authorization.pojo.Permission;
import com.tencent.supersonic.auth.api.authorization.pojo.Role;
import com.tencent.supersonic.auth.api.authorization.request.MenuReq;
import com.tencent.supersonic.auth.api.authorization.request.OrganizationReq;
import com.tencent.supersonic.auth.api.authorization.request.PermissionReq;
import com.tencent.supersonic.auth.api.authorization.request.RoleReq;
import com.tencent.supersonic.auth.api.authorization.request.UserRoleReq;
import com.tencent.supersonic.auth.api.authorization.service.AuthorizationService;
import com.tencent.supersonic.common.pojo.ResultData;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/roles")
    public ResultData<List<Role>> getAllRoles() {
        return ResultData.success(authorizationService.getAllRoles());
    }

    @GetMapping("/role/{id}")
    public ResultData<Role> getRoleById(@PathVariable Long id) {
        return ResultData.success(authorizationService.getRoleById(id));
    }

    @PostMapping("/role")
    public ResultData<Void> createRole(@RequestBody RoleReq roleReq) {
        authorizationService.createRole(roleReq);
        return ResultData.success(null);
    }

    @PutMapping("/role")
    public ResultData<Void> updateRole(@RequestBody RoleReq roleReq) {
        authorizationService.updateRole(roleReq);
        return ResultData.success(null);
    }

    @DeleteMapping("/role/{id}")
    public ResultData<Void> deleteRole(@PathVariable Long id) {
        authorizationService.deleteRole(id);
        return ResultData.success(null);
    }

    @GetMapping("/permissions")
    public ResultData<List<Permission>> getAllPermissions() {
        return ResultData.success(authorizationService.getAllPermissions());
    }

    @GetMapping("/permission/{id}")
    public ResultData<Permission> getPermissionById(@PathVariable Long id) {
        return ResultData.success(authorizationService.getPermissionById(id));
    }

    @PostMapping("/permission")
    public ResultData<Void> createPermission(@RequestBody PermissionReq permissionReq) {
        authorizationService.createPermission(permissionReq);
        return ResultData.success(null);
    }

    @PutMapping("/permission")
    public ResultData<Void> updatePermission(@RequestBody PermissionReq permissionReq) {
        authorizationService.updatePermission(permissionReq);
        return ResultData.success(null);
    }

    @DeleteMapping("/permission/{id}")
    public ResultData<Void> deletePermission(@PathVariable Long id) {
        authorizationService.deletePermission(id);
        return ResultData.success(null);
    }

    @GetMapping("/menus")
    public ResultData<List<Menu>> getAllMenus() {
        return ResultData.success(authorizationService.getAllMenus());
    }

    @GetMapping("/menu/tree")
    public ResultData<List<Menu>> getMenuTree() {
        return ResultData.success(authorizationService.getMenuTree());
    }

    @GetMapping("/menu/{id}")
    public ResultData<Menu> getMenuById(@PathVariable Long id) {
        return ResultData.success(authorizationService.getMenuById(id));
    }

    @PostMapping("/menu")
    public ResultData<Void> createMenu(@RequestBody MenuReq menuReq) {
        authorizationService.createMenu(menuReq);
        return ResultData.success(null);
    }

    @PutMapping("/menu")
    public ResultData<Void> updateMenu(@RequestBody MenuReq menuReq) {
        authorizationService.updateMenu(menuReq);
        return ResultData.success(null);
    }

    @DeleteMapping("/menu/{id}")
    public ResultData<Void> deleteMenu(@PathVariable Long id) {
        authorizationService.deleteMenu(id);
        return ResultData.success(null);
    }

    @GetMapping("/role/{roleId}/permissions")
    public ResultData<List<Long>> getRolePermissionIds(@PathVariable Long roleId) {
        return ResultData.success(authorizationService.getRolePermissionIds(roleId));
    }

    @GetMapping("/role/{roleId}/menus")
    public ResultData<List<Long>> getRoleMenuIds(@PathVariable Long roleId) {
        return ResultData.success(authorizationService.getRoleMenuIds(roleId));
    }

    @GetMapping("/user/{userId}/roles")
    public ResultData<List<Role>> getUserRoles(@PathVariable Long userId) {
        return ResultData.success(authorizationService.getUserRoles(userId));
    }

    @GetMapping("/user/{userId}/menus")
    public ResultData<List<Menu>> getUserMenus(@PathVariable Long userId) {
        return ResultData.success(authorizationService.getUserMenus(userId));
    }

    @GetMapping("/user/{userId}/permissions")
    public ResultData<List<Permission>> getUserPermissions(@PathVariable Long userId) {
        return ResultData.success(authorizationService.getUserPermissions(userId));
    }

    @PostMapping("/user/roles")
    public ResultData<Void> assignRolesToUser(@RequestBody UserRoleReq userRoleReq) {
        authorizationService.assignRolesToUser(userRoleReq);
        return ResultData.success(null);
    }

    @GetMapping("/organizations")
    public ResultData<List<Organization>> getAllOrganizations() {
        return ResultData.success(authorizationService.getAllOrganizations());
    }

    @GetMapping("/organization/tree")
    public ResultData<List<Organization>> getOrganizationTree() {
        return ResultData.success(authorizationService.getOrganizationTree());
    }

    @GetMapping("/organization/{id}")
    public ResultData<Organization> getOrganizationById(@PathVariable Long id) {
        return ResultData.success(authorizationService.getOrganizationById(id));
    }

    @PostMapping("/organization")
    public ResultData<Void> createOrganization(@RequestBody OrganizationReq organizationReq) {
        authorizationService.createOrganization(organizationReq);
        return ResultData.success(null);
    }

    @PutMapping("/organization")
    public ResultData<Void> updateOrganization(@RequestBody OrganizationReq organizationReq) {
        authorizationService.updateOrganization(organizationReq);
        return ResultData.success(null);
    }

    @DeleteMapping("/organization/{id}")
    public ResultData<Void> deleteOrganization(@PathVariable Long id) {
        authorizationService.deleteOrganization(id);
        return ResultData.success(null);
    }

    @GetMapping("/role/{roleId}/organizations")
    public ResultData<List<Long>> getRoleOrganizationIds(@PathVariable Long roleId) {
        return ResultData.success(authorizationService.getRoleOrganizationIds(roleId));
    }

    @GetMapping("/organization/{organizationId}/roles")
    public ResultData<List<Role>> getOrganizationRoles(@PathVariable Long organizationId) {
        return ResultData.success(authorizationService.getOrganizationRoles(organizationId));
    }

    @PostMapping("/organization/roles")
    public ResultData<Void> assignRolesToOrganization(@RequestBody OrganizationReq organizationReq) {
        authorizationService.assignRolesToOrganization(organizationReq);
        return ResultData.success(null);
    }
}
