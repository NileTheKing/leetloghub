package com.leethublog.controller.dto;

import lombok.Data;

@Data
public class CreateRepoRequestDto {
    private String name;
    private boolean isPrivate;
}
