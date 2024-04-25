package com.example.novelcastserver.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lines {
    private String index;
    private String lines;
    private Boolean delFlag;
}
