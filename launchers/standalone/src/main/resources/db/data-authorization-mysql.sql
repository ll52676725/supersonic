-- 权限系统初始化数据

-- 插入默认角色
INSERT INTO `s2_role` (`id`, `role_code`, `role_name`, `description`, `status`, `sort`, `created_at`, `created_by`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '系统超级管理员，拥有所有权限', 1, 1, NOW(), 'system'),
(2, 'ADMIN', '管理员', '系统管理员', 1, 2, NOW(), 'system'),
(3, 'USER', '普通用户', '普通用户角色', 1, 3, NOW(), 'system');

-- 插入默认权限
INSERT INTO `s2_permission` (`id`, `permission_code`, `permission_name`, `description`, `type`, `status`, `sort`, `created_at`, `created_by`) VALUES
(1, 'user:view', '查看用户', '查看用户列表和详情', 'user', 1, 1, NOW(), 'system'),
(2, 'user:create', '创建用户', '创建新用户', 'user', 1, 2, NOW(), 'system'),
(3, 'user:update', '编辑用户', '编辑用户信息', 'user', 1, 3, NOW(), 'system'),
(4, 'user:delete', '删除用户', '删除用户', 'user', 1, 4, NOW(), 'system'),
(5, 'role:view', '查看角色', '查看角色列表和详情', 'role', 1, 5, NOW(), 'system'),
(6, 'role:create', '创建角色', '创建新角色', 'role', 1, 6, NOW(), 'system'),
(7, 'role:update', '编辑角色', '编辑角色信息和权限', 'role', 1, 7, NOW(), 'system'),
(8, 'role:delete', '删除角色', '删除角色', 'role', 1, 8, NOW(), 'system'),
(9, 'menu:view', '查看菜单', '查看菜单列表', 'menu', 1, 9, NOW(), 'system'),
(10, 'menu:create', '创建菜单', '创建新菜单', 'menu', 1, 10, NOW(), 'system'),
(11, 'menu:update', '编辑菜单', '编辑菜单信息', 'menu', 1, 11, NOW(), 'system'),
(12, 'menu:delete', '删除菜单', '删除菜单', 'menu', 1, 12, NOW(), 'system');

-- 插入默认菜单（一级菜单）
INSERT INTO `s2_menu` (`id`, `menu_code`, `menu_name`, `parent_id`, `path`, `icon`, `component`, `description`, `type`, `status`, `sort`, `created_at`, `created_by`) VALUES
(1, 'dashboard', '数据看板', 0, '/dashboard', 'dashboard', 'Dashboard', '数据看板', 'menu', 1, 1, NOW(), 'system'),
(2, 'dataModel', '数据模型', 0, '/dataModel', 'model', 'DataModel', '数据模型管理', 'menu', 1, 2, NOW(), 'system'),
(3, 'chat', '智能问答', 0, '/chat', 'chat', 'Chat', '智能问答', 'menu', 1, 3, NOW(), 'system'),
(4, 'system', '系统管理', 0, '/system', 'setting', 'System', '系统管理', 'directory', 1, 4, NOW(), 'system'),
(5, 'userManage', '用户管理', 4, '/system/user', 'user', 'UserManage', '用户管理', 'menu', 1, 1, NOW(), 'system'),
(6, 'roleManage', '角色管理', 4, '/system/role', 'team', 'RoleManage', '角色权限管理', 'menu', 1, 2, NOW(), 'system'),
(7, 'menuManage', '菜单管理', 4, '/system/menu', 'menu', 'MenuManage', '菜单管理', 'menu', 1, 3, NOW(), 'system');

-- 给超级管理员角色分配所有权限
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`, `created_at`, `created_by`)
SELECT 1, id, NOW(), 'system' FROM `s2_permission`;

-- 给超级管理员角色分配所有菜单
INSERT INTO `s2_role_menu` (`role_id`, `menu_id`, `created_at`, `created_by`)
SELECT 1, id, NOW(), 'system' FROM `s2_menu`;

-- 给管理员角色分配部分权限
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`, `created_at`, `created_by`) VALUES
(2, 1, NOW(), 'system'),
(2, 2, NOW(), 'system'),
(2, 3, NOW(), 'system'),
(2, 5, NOW(), 'system'),
(2, 9, NOW(), 'system');

-- 给管理员角色分配部分菜单
INSERT INTO `s2_role_menu` (`role_id`, `menu_id`, `created_at`, `created_by`) VALUES
(2, 1, NOW(), 'system'),
(2, 2, NOW(), 'system'),
(2, 3, NOW(), 'system'),
(2, 4, NOW(), 'system'),
(2, 5, NOW(), 'system');

-- 给普通用户角色分配部分菜单
INSERT INTO `s2_role_menu` (`role_id`, `menu_id`, `created_at`, `created_by`) VALUES
(3, 1, NOW(), 'system'),
(3, 3, NOW(), 'system');

-- 给默认admin用户分配超级管理员角色（假设admin用户ID为1，需要根据实际情况调整）
-- INSERT INTO `s2_user_role` (`user_id`, `role_id`, `created_at`, `created_by`) VALUES (1, 1, NOW(), 'system');

-- 插入默认组织架构
INSERT INTO `s2_organization` (`id`, `org_code`, `org_name`, `parent_id`, `description`, `status`, `sort`, `created_at`, `created_by`) VALUES
(1, 'HEADQUARTERS', '总公司', 0, '公司总部', 1, 1, NOW(), 'system'),
(2, 'TECH_DEPT', '技术部', 1, '技术研发部门', 1, 1, NOW(), 'system'),
(3, 'PRODUCT_DEPT', '产品部', 1, '产品设计部门', 1, 2, NOW(), 'system'),
(4, 'OPERATION_DEPT', '运营部', 1, '运营管理部门', 1, 3, NOW(), 'system'),
(5, 'FRONTEND_TEAM', '前端组', 2, '前端开发团队', 1, 1, NOW(), 'system'),
(6, 'BACKEND_TEAM', '后端组', 2, '后端开发团队', 1, 2, NOW(), 'system'),
(7, 'AI_TEAM', 'AI组', 2, '人工智能研发团队', 1, 3, NOW(), 'system');

-- 给组织架构分配角色
INSERT INTO `s2_role_organization` (`role_id`, `organization_id`, `created_at`, `created_by`) VALUES
(1, 1, NOW(), 'system'),
(2, 2, NOW(), 'system'),
(3, 5, NOW(), 'system'),
(3, 6, NOW(), 'system'),
(3, 7, NOW(), 'system');
