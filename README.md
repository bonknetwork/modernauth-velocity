# ModernAuthentication - Velocity Plugin

A modern authentication plugin for Velocity proxies. This plugin integrates with a backend API to provide a seamless authentication experience for players.

---

## **Important Notes**

- **THIS PLUGIN USES THE nLogin API!** You MUST have nLogin installed on your **Velocity server** for this to work! This plugin is designed to work in conjunction with nLogin for authentication.
- **Access Code Required**: This plugin requires an access code to function. For now, it is invite-only. To gain access, DM **@pyroedged** on Discord.
- **Velocity Support**: This plugin is specifically designed for Velocity proxies. Ensure you are running a Velocity server to use this plugin. the credits of the velocity version of this plugin Comes to **Gabuzard**

---

## **Our Mission with ModernAuth**

ModernAuth's goal is to simplify and revolutionize authentication on offline-mode (cracked) Minecraft servers. By automating password resets and reducing the need for human intervention, we strive to create a more secure, user-friendly environment for both players and server administrators.

Our system drastically streamlines account recovery and password management, eliminating the frustrations associated with lost credentials. In doing so, we allow server owners to focus on delivering the best possible gameplay experience rather than spending time on manual authentication tasks. ModernAuth is designed to protect against unauthorized access, simplify user logins, and ensure that communities can thrive without compromising on convenience or security. We believe that by removing barriers to seamless authentication, we can make Minecraft servers more accessible, enjoyable, and trustworthy for all.

---

## **How It Works**

No available yet

## **Configuration**

After the plugin generates the `config.yml` file, open it and configure the following settings:

### Example `config.yml`:

```yaml
# The URL of your authentication backend.
backendUrl: "https://auth.bonkmc.org"

# The port on which your backend is running.
backendPort: 443

# The access code used when communicating with the backend API.
access-code: "your_access_code_here"

# The public server ID used in the link sent to players.
server-id: "server-id-here"

messages:
  reloadSuccess: "§7------------------------------\n§aModernAuthentication configuration reloaded.\n§7------------------------------"
  notPlayer: "§7------------------------------\nOnly players can use this command.\n§7------------------------------"
  confirmUsage: "§7------------------------------\n§ePlease type /modernconfirm <yes|no>\n§7------------------------------"
  noSwitch: "§7------------------------------\n§aNo problem! You can continue using your current login method.\n§7------------------------------"
  invalidOption: "§7------------------------------\n§eInvalid option. Please type /modernconfirm <yes|no>\n§7------------------------------"
  authSuccess: ""
  authSuccessAfterRegister: ""
  authFailed: "§7------------------------------\n§cAuthentication failed even after registration. Please contact a higher administrator.\n§7------------------------------"
  registrationFailed: "§7------------------------------\n§cRegistration failed. Please contact an administrator. You have not been logged in.\n§7------------------------------"
  tokenCreationFailed: "§7------------------------------\n§cError: Failed to initiate authentication. Please try again later.\n§7------------------------------"
  passwordLoginDisabled: "§7------------------------------\n§cPassword login is modified for this account.\n§aClick here to login using ModernAuth.\n§7------------------------------"
  passwordLoginDisabledHover: "Open the ModernAuth login page"
  switchConfirmation: "§7------------------------------\n§eClick here to switch to ModernAuth\n§7------------------------------"
  switchConfirmationHover: "Switch to ModernAuth"
```

---

## **Commands**

- **`/modernconfirm <yes|no>`**:
  - Allows players to confirm whether they want to switch to ModernAuth.
  - Example:
    - `/modernconfirm yes`: Confirms the switch to ModernAuth.
    - `/modernconfirm no`: Cancels the switch and allows the player to continue using their current login method.

- **`/authreload`**:
  - Reloads the plugin's configuration file (`config.yml`).
  - Permission: `modernauth.reload` (default: OP or console).

---

## **Permissions**

- **`modernauth.reload`**: Allows the use of the `/authreload` command.
  - Default: OP or console.

---

## **Notes**

- **Backend Code**: The backend code is not publicly available.
- **Velocity Support**: This plugin is specifically designed for Velocity proxies. Ensure you are running a Velocity server to use this plugin.

---

## **Support**

For assistance, DM **@pyroedged** on Discord. Please include details about the issue you're experiencing, and we'll do our best to help!

---

## **License**

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

This README provides all the necessary information for users to install, configure, and use your **ModernAuthentication Velocity plugin**. Let me know if you need further adjustments! 😊
