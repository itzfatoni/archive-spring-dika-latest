package com.juaracoding.dto.validasi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class VerifyUserDTO {

    @NotNull(message = "NULL!!!")
    @NotBlank(message = "BLANK!!!")
    @NotEmpty(message = "EMPTY!!!")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
