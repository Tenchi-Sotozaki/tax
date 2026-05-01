package jp.lg.asp.accommodation.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoleForm {
    
    private Long roleId;
    private String name;
    private Map<String, Integer> screenPermissions; // screenId -> permission
    private Long version;
    
    public RoleForm() {
    }
    
    public RoleForm(Long roleId, String name, Long version) {
        this.roleId = roleId;
        this.name = name;
        this.version = version;
    }
}