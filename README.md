# PresenceJS

PresenceJS is a Forge **1.20.1** client-side mod that adds Discord Rich Presence to Minecraft and exposes a broad scripting surface for **KubeJS**.

It uses a pure-Java Discord RPC backend designed for Minecraft mods rather than a native JNI bridge.

## What it does

- Connects the Minecraft client to Discord Rich Presence
- Ships with automatic menu / singleplayer / multiplayer presence defaults
- Exposes a mutable Rich Presence object to KubeJS every update cycle
- Lets scripts control:
  - Discord application ID
  - activity type
  - details and state
  - timestamps
  - large/small images
  - buttons
  - party data
  - join / spectate / match secrets
- Emits KubeJS events for:
  - presence building
  - Discord ready/disconnect lifecycle
  - Discord join / spectate callbacks
  - Discord join requests

## Requirements

- Minecraft **1.20.1**
- Forge **47.4.0**
- Java **17**
- KubeJS **2001.6.5-build.16** for scripting integration
- A Discord application with Rich Presence assets configured in the [Discord Developer Portal](https://discord.com/developers/applications)

## Configuration

PresenceJS registers a client config file. The most important setting is the Discord application ID:

- `clientId`
- `defaultLargeImageKey`
- `defaultSmallImageKey`
- `menuDetails`
- `singleplayerDetails`
- `multiplayerDetails`

You can leave `clientId` empty and provide it entirely from KubeJS instead.

## KubeJS bindings

PresenceJS adds a client-side global binding named `PresenceJS` and an event group named `PresenceJSEvents`.

### Main binding methods

- `PresenceJS.activity()` → create a new mutable activity object
- `PresenceJS.button(label, url)` → create a button object
- `PresenceJS.image(key, text)` → create an image object
- `PresenceJS.getContext()` → inspect the latest client snapshot
- `PresenceJS.getBaseActivity()` / `setBaseActivity(activity)`
- `PresenceJS.clearBaseActivity()`
- `PresenceJS.getLastSentActivity()`
- `PresenceJS.getCurrentDiscordUser()`
- `PresenceJS.getConnectionState()` / `getConnectionMessage()`
- `PresenceJS.isConnected()`
- `PresenceJS.isEnabled()` / `setEnabled(enabled)`
- `PresenceJS.refresh()`
- `PresenceJS.disconnect()`

### Client events

- `PresenceJSEvents.build(event => {})`
- `PresenceJSEvents.ready(event => {})`
- `PresenceJSEvents.disconnected(event => {})`
- `PresenceJSEvents.join(event => {})`
- `PresenceJSEvents.spectate(event => {})`
- `PresenceJSEvents.joinRequest(event => {})`

## Example KubeJS script

Edit the included file `kubejs/client_scripts/presence.js`:

```js
PresenceJSEvents.build(event => {
  const ctx = event.context
  const presence = event.presence

  presence.setClientId('123456789012345678')
  presence.setActivityType('PLAYING')

  if (!ctx.inWorld) {
    presence.setDetails('Browsing menus')
    presence.setState(ctx.screenTitle || 'Idle')
    presence.setLargeImage('main_menu', 'Main Menu')
    presence.clearButtons()
    return
  }

  presence.setDetails(ctx.singleplayer ? 'Custom Singleplayer' : 'Custom Multiplayer')
  presence.setState((ctx.worldName || ctx.serverName || 'Unknown world') + ' • ' + (ctx.dimensionId || 'minecraft:overworld'))
  presence.setLargeImage('minecraft_logo', ctx.biomeId || 'Minecraft')
  presence.setSmallImage('pickaxe', ctx.selectedItemName || 'Exploring')
  presence.setStartTimestamp(ctx.worldStartEpochSecond)
  presence.clearButtons()
  presence.addButton('GitHub', 'https://github.com/')
  presence.addButton('Docs', 'https://kubejs.com/')
})

PresenceJSEvents.joinRequest(event => {
  console.info(`Discord join request from ${event.user?.effectiveName || 'unknown user'}`)
  event.approve()
})
```

## Building

If the Gradle wrapper is intact in your environment, build with:

```powershell
.\gradlew.bat build
```

## Notes

- PresenceJS is intentionally **client-side only**.
- If KubeJS is not installed, PresenceJS still provides automatic Rich Presence behavior from the client config.
- Discord Rich Presence images and buttons only work when your Discord application is configured correctly.





