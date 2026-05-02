package com.example.vpn_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {
	
	
	    @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(nullable = false)
	    private String deviceName;

	    @Column(nullable = false, unique = true, length = 255)
	    private String clientPublicKey;

	    @Column(nullable = false, unique = true)
	    private String assignedIp;

	    @Column(nullable = false)
	    private String status;

	    @Column(nullable = false)
	    private LocalDateTime createdAt;

	    @Column
	    private LocalDateTime lastConnectedAt;

	    @Column
	    private LocalDateTime lastDisconnectedAt;

}
