package com.vpn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class App extends Application {

    private static final String CONNECT_URL = "http://shobhit-vpn.duckdns.org:8080/api/vpn/connect";
    private static final String DISCONNECT_URL = "http://shobhit-vpn.duckdns.org:8080/api/vpn/disconnect";

    private static final String WG_EXE_PATH = "tools\\wg.exe";

    private static final String CONFIG_DIR = "vpn-client";
    private static final String CONFIG_FILE = "vpn-client.conf";
    private static final String KEY_FILE = "keys.txt";
    private static final String DEVICE_FILE = "device.txt";

    private static final String HELPER_HOST = "127.0.0.1";
    private static final int HELPER_PORT = 9876;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String lastClientPublicKey;

    @Override
    public void start(Stage stage) {

        Label statusLabel = new Label("Not Connected");

        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");
        disconnectButton.setDisable(true);

        TextArea outputArea = new TextArea();
        outputArea.setPrefHeight(300);
        outputArea.setWrapText(true);

        connectButton.setOnAction(e -> connectVPN(
                statusLabel,
                outputArea,
                connectButton,
                disconnectButton
        ));

        disconnectButton.setOnAction(e -> disconnectVPN(
                statusLabel,
                outputArea,
                connectButton,
                disconnectButton
        ));

        VBox root = new VBox(12,
                statusLabel,
                connectButton,
                disconnectButton,
                outputArea
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        stage.setScene(new Scene(root, 600, 500));
        stage.setTitle("VPN Client");
        stage.show();
    }

    // ================= CONNECT =================

    private void connectVPN(
            Label statusLabel,
            TextArea outputArea,
            Button connectButton,
            Button disconnectButton
    ) {

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("Loading device and keys...");
                    connectButton.setDisable(true);
                    disconnectButton.setDisable(true);
                    outputArea.setText("Loading saved device identity and WireGuard keys...");
                });

                String deviceName = getOrCreateDeviceName();

                String[] keys = loadOrGenerateKeys();

                String clientPrivateKey = keys[0];
                String clientPublicKey = keys[1];

                lastClientPublicKey = clientPublicKey;

                Platform.runLater(() -> {
                    statusLabel.setText("Connecting...");
                    outputArea.setText("Calling backend connect API...");
                });

                String requestJson = "{"
                        + "\"Devicename\":\"" + escapeJson(deviceName) + "\","
                        + "\"publickey\":\"" + escapeJson(clientPublicKey) + "\""
                        + "}";

                try {
                    File oldConfFile = getConfigFile();
                    if (oldConfFile.exists()) {
                        sendCommandToHelper("DISCONNECT " + oldConfFile.getAbsolutePath());
                    }
                } catch (Exception ignored) {
                    // Ignore because tunnel may not exist yet
                }

                String backendResponse = sendPostRequest(CONNECT_URL, requestJson);

                JsonNode root = objectMapper.readTree(backendResponse);

                String config = buildConfig(
                        clientPrivateKey,
                        root.get("assignedIp").asText(),
                        root.get("dns").asText(),
                        root.get("serverPublicKey").asText(),
                        root.get("serverEndpoint").asText(),
                        root.get("allowedIps").asText(),
                        root.get("persistentKeepalive").asInt()
                );

                File confFile = saveConfig(config);

                String helperOutput = sendCommandToHelper(
                        "CONNECT " + confFile.getAbsolutePath()
                );

                Platform.runLater(() -> {
                    statusLabel.setText("Connected");
                    connectButton.setDisable(true);
                    disconnectButton.setDisable(false);
                    outputArea.setText(
                            "VPN connected successfully.\n\n" +
                                    "Device Name:\n" + deviceName + "\n\n" +
                                    "Client Public Key:\n" + clientPublicKey + "\n\n" +
                                    "Assigned IP:\n" + root.get("assignedIp").asText() + "\n\n" +
                                    "Config File:\n" + confFile.getAbsolutePath() + "\n\n" +
                                    "Helper Response:\n" + helperOutput
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    statusLabel.setText("Failed");
                    connectButton.setDisable(false);
                    disconnectButton.setDisable(true);
                    outputArea.setText(e.getMessage());
                });
            }
        }).start();
    }

    // ================= DISCONNECT =================

    private void disconnectVPN(
            Label statusLabel,
            TextArea outputArea,
            Button connectButton,
            Button disconnectButton
    ) {

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("Disconnecting...");
                    connectButton.setDisable(true);
                    disconnectButton.setDisable(true);
                    outputArea.setText("Stopping local tunnel through helper service...");
                });

                String clientPublicKey;

                if (lastClientPublicKey != null && !lastClientPublicKey.isBlank()) {
                    clientPublicKey = lastClientPublicKey;
                } else {
                    String[] keys = loadOrGenerateKeys();
                    clientPublicKey = keys[1];
                }

                File confFile = getConfigFile();

                String helperOutput = sendCommandToHelper(
                        "DISCONNECT " + confFile.getAbsolutePath()
                );

                String requestJson = "{"
                        + "\"clientPublicKey\":\"" + escapeJson(clientPublicKey) + "\""
                        + "}";

                String backendResponse = sendPostRequest(DISCONNECT_URL, requestJson);

                lastClientPublicKey = null;

                Platform.runLater(() -> {
                    statusLabel.setText("Disconnected");
                    connectButton.setDisable(false);
                    disconnectButton.setDisable(true);
                    outputArea.setText(
                            "VPN disconnected successfully.\n\n" +
                                    "Helper Response:\n" + helperOutput + "\n\n" +
                                    "Backend Response:\n" + backendResponse
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    statusLabel.setText("Disconnect Failed");
                    connectButton.setDisable(false);
                    disconnectButton.setDisable(false);
                    outputArea.setText(e.getMessage());
                });
            }
        }).start();
    }

    // ================= HELPER SERVICE COMMUNICATION =================

    private String sendCommandToHelper(String command) throws IOException {

        try (Socket socket = new Socket(HELPER_HOST, HELPER_PORT)) {

            socket.setSoTimeout(30000);

            try (OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                out.write(command.getBytes(StandardCharsets.UTF_8));
                out.flush();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)
                );

                String response = reader.lines().collect(Collectors.joining("\n"));

                if (response == null || response.isBlank()) {
                    throw new IOException("Empty response from VPN helper service.");
                }

                if (response.startsWith("ERROR")) {
                    throw new IOException("VPN helper error: " + response);
                }

                return response;
            }
        } catch (IOException e) {
            throw new IOException(
                    "Could not communicate with VPN helper service on 127.0.0.1:9876.\n" +
                            "Make sure ShobhitVpnHelperService is installed and running.\n\n" +
                            e.getMessage(),
                    e
            );
        }
    }

    // ================= DEVICE IDENTITY =================

    private File getAppDataDir() {
        return new File(System.getProperty("user.home"), CONFIG_DIR);
    }

    private File getDeviceFile() {
        return new File(getAppDataDir(), DEVICE_FILE);
    }

    private String getOrCreateDeviceName() throws IOException {

        File deviceFile = getDeviceFile();

        if (deviceFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(deviceFile))) {
                String savedDeviceName = reader.readLine();

                if (savedDeviceName != null && !savedDeviceName.isBlank()) {
                    return savedDeviceName.trim();
                }
            }
        }

        String generatedDeviceName = generateReadableDeviceName();

        File parentDir = deviceFile.getParentFile();

        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
        }

        try (FileWriter writer = new FileWriter(deviceFile)) {
            writer.write(generatedDeviceName);
        }

        return generatedDeviceName;
    }

    private String generateReadableDeviceName() {
        try {
            String user = System.getProperty("user.name");
            String host = InetAddress.getLocalHost().getHostName();

            user = sanitizeDeviceName(user);
            host = sanitizeDeviceName(host);

            if (!user.isBlank() && !host.isBlank()) {
                return user + "-" + host;
            }

            if (!host.isBlank()) {
                return host;
            }

            if (!user.isBlank()) {
                return user + "-MyVPN-Device";
            }

        } catch (Exception ignored) {
            // fallback below
        }

        return "MyVPN-Device";
    }

    private String sanitizeDeviceName(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("[^a-zA-Z0-9-_]", "-")
                .replaceAll("-+", "-");
    }

    // ================= KEY FILE PERSISTENCE =================

    private File getKeyFile() {
        return new File(getAppDataDir(), KEY_FILE);
    }

    private String[] loadOrGenerateKeys() throws IOException, InterruptedException {

        File keyFile = getKeyFile();

        if (keyFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(keyFile))) {
                String privateKey = reader.readLine();
                String publicKey = reader.readLine();

                if (privateKey != null && publicKey != null &&
                        !privateKey.isBlank() && !publicKey.isBlank()) {

                    return new String[]{
                            privateKey.trim(),
                            publicKey.trim()
                    };
                }
            }
        }

        String privateKey = generatePrivateKey();
        String publicKey = generatePublicKey(privateKey);

        File parentDir = keyFile.getParentFile();

        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
        }

        try (FileWriter writer = new FileWriter(keyFile)) {
            writer.write(privateKey + "\n");
            writer.write(publicKey + "\n");
        }

        return new String[]{privateKey, publicKey};
    }

    // ================= WIREGUARD KEY GENERATION =================

    private String generatePrivateKey() throws IOException, InterruptedException {
        return runCommand(WG_EXE_PATH, "genkey");
    }

    private String generatePublicKey(String privateKey) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(WG_EXE_PATH, "pubkey");
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (OutputStream os = process.getOutputStream()) {
            os.write(privateKey.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        String publicKey;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            publicKey = reader.lines().collect(Collectors.joining("\n")).trim();
        }

        int exitCode = process.waitFor();

        if (exitCode != 0 || publicKey.isBlank()) {
            throw new IOException("Failed to generate public key using wg.exe");
        }

        return publicKey;
    }

    private String runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        String output;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            output = reader.lines().collect(Collectors.joining("\n")).trim();
        }

        int exitCode = process.waitFor();

        if (exitCode != 0 || output.isBlank()) {
            throw new IOException(
                    "Command failed: " + String.join(" ", command) +
                            "\nOutput: " + output
            );
        }

        return output;
    }

    // ================= CONFIG =================

    private String buildConfig(
            String clientPrivateKey,
            String ip,
            String dns,
            String serverKey,
            String endpoint,
            String allowedIps,
            int keepalive
    ) {
        return "[Interface]\n" +
                "PrivateKey = " + clientPrivateKey + "\n" +
                "Address = " + ip + "\n" +
                "DNS = " + dns + "\n\n" +
                "[Peer]\n" +
                "PublicKey = " + serverKey + "\n" +
                "Endpoint = " + endpoint + "\n" +
                "AllowedIPs = " + allowedIps + "\n" +
                "PersistentKeepalive = " + keepalive;
    }

    private File saveConfig(String config) throws IOException {
        File dir = getAppDataDir();

        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create config directory: " + dir.getAbsolutePath());
        }

        File file = new File(dir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(config);
        }

        return file;
    }

    private File getConfigFile() {
        return new File(getAppDataDir(), CONFIG_FILE);
    }

    // ================= HTTP =================

    private String sendPostRequest(String urlStr, String json) throws IOException {

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        conn.setConnectTimeout(60000);
        conn.setReadTimeout(100000);

        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int statusCode = conn.getResponseCode();

        InputStream inputStream;

        if (statusCode >= 200 && statusCode < 300) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        StringBuilder response = new StringBuilder();

        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        conn.disconnect();

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("HTTP " + statusCode + ": " + response);
        }

        return response.toString();
    }

    // ================= UTIL =================

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    public static void main(String[] args) {
        launch(args);
    }
}