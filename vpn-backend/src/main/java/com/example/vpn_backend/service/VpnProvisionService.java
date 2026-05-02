package com.example.vpn_backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.vpn_backend.dto.ConnectRequest;
import com.example.vpn_backend.dto.ConnectResponse;
import com.example.vpn_backend.dto.DisconnectRequest;
import com.example.vpn_backend.entity.Device;
import com.example.vpn_backend.repository.DeviceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VpnProvisionService {

    private final DeviceRepository deviceRepository;
    private final LocalWireGuardService localWireGuardService;

    @Value("${vpn.server.endpoint}")
    private String serverEndpoint;

    @Value("${vpn.server.dns}")
    private String dns;

    @Value("${vpn.server.allowed-ips}")
    private String allowedIps;

    @Value("${vpn.server.persistent-keepalive}")
    private Integer persistentKeepalive;

    public ConnectResponse connect(ConnectRequest request) {
        Optional<Device> existingDevice =
                deviceRepository.findByClientPublicKey(request.getPublickey());

        Device device;

        if (existingDevice.isPresent()) {
            device = existingDevice.get();
            device.setStatus("CONNECTED");
            device.setLastConnectedAt(LocalDateTime.now());
        } else {
            device = Device.builder()
                    .deviceName(request.getDevicename())
                    .clientPublicKey(request.getPublickey())
                    .assignedIp(allocateNextFreeIp())
                    .status("CONNECTED")
                    .createdAt(LocalDateTime.now())
                    .lastConnectedAt(LocalDateTime.now())
                    .build();
        }

        deviceRepository.save(device);

        localWireGuardService.addPeer(
                device.getClientPublicKey(),
                device.getAssignedIp()
        );

        String serverPublicKey = localWireGuardService.getServerPublicKey();

        return new ConnectResponse(
                device.getAssignedIp(),
                serverPublicKey,
                serverEndpoint,
                dns,
                allowedIps,
                persistentKeepalive
        );
    }

    @Transactional
    public String disconnect(DisconnectRequest request) {
        Device device = deviceRepository.findByClientPublicKey(request.getClientPublicKey())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        localWireGuardService.removePeer(device.getClientPublicKey());

        device.setStatus("DISCONNECTED");
        device.setLastDisconnectedAt(LocalDateTime.now());
        deviceRepository.save(device);

        return "Client disconnected successfully";
    }

    private String allocateNextFreeIp() {
        List<Device> devices = deviceRepository.findAll();

        Set<String> usedIps = devices.stream()
                .map(Device::getAssignedIp)
                .collect(Collectors.toSet());

        for (int i = 2; i <= 254; i++) {
            String candidateIp = "10.8.0." + i + "/32";

            if (!usedIps.contains(candidateIp)) {
                return candidateIp;
            }
        }

        throw new RuntimeException("No free IPs available in VPN subnet");
    }
}
