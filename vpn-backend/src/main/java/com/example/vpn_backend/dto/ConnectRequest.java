package com.example.vpn_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectRequest {
	@NotBlank(message="Device name is required")
	private String Devicename;
	
	@NotBlank(message="public key is required")
	private String publickey;

}
