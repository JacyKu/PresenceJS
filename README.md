# PresenceJS

PresenceJS is a Forge **1.20.1** client-side mod that adds Discord Rich Presence to Minecraft and exposes a broad scripting surface for **KubeJS**.

It uses a pure-Java Discord RPC backend designed for Minecraft mods rather than a native JNI bridge.

## What it does

- Connects the Minecraft client to Discord Rich Presence
- Exposes Discord webhook REST operations, including message execution, webhook management, Slack-compatible payloads, GitHub-compatible payloads, and webhook message editing/deletion
- Ships with automatic menu / singleplayer / multiplayer presence defaults
- Exposes a mutable Rich Presence object to KubeJS every update cycle
- Lets scripts control:
  - Discord application ID
  - activity type
  - details and state
  - timestamps
  - large/small images
  - buttons
- Emits KubeJS events for:
  - presence building
  - Discord ready/disconnect lifecycle
  - asynchronous webhook responses and errors

## Requirements

- Minecraft **1.20.1**
- Forge **47.4.0**
- Java **17**
- KubeJS **2001.6.5-build.16** for scripting integration
- A Discord application with Rich Presence assets configured in the [Discord Developer Portal](https://discord.com/developers/applications)

## Configuration

PresenceJS registers a client config file. The most important setting is the Discord application ID:

- `clientId`
- `webhookEnabled`
- `defaultLargeImageKey`
- `defaultSmallImageKey`
- `menuDetails`
- `singleplayerDetails`
- `multiplayerDetails`

You can leave `clientId` empty and provide it entirely from KubeJS instead.
Webhook functionality is disabled by default and must be enabled explicitly through `webhookEnabled` before any webhook requests can be sent.

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
- `PresenceJS.jsonObject()` / `jsonArray()` / `json(jsonString)` → create or parse Gson JSON payloads for webhook requests
- `PresenceJS.webhookMessage()` / `webhookEmbed()` / `webhookField(name, value)` → build common webhook message payloads without hand-writing raw JSON
- `PresenceJS.webhooks()` → access the Discord webhook service

### Client events

- `PresenceJSEvents.build(event => {})`
- `PresenceJSEvents.ready(event => {})`
- `PresenceJSEvents.disconnected(event => {})`
- `PresenceJSEvents.webhookResponse(event => {})`
- `PresenceJSEvents.webhookError(event => {})`

Example build handler:

```js
PresenceJSEvents.build(event => {
  const presence = event.getPresence()
  const context = event.getContext()

  presence.setClientId('123456789012345678')
  presence.setDetails('Playing Minecraft')
  presence.setState(context && context.getSingleplayer() ? 'Singleplayer world' : 'In-game')
  presence.setLargeImage(PresenceJS.image('minecraft', 'Minecraft 1.20.1'))
  presence.setSmallImage(PresenceJS.image('kubejs', 'Customized with KubeJS'))
  presence.setButtons([
    PresenceJS.button('Download Pack', 'https://example.com'),
    PresenceJS.button('Join Discord', 'https://discord.gg/example')
  ])
})
```

## Webhooks

PresenceJS exposes Discord's webhook HTTP surface through `PresenceJS.webhooks()`.

### Request factories

The webhook service includes typed request builders for the documented Discord webhook endpoints:

- `createWebhook(channelId)`
- `getChannelWebhooks(channelId)`
- `getGuildWebhooks(guildId)`
- `getWebhook(webhookId)`
- `getWebhookWithToken(webhookId, webhookToken)` or `getWebhookWithToken(webhookUrl)`
- `modifyWebhook(webhookId)`
- `modifyWebhookWithToken(webhookId, webhookToken)` or `modifyWebhookWithToken(webhookUrl)`
- `deleteWebhook(webhookId)`
- `deleteWebhookWithToken(webhookId, webhookToken)` or `deleteWebhookWithToken(webhookUrl)`
- `executeWebhook(webhookId, webhookToken)` or `executeWebhook(webhookUrl)`
- `executeSlackCompatibleWebhook(webhookId, webhookToken)` or `executeSlackCompatibleWebhook(webhookUrl)`
- `executeGithubCompatibleWebhook(webhookId, webhookToken)` or `executeGithubCompatibleWebhook(webhookUrl)`
- `getWebhookMessage(webhookId, webhookToken, messageId)` or `getWebhookMessage(webhookUrl, messageId)`
- `editWebhookMessage(webhookId, webhookToken, messageId)` or `editWebhookMessage(webhookUrl, messageId)`
- `deleteWebhookMessage(webhookId, webhookToken, messageId)` or `deleteWebhookMessage(webhookUrl, messageId)`
- `request(method, pathOrUrl)` for raw access to Discord webhook API paths or an official Discord webhook URL

For real Discord incoming webhooks, the URL overloads are the intended convenience surface. `createWebhook(channelId)` still uses a channel id because that endpoint creates the webhook before a webhook URL exists.
PresenceJS rejects non-Discord hosts, inline query strings, fragments, and non-webhook API routes. Use `.query(...)`, `.waitForResponse(...)`, `.threadId(...)`, and `.withComponents(...)` to add query parameters safely.
PresenceJS also blocks webhook execution entirely unless the client config setting `webhookEnabled` is turned on.

Each `DiscordWebhookRequest` supports:

- `.authorization(rawHeader)`
- `.botToken(token)`
- `.bearerToken(token)`
- `.auditLogReason(reason)`
- `.query(name, value)`
- `.waitForResponse(boolean)`
- `.threadId(threadId)`
- `.withComponents(boolean)`
- `.header(name, value)`
- `.json(JsonElement)` or `.message(DiscordWebhookMessage)`
- `.addFile(webhooks.file(path))`

For common execute payloads, PresenceJS also exposes typed KubeJS builders for:

- message `content`, `username`, `avatar_url`, `tts`, `flags`, and `thread_name`
- embed `title`, `description`, `url`, `color`, `fields`, `author`, `footer`, `timestamp`, `image`, and `thumbnail`

Raw JSON is still supported for webhook features that are not wrapped yet.

The service supports both synchronous and asynchronous execution:

- `webhooks.execute(request)` returns a `DiscordWebhookResponse`
- `webhooks.submit(request)` queues the request on a background thread and returns a request id
- `webhooks.cancel(requestId)` cancels a queued request when possible

### KubeJS examples

Execute a webhook message and wait for the created message object:

```js
const webhooks = PresenceJS.webhooks()
const webhookUrl = 'https://discord.com/api/webhooks/WEBHOOK_ID/WEBHOOK_TOKEN'
const message = PresenceJS.webhookMessage()
message.setContent('Hello from PresenceJS')
message.setUsername('PresenceJS')

const embed = PresenceJS.webhookEmbed()
embed.setTitle('Webhook builder')
embed.setDescription('Built from typed KubeJS helpers')

const field = PresenceJS.webhookField('Mode', 'Typed message body')
field.setInline(true)
embed.addField(field)

message.addEmbed(embed)

const response = webhooks.execute(
  webhooks.executeWebhook(webhookUrl)
    .waitForResponse(true)
    .message(message)
)

if (response.isSuccess()) {
  console.log(response.getBody())
}
```

Create or manage webhooks with an authenticated bot token:

```js
const webhooks = PresenceJS.webhooks()
const body = PresenceJS.json('{"name":"PresenceJS Hook"}')

const response = webhooks.execute(
  webhooks.createWebhook('CHANNEL_ID')
    .botToken('YOUR_BOT_TOKEN')
    .auditLogReason('Provisioned from PresenceJS')
    .json(body)
)
```

Send a webhook asynchronously and handle completion in KubeJS events:

```js
const webhooks = PresenceJS.webhooks()
const webhookUrl = 'https://discord.com/api/webhooks/WEBHOOK_ID/WEBHOOK_TOKEN'
const message = PresenceJS.webhookMessage()
message.setContent('Async message')
const requestId = webhooks.submit(
  webhooks.executeWebhook(webhookUrl)
    .waitForResponse(true)
    .message(message)
)

PresenceJSEvents.webhookResponse(event => {
  if (event.getRequestId() !== requestId) return
  console.log(event.getResponse().getStatusCode())
})

PresenceJSEvents.webhookError(event => {
  if (event.getRequestId() !== requestId) return
  console.error(event.getMessage())
})
```

For payloads with files, use `webhooks.file(path)` or `webhooks.textFile(filename, content)` and add the parts to the request with `.addFile(...)`. For unsupported webhook properties, you can still drop to raw JSON with `PresenceJS.json(...)` plus `.json(...)`. JSON request bodies and response bodies use Gson types, and `com.google.gson` is whitelisted for client-side KubeJS scripts.




