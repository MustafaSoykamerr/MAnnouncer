## 🛁 MAnnouncer | Velocity Plugin for Custom Announcements | 💬 Chat, 🖼️ Titles, 🎯 BossBars, 🌐 Discord Webhooks

---

### 🤩 Intro

**MAnnouncer** is a powerful and modular announcement plugin designed **specifically for Velocity 3.4.0+** and **Java 21+**.

It allows you to create and manage **per-server announcements** easily, with full control over formats, permissions, and delivery methods.

Whether you run a competitive PvP server, a casual SMP, or a full-scale Velocity network — MAnnouncer makes it easy to:

* Send **chat**, **title**, **bossbar**, and **advancement**-based messages.
* Assign **custom permissions** per announcement type and server.
* Manage toggles and cooldowns per message or message type.
* Broadcast to **Discord via Webhooks** (each announcement has its own).
* Detect if a server is down and alert via Discord.
* Organize announcements using `servers/{servername}/` folders.
* Enable/disable announcements with simple commands like:

```bash
/mannouncer announcement boxpvp 1 on
/mannouncer announcement boxpvp 1 off
```

Each message type (chat, bossbar, title, advancement) has its own file, giving you full control.
Includes centralized message support, `MiniMessage`, `#hex` colors, and future `ItemsAdder` support.

---

### 🛡️ Permissions

Below are the permission nodes available in MAnnouncer. Each server and announcement type can be fully controlled with structured nodes.

```yaml
permissions:
  # Base permission node for all commands and features
  base: "mannouncer"

  # Permission nodes for commands
  commands:
    admin: "mannouncer.admin"                # Full admin control
    reload: "mannouncer.reload"              # Reload config & messages
    test: "mannouncer.test"                  # Test announcements
    announcement: "mannouncer.announcement"  # Announcement-related commands
```

**Per-Announcement-Type Permissions:**
You can define permissions for specific announcement types and servers like this:

```
mannouncer.announcement.{type}.{servername}.{id}
```

Example:

```
mannouncer.announcement.chat.boxpvp.1
```

---

### 📘️ Help

If the plugin doesn't seem to work:

* Make sure you're using **Velocity 3.4.0 or above**
* Your server must be running **Java 21**
* Check if you’ve set up the right folders: `servers/BoxPvP/chat-announcement.yml`, etc.
* Reload with `/mannouncer reload` after any change
* Ensure permissions are correctly applied

📩 For detailed support and community help, join our Discord:
**[discord.gg/aghv3QT9wp](https://discord.gg/aghv3QT9wp)**

---

### 🔄 Next Update

Here’s what’s coming in the next version:

* ⚙️ General performance improvements
* 🛠️ Fixes for Twitch/Kick/YouTube auto-live announcements
* 🔧 Improved Discord webhook reliability
* 🎨 Future ItemsAdder integration for styled messages
