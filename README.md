[​IMG]
MAnnouncer is a powerful, modular, and Velocity-native announcement plugin designed for Minecraft networks using Velocity 3.4.0+ and Java 21.

It allows you to easily manage and deliver announcements across all your servers individually — with full support for:

    Chat announcements
    BossBars
    ️ Titles & Subtitles
    Advancements
    Discord Webhooks


[​IMG]

Base node:
Code (Text):
mannouncer
Command permissions:
Code (Text):

mannouncer.admin         # Access all commands
mannouncer.reload        # Reload all plugin files
mannouncer.test          # Run test announcements
mannouncer.announcement  # Send and manage announcements
 
Per-announcement permissions:
Code (Text):
mannouncer.announcement.<type>.<server>.<id>
Example:
Code (Text):
mannouncer.announcement.chat.boxpvp.1

[​IMG]
Each server has its own folder and announcement types inside:

Code (Text):

/plugins/MAnnouncer/servers/<server-name>/
├─ chat-announcement.yml
├─ bossbar-announcement.yml
├─ title-subtitle-announcement.yml
├─ advancement-announcement.yml
 
To toggle announcements:
Code (Text):
/mannouncer announcement <server> <id> on|off
✅ Supports MiniMessage formatting, HEX colors, and center-aligned lines
✅ Per-announcement Discord webhook integration
✅ Failure detection: If a server is offline, a webhook warning will be sent
✅ Automatically reads servers from your velocity.toml file

Still need help?
➡️ Join our support Discord


[​IMG]

✔ ItemsAdder support (planned)
✔ Better performance for large networks
✔ Improved Twitch/Kick/YouTube live event detection
✔ More reliable and faster Discord webhook delivery

✅ Fully supports Java 21 and Velocity 3.4.0+ (including snapshots)​
