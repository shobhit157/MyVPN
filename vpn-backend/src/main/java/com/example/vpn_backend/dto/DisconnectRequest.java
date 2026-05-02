package com.example.vpn_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisconnectRequest {
	
	@NotBlank(message = "Client public key is required")
    private String clientPublicKey;

}
