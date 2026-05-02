package com.example.vpn_backend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalWireGuardService {

    public void addPeer(String publicKey, String assignedIp) {
        executeCommand(
                "wg", "set", "wg0",
                "peer", publicKey,
                "allowed-ips", assignedIp
        );
    }

    public void removePeer(String publicKey) {
        executeCommand(
                "wg", "set", "wg0",
                "peer", publicKey,
                "remove"
        );
    }

    public String getServerPublicKey() {
        return executeCommand("wg", "show", "wg0", "public-key").trim();
    }

    private String executeCommand(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            String stdout;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                stdout = reader.lines().collect(Collectors.joining("\n"));
            }

            String stderr;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                stderr = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();

            log.info("WireGuard stdout: {}", stdout);

            if (exitCode != 0) {
                log.error("WireGuard stderr: {}", stderr);
                throw new RuntimeException("WireGuard command failed: " + stderr);
            }

            return stdout;

        } catch (Exception e) {
            throw new RuntimeException("Local WireGuard execution failed", e);
        }
    }
}