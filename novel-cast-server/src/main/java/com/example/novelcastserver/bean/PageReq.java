package com.example.novelcastserver.bean;

import lombok.Data;

@Data
public class PageReq {

    private Long current = 1L;

    private Long pageSize = 10L;
}
