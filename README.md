# MyVPN – One-Click VPN Client

## 🚀 Overview

**MyVPN** is a custom VPN client that makes it easy to connect securely using a single click.

It is built on top of WireGuard, which handles the actual VPN tunnel and encryption. MyVPN focuses on simplifying everything around it—like key generation, peer setup, and connection management.

The system combines a desktop app, backend service, and a Windows helper service to automate the entire connection process.

---

## 📥 Download (Windows)

You can download the latest Windows installer from GitHub Releases:

👉 [Download MyVPN Installer (.msi)]((https://github.com/shobhit157/MyVPN/releases/tag/v1.0))

**Requirements:**

* Windows

**Notes:**

* During installation, Windows may show a security prompt. This is expected because the installer sets up a system-level service. You can proceed after reviewing the details.
* After installation, the **MyVPN app will be available on your Desktop**, and you can launch it directly.
* The application can be **easily uninstalled** from the Windows Apps & Features (or Control Panel), which will also remove the installed service.

---

## 🧠 Architecture

User (JavaFX App)
↓
Windows Helper Service (C++)
↓
WireGuard (tunnel.dll / wireguard.dll)
↓
Spring Boot Backend (Docker on VM)
↓
WireGuard Server (wg0)
↓
Internet

---

## 🛠️ Tech Stack

* **Frontend:** JavaFX (Java 17)
* **Backend:** Spring Boot
* **VPN:** WireGuard
* **System Layer:** C++ (Windows Service)
* **Packaging:** jpackage + WiX Toolset
* **Deployment:** Docker + Linux VM (EC2)

---

## ⚙️ How It Works

1. User clicks **Connect** in the app
2. Client generates key pair 
3. Request sent to backend 
4. Backend:

   * Assigns VPN IP (e.g., 10.8.0.x)
   * Adds peer using `wg`
5. Config returned to client
6. Helper service brings up WireGuard tunnel

---

## 📦 Installer

Packaged as a Windows `.msi` installer:

* Bundles JavaFX app + runtime
* Installs helper service with admin privileges
* Enables VPN usage without repeated permission prompts

---

## ⚠️ Limitations

* Currently supports Windows only
* Backend authentication is not implemented yet
* Designed for single-device usage (multi-device support planned)

---

## 🧪 Status

* VPN connection working end-to-end
* Backend deployed with Docker
* Installer created and tested
