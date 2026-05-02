package com.example.vpn_backend.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.vpn_backend.entity.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device,Long>{
	
	Optional<Device> findByClientPublicKey(String clientPublicKey);
    Optional<Device> findByAssignedIp(String assignedIp);
    

}
