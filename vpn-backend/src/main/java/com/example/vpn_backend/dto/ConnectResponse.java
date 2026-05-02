package com.example.vpn_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectResponse {
	
	private String assignedIp;
    private String serverPublicKey;
    private String serverEndpoint;
    private String dns;
    private String allowedIps;
    private Integer persistentKeepalive;
	
	

}
