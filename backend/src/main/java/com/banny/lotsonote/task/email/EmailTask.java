package com.banny.lotsonote.task.email;

import lombok.Data;

@Data
public class EmailTask {
    private String email;
    private String code;
    private String type;
    private long timestamp;
}
