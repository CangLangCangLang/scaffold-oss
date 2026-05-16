package com.scaffold.framework.web.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.scaffold.common.constant.Constants;
import com.scaffold.common.constant.UserConstants;
import com.scaffold.common.core.domain.entity.SysRole;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.system.service.ISysMenuService;
import com.scaffold.system.service.ISysRoleService;

/**
 * 用户权限处理
 * 
 * @author scaffold
 */
@Component
public class SysPermissionService
{
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取角色数据权限
     * 
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user)
    {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            roles.add(Constants.SUPER_ADMIN);
        }
        else
        {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     * 
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(SysUser user)
    {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            perms.add(Constants.ALL_PERMISSION);
        }
        else
        {
            List<SysRole> roles = user.getRoles();
            if (!CollectionUtils.isEmpty(roles))
            {
                // 多角色设置permissions属性，以便数据权限匹配权限
                for (SysRole role : roles)
                {
                    if (!StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL))
                    {
                        continue;
                    }
                    // 注：原实现里 admin role (role_id=1) 被跳过，意图是"留给 super-admin 走 ALL_PERMISSION 分支"。
                    // 但这导致普通用户被赋予 admin role 时拿不到任何菜单权限（菜单 → role_id=1 的 sys_role_menu 全被忽略）。
                    // 修法：admin role 与 common role 一视同仁地查菜单权限；super-admin 仍由 user.isAdmin() 那条短路覆盖。
                    Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                    role.setPermissions(rolePerms);
                    perms.addAll(rolePerms);
                }
            }
            else
            {
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
        }
        return perms;
    }
}
