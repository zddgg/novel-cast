package com.example.novelcastserver.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private String role;
    private String gender;
    private String ageGroup;
    private String backup;

    public Role(String role, String gender, String ageGroup) {
        this.role = role;
        this.gender = gender;
        this.ageGroup = ageGroup;
    }
}