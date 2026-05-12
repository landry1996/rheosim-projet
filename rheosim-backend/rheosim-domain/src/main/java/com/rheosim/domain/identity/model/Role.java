package com.rheosim.domain.identity.model;

import com.rheosim.domain.shared.BaseEntity;

import java.util.UUID;

public class Role extends BaseEntity {

    private RoleName name;
    private String description;

    private Role() {
        super();
    }

    public Role(RoleName name, String description) {
        super();
        this.name = name;
        this.description = description;
    }

    public Role(UUID id, RoleName name, String description) {
        super(id);
        this.name = name;
        this.description = description;
    }

    public RoleName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
