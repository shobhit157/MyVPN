package com.example.vpn_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vpn_backend.dto.ConnectRequest;
import com.example.vpn_backend.dto.ConnectResponse;
import com.example.vpn_backend.dto.DisconnectRequest;
import com.example.vpn_backend.service.VpnProvisionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vpn")
@RequiredArgsConstructor
public class VpnController {
	
	 private final VpnProvisionService vpnProvisionService;

	    @PostMapping("/connect")
	    public ResponseEntity<ConnectResponse> connect(@Valid @RequestBody ConnectRequest request) {
	        ConnectResponse response = vpnProvisionService.connect(request);
	        return ResponseEntity.ok(response);
	    }

	    @PostMapping("/disconnect")
	    public ResponseEntity<String> disconnect(@Valid @RequestBody DisconnectRequest request) {
	        String response = vpnProvisionService.disconnect(request);
	        return ResponseEntity.ok(response);
	    }
	
	

}
