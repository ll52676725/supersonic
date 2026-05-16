package com.tencent.supersonic.auth.authorization.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
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
import com.tencent.supersonic.auth.authentication.persistence.dataobject.*;
import com.tencent.supersonic.auth.authentication.persistence.mapper.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private final RoleDOMapper roleDOMapper;
    private final PermissionDOMapper permissionDOMapper;
    private final MenuDOMapper menuDOMapper;
    private final OrganizationDOMapper organizationDOMapper;
    private final RolePermissionDOMapper rolePermissionDOMapper;
    private final RoleMenuDOMapper roleMenuDOMapper;
    private final UserRoleDOMapper userRoleDOMapper;
    private final RoleOrganizationDOMapper roleOrganizationDOMapper;

    public AuthorizationServiceImpl(RoleDOMapper roleDOMapper,
                                    PermissionDOMapper permissionDOMapper,
                                    MenuDOMapper menuDOMapper,
                                    OrganizationDOMapper organizationDOMapper,
                                    RolePermissionDOMapper rolePermissionDOMapper,
                                    RoleMenuDOMapper roleMenuDOMapper,
                                    UserRoleDOMapper userRoleDOMapper,
                                    RoleOrganizationDOMapper roleOrganizationDOMapper) {
        this.roleDOMapper = roleDOMapper;
        this.permissionDOMapper = permissionDOMapper;
        this.menuDOMapper = menuDOMapper;
        this.organizationDOMapper = organizationDOMapper;
        this.rolePermissionDOMapper = rolePermissionDOMapper;
        this.roleMenuDOMapper = roleMenuDOMapper;
        this.userRoleDOMapper = userRoleDOMapper;
        this.roleOrganizationDOMapper = roleOrganizationDOMapper;
    }

    @Override
    public List<Role> getAllRoles() {
        List<RoleDO> roleDOList = roleDOMapper.selectList(
                new QueryWrapper<RoleDO>().orderByAsc("sort"));
        return roleDOList.stream().map(this::convertToRole).collect(Collectors.toList());
    }

    @Override
    public Role getRoleById(Long id) {
        RoleDO roleDO = roleDOMapper.selectById(id);
        return roleDO != null ? convertToRole(roleDO) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleReq roleReq) {
        String currentUser = getCurrentUser();
        RoleDO roleDO = new RoleDO();
        BeanUtils.copyProperties(roleReq, roleDO);
        roleDO.setCreatedAt(new Date());
        roleDO.setCreatedBy(currentUser);
        roleDO.setStatus(roleReq.getStatus() != null ? roleReq.getStatus() : 1);
        roleDOMapper.insert(roleDO);

        if (!CollectionUtils.isEmpty(roleReq.getPermissionIds())) {
            for (Long permissionId : roleReq.getPermissionIds()) {
                RolePermissionDO rolePermissionDO = new RolePermissionDO();
                rolePermissionDO.setRoleId(roleDO.getId());
                rolePermissionDO.setPermissionId(permissionId);
                rolePermissionDO.setCreatedAt(new Date());
                rolePermissionDO.setCreatedBy(currentUser);
                rolePermissionDOMapper.insert(rolePermissionDO);
            }
        }

        if (!CollectionUtils.isEmpty(roleReq.getMenuIds())) {
            for (Long menuId : roleReq.getMenuIds()) {
                RoleMenuDO roleMenuDO = new RoleMenuDO();
                roleMenuDO.setRoleId(roleDO.getId());
                roleMenuDO.setMenuId(menuId);
                roleMenuDO.setCreatedAt(new Date());
                roleMenuDO.setCreatedBy(currentUser);
                roleMenuDOMapper.insert(roleMenuDO);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleReq roleReq) {
        String currentUser = getCurrentUser();
        RoleDO roleDO = roleDOMapper.selectById(roleReq.getId());
        if (roleDO == null) {
            return;
        }
        BeanUtils.copyProperties(roleReq, roleDO);
        roleDO.setUpdatedAt(new Date());
        roleDO.setUpdatedBy(currentUser);
        roleDOMapper.updateById(roleDO);

        if (roleReq.getPermissionIds() != null) {
            rolePermissionDOMapper.deleteByRoleId(roleReq.getId());
            for (Long permissionId : roleReq.getPermissionIds()) {
                RolePermissionDO rolePermissionDO = new RolePermissionDO();
                rolePermissionDO.setRoleId(roleReq.getId());
                rolePermissionDO.setPermissionId(permissionId);
                rolePermissionDO.setCreatedAt(new Date());
                rolePermissionDO.setCreatedBy(currentUser);
                rolePermissionDOMapper.insert(rolePermissionDO);
            }
        }

        if (roleReq.getMenuIds() != null) {
            roleMenuDOMapper.deleteByRoleId(roleReq.getId());
            for (Long menuId : roleReq.getMenuIds()) {
                RoleMenuDO roleMenuDO = new RoleMenuDO();
                roleMenuDO.setRoleId(roleReq.getId());
                roleMenuDO.setMenuId(menuId);
                roleMenuDO.setCreatedAt(new Date());
                roleMenuDO.setCreatedBy(currentUser);
                roleMenuDOMapper.insert(roleMenuDO);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        roleDOMapper.deleteById(id);
        rolePermissionDOMapper.deleteByRoleId(id);
        roleMenuDOMapper.deleteByRoleId(id);
    }

    @Override
    public List<Permission> getAllPermissions() {
        List<PermissionDO> permissionDOList = permissionDOMapper.selectList(
                new QueryWrapper<PermissionDO>().orderByAsc("sort"));
        return permissionDOList.stream().map(this::convertToPermission).collect(Collectors.toList());
    }

    @Override
    public Permission getPermissionById(Long id) {
        PermissionDO permissionDO = permissionDOMapper.selectById(id);
        return permissionDO != null ? convertToPermission(permissionDO) : null;
    }

    @Override
    public void createPermission(PermissionReq permissionReq) {
        String currentUser = getCurrentUser();
        PermissionDO permissionDO = new PermissionDO();
        BeanUtils.copyProperties(permissionReq, permissionDO);
        permissionDO.setCreatedAt(new Date());
        permissionDO.setCreatedBy(currentUser);
        permissionDO.setStatus(permissionReq.getStatus() != null ? permissionReq.getStatus() : 1);
        permissionDOMapper.insert(permissionDO);
    }

    @Override
    public void updatePermission(PermissionReq permissionReq) {
        String currentUser = getCurrentUser();
        PermissionDO permissionDO = permissionDOMapper.selectById(permissionReq.getId());
        if (permissionDO == null) {
            return;
        }
        BeanUtils.copyProperties(permissionReq, permissionDO);
        permissionDO.setUpdatedAt(new Date());
        permissionDO.setUpdatedBy(currentUser);
        permissionDOMapper.updateById(permissionDO);
    }

    @Override
    public void deletePermission(Long id) {
        permissionDOMapper.deleteById(id);
    }

    @Override
    public List<Menu> getAllMenus() {
        List<MenuDO> menuDOList = menuDOMapper.selectList(
                new QueryWrapper<MenuDO>().orderByAsc("sort"));
        return menuDOList.stream().map(this::convertToMenu).collect(Collectors.toList());
    }

    @Override
    public List<Menu> getMenuTree() {
        List<Menu> allMenus = getAllMenus();
        return buildTree(allMenus);
    }

    @Override
    public Menu getMenuById(Long id) {
        MenuDO menuDO = menuDOMapper.selectById(id);
        return menuDO != null ? convertToMenu(menuDO) : null;
    }

    @Override
    public void createMenu(MenuReq menuReq) {
        String currentUser = getCurrentUser();
        MenuDO menuDO = new MenuDO();
        BeanUtils.copyProperties(menuReq, menuDO);
        menuDO.setCreatedAt(new Date());
        menuDO.setCreatedBy(currentUser);
        menuDO.setStatus(menuReq.getStatus() != null ? menuReq.getStatus() : 1);
        menuDOMapper.insert(menuDO);
    }

    @Override
    public void updateMenu(MenuReq menuReq) {
        String currentUser = getCurrentUser();
        MenuDO menuDO = menuDOMapper.selectById(menuReq.getId());
        if (menuDO == null) {
            return;
        }
        BeanUtils.copyProperties(menuReq, menuDO);
        menuDO.setUpdatedAt(new Date());
        menuDO.setUpdatedBy(currentUser);
        menuDOMapper.updateById(menuDO);
    }

    @Override
    public void deleteMenu(Long id) {
        menuDOMapper.deleteById(id);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionDOMapper.getPermissionIdsByRoleId(roleId);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuDOMapper.getMenuIdsByRoleId(roleId);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleDOMapper.getRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToUser(UserRoleReq userRoleReq) {
        String currentUser = getCurrentUser();
        userRoleDOMapper.deleteByUserId(userRoleReq.getUserId());
        if (!CollectionUtils.isEmpty(userRoleReq.getRoleIds())) {
            for (Long roleId : userRoleReq.getRoleIds()) {
                UserRoleDO userRoleDO = new UserRoleDO();
                userRoleDO.setUserId(userRoleReq.getUserId());
                userRoleDO.setRoleId(roleId);
                userRoleDO.setCreatedAt(new Date());
                userRoleDO.setCreatedBy(currentUser);
                userRoleDOMapper.insert(userRoleDO);
            }
        }
    }

    @Override
    public List<Role> getUserRoles(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<RoleDO> roleDOList = roleDOMapper.selectBatchIds(roleIds);
        return roleDOList.stream().map(this::convertToRole).collect(Collectors.toList());
    }

    @Override
    public List<Menu> getUserMenus(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        Set<Long> menuIdSet = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> menuIds = getRoleMenuIds(roleId);
            if (!CollectionUtils.isEmpty(menuIds)) {
                menuIdSet.addAll(menuIds);
            }
        }
        if (menuIdSet.isEmpty()) {
            return Collections.emptyList();
        }
        List<MenuDO> menuDOList = menuDOMapper.selectBatchIds(menuIdSet);
        List<Menu> menus = menuDOList.stream()
                .filter(m -> m.getStatus() == 1)
                .map(this::convertToMenu)
                .sorted(Comparator.comparing(Menu::getSort))
                .collect(Collectors.toList());
        return buildTree(menus);
    }

    @Override
    public List<Permission> getUserPermissions(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        Set<Long> permissionIdSet = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> permissionIds = getRolePermissionIds(roleId);
            if (!CollectionUtils.isEmpty(permissionIds)) {
                permissionIdSet.addAll(permissionIds);
            }
        }
        if (permissionIdSet.isEmpty()) {
            return Collections.emptyList();
        }
        List<PermissionDO> permissionDOList = permissionDOMapper.selectBatchIds(permissionIdSet);
        return permissionDOList.stream()
                .filter(p -> p.getStatus() == 1)
                .map(this::convertToPermission)
                .collect(Collectors.toList());
    }

    @Override
    public List<Organization> getAllOrganizations() {
        List<OrganizationDO> organizationDOList = organizationDOMapper.selectList(
                new QueryWrapper<OrganizationDO>().orderByAsc("sort"));
        return organizationDOList.stream().map(this::convertToOrganization).collect(Collectors.toList());
    }

    @Override
    public List<Organization> getOrganizationTree() {
        List<Organization> allOrganizations = getAllOrganizations();
        return buildOrganizationTree(allOrganizations);
    }

    @Override
    public Organization getOrganizationById(Long id) {
        OrganizationDO organizationDO = organizationDOMapper.selectById(id);
        return organizationDO != null ? convertToOrganization(organizationDO) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrganization(OrganizationReq organizationReq) {
        String currentUser = getCurrentUser();
        OrganizationDO organizationDO = new OrganizationDO();
        BeanUtils.copyProperties(organizationReq, organizationDO);
        organizationDO.setCreatedAt(new Date());
        organizationDO.setCreatedBy(currentUser);
        organizationDO.setStatus(organizationReq.getStatus() != null ? organizationReq.getStatus() : 1);
        organizationDOMapper.insert(organizationDO);

        if (!CollectionUtils.isEmpty(organizationReq.getRoleIds())) {
            for (Long roleId : organizationReq.getRoleIds()) {
                RoleOrganizationDO roleOrganizationDO = new RoleOrganizationDO();
                roleOrganizationDO.setRoleId(roleId);
                roleOrganizationDO.setOrganizationId(organizationDO.getId());
                roleOrganizationDO.setCreatedAt(new Date());
                roleOrganizationDO.setCreatedBy(currentUser);
                roleOrganizationDOMapper.insert(roleOrganizationDO);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrganization(OrganizationReq organizationReq) {
        String currentUser = getCurrentUser();
        OrganizationDO organizationDO = organizationDOMapper.selectById(organizationReq.getId());
        if (organizationDO == null) {
            return;
        }
        BeanUtils.copyProperties(organizationReq, organizationDO);
        organizationDO.setUpdatedAt(new Date());
        organizationDO.setUpdatedBy(currentUser);
        organizationDOMapper.updateById(organizationDO);

        if (organizationReq.getRoleIds() != null) {
            roleOrganizationDOMapper.deleteByOrganizationId(organizationReq.getId());
            for (Long roleId : organizationReq.getRoleIds()) {
                RoleOrganizationDO roleOrganizationDO = new RoleOrganizationDO();
                roleOrganizationDO.setRoleId(roleId);
                roleOrganizationDO.setOrganizationId(organizationReq.getId());
                roleOrganizationDO.setCreatedAt(new Date());
                roleOrganizationDO.setCreatedBy(currentUser);
                roleOrganizationDOMapper.insert(roleOrganizationDO);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrganization(Long id) {
        organizationDOMapper.deleteById(id);
        roleOrganizationDOMapper.deleteByOrganizationId(id);
    }

    @Override
    public List<Long> getRoleOrganizationIds(Long roleId) {
        return roleOrganizationDOMapper.getOrganizationIdsByRoleId(roleId);
    }

    @Override
    public List<Long> getOrganizationRoleIds(Long organizationId) {
        return roleOrganizationDOMapper.getRoleIdsByOrganizationId(organizationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToOrganization(OrganizationReq organizationReq) {
        String currentUser = getCurrentUser();
        roleOrganizationDOMapper.deleteByOrganizationId(organizationReq.getId());
        if (!CollectionUtils.isEmpty(organizationReq.getRoleIds())) {
            for (Long roleId : organizationReq.getRoleIds()) {
                RoleOrganizationDO roleOrganizationDO = new RoleOrganizationDO();
                roleOrganizationDO.setRoleId(roleId);
                roleOrganizationDO.setOrganizationId(organizationReq.getId());
                roleOrganizationDO.setCreatedAt(new Date());
                roleOrganizationDO.setCreatedBy(currentUser);
                roleOrganizationDOMapper.insert(roleOrganizationDO);
            }
        }
    }

    @Override
    public List<Role> getOrganizationRoles(Long organizationId) {
        List<Long> roleIds = getOrganizationRoleIds(organizationId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<RoleDO> roleDOList = roleDOMapper.selectBatchIds(roleIds);
        return roleDOList.stream().map(this::convertToRole).collect(Collectors.toList());
    }

    private List<Organization> buildOrganizationTree(List<Organization> organizations) {
        if (CollectionUtils.isEmpty(organizations)) {
            return Collections.emptyList();
        }
        Map<Long, List<Organization>> parentChildrenMap = organizations.stream()
                .collect(Collectors.groupingBy(org -> org.getParentId() != null ? org.getParentId() : 0L));
        List<Organization> rootOrganizations = parentChildrenMap.getOrDefault(0L, Collections.emptyList());
        for (Organization organization : rootOrganizations) {
            setOrganizationChildren(organization, parentChildrenMap);
        }
        return rootOrganizations.stream()
                .sorted(Comparator.comparing(Organization::getSort))
                .collect(Collectors.toList());
    }

    private void setOrganizationChildren(Organization parent, Map<Long, List<Organization>> parentChildrenMap) {
        List<Organization> children = parentChildrenMap.getOrDefault(parent.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            children = children.stream()
                    .sorted(Comparator.comparing(Organization::getSort))
                    .collect(Collectors.toList());
            parent.setChildren(children);
            for (Organization child : children) {
                setOrganizationChildren(child, parentChildrenMap);
            }
        }
    }

    private Organization convertToOrganization(OrganizationDO organizationDO) {
        Organization organization = new Organization();
        BeanUtils.copyProperties(organizationDO, organization);
        return organization;
    }

    private List<Menu> buildTree(List<Menu> menus) {
        if (CollectionUtils.isEmpty(menus)) {
            return Collections.emptyList();
        }
        Map<Long, List<Menu>> parentChildrenMap = menus.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() != null ? menu.getParentId() : 0L));
        List<Menu> rootMenus = parentChildrenMap.getOrDefault(0L, Collections.emptyList());
        for (Menu menu : rootMenus) {
            setChildren(menu, parentChildrenMap);
        }
        return rootMenus.stream()
                .sorted(Comparator.comparing(Menu::getSort))
                .collect(Collectors.toList());
    }

    private void setChildren(Menu parent, Map<Long, List<Menu>> parentChildrenMap) {
        List<Menu> children = parentChildrenMap.getOrDefault(parent.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            children = children.stream()
                    .sorted(Comparator.comparing(Menu::getSort))
                    .collect(Collectors.toList());
            parent.setChildren(children);
            for (Menu child : children) {
                setChildren(child, parentChildrenMap);
            }
        }
    }

    private Role convertToRole(RoleDO roleDO) {
        Role role = new Role();
        BeanUtils.copyProperties(roleDO, role);
        return role;
    }

    private Permission convertToPermission(PermissionDO permissionDO) {
        Permission permission = new Permission();
        BeanUtils.copyProperties(permissionDO, permission);
        return permission;
    }

    private Menu convertToMenu(MenuDO menuDO) {
        Menu menu = new Menu();
        BeanUtils.copyProperties(menuDO, menu);
        return menu;
    }

    private String getCurrentUser() {
        try {
            if (UserHolder.get() != null) {
                return UserHolder.get().getName();
            }
        } catch (Exception e) {
        }
        return "system";
    }
}
