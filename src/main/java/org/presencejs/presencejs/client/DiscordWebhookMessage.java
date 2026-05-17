package org.presencejs.presencejs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class DiscordWebhookMessage {
    private String content;
    private String username;
    private String avatarUrl;
    private Boolean tts;
    private Integer flags;
    private String threadName;
    private final List<Embed> embeds = new ArrayList<>();

    public DiscordWebhookMessage() {
    }

    public DiscordWebhookMessage(DiscordWebhookMessage other) {
        content = other.content;
        username = other.username;
        avatarUrl = other.avatarUrl;
        tts = other.tts;
        flags = other.flags;
        threadName = other.threadName;
        other.embeds.stream().map(Embed::new).forEach(embeds::add);
    }

    public DiscordWebhookMessage copy() {
        return new DiscordWebhookMessage(this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        if (!isBlank(content)) {
            json.addProperty("content", content);
        }
        if (!isBlank(username)) {
            json.addProperty("username", username);
        }
        if (!isBlank(avatarUrl)) {
            json.addProperty("avatar_url", avatarUrl);
        }
        if (tts != null) {
            json.addProperty("tts", tts.booleanValue());
        }
        if (flags != null) {
            json.addProperty("flags", flags.intValue());
        }
        if (!isBlank(threadName)) {
            json.addProperty("thread_name", threadName);
        }
        if (!embeds.isEmpty()) {
            JsonArray embedsJson = new JsonArray();
            for (Embed embed : embeds) {
                if (!embed.hasData()) {
                    continue;
                }
                embedsJson.add(embed.toJson());
            }
            if (embedsJson.size() > 0) {
                json.add("embeds", embedsJson);
            }
        }

        return json;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void clearContent() {
        content = null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void clearUsername() {
        username = null;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void clearAvatarUrl() {
        avatarUrl = null;
    }

    public Boolean getTts() {
        return tts;
    }

    public void setTts(Boolean tts) {
        this.tts = tts;
    }

    public Integer getFlags() {
        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void clearThreadName() {
        threadName = null;
    }

    public List<Embed> getEmbeds() {
        return embeds;
    }

    public void addEmbed(Embed embed) {
        if (embed != null && embeds.size() < 10) {
            embeds.add(new Embed(embed));
        }
    }

    public void clearEmbeds() {
        embeds.clear();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DiscordWebhookMessage that)) {
            return false;
        }
        return Objects.equals(content, that.content)
                && Objects.equals(username, that.username)
                && Objects.equals(avatarUrl, that.avatarUrl)
                && Objects.equals(tts, that.tts)
                && Objects.equals(flags, that.flags)
                && Objects.equals(threadName, that.threadName)
                && Objects.equals(embeds, that.embeds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, username, avatarUrl, tts, flags, threadName, embeds);
    }

    public static final class Embed {
        private String title;
        private String description;
        private String url;
        private Integer color;
        private final List<EmbedField> fields = new ArrayList<>();
        private final EmbedAuthor author = new EmbedAuthor();
        private final EmbedFooter footer = new EmbedFooter();
        private String timestamp;
        private String imageUrl;
        private String thumbnailUrl;

        public Embed() {
        }

        public Embed(Embed other) {
            title = other.title;
            description = other.description;
            url = other.url;
            color = other.color;
            other.fields.stream().map(EmbedField::new).forEach(fields::add);
            author.copyFrom(other.author);
            footer.copyFrom(other.footer);
            timestamp = other.timestamp;
            imageUrl = other.imageUrl;
            thumbnailUrl = other.thumbnailUrl;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();

            if (!isBlank(title)) {
                json.addProperty("title", title);
            }
            if (!isBlank(description)) {
                json.addProperty("description", description);
            }
            if (!isBlank(url)) {
                json.addProperty("url", url);
            }
            if (color != null) {
                json.addProperty("color", color.intValue());
            }
            if (!fields.isEmpty()) {
                JsonArray fieldsJson = new JsonArray();
                for (EmbedField field : fields) {
                    if (!field.hasData()) {
                        continue;
                    }
                    fieldsJson.add(field.toJson());
                }
                if (fieldsJson.size() > 0) {
                    json.add("fields", fieldsJson);
                }
            }
            if (author.hasData()) {
                json.add("author", author.toJson());
            }
            if (footer.hasData()) {
                json.add("footer", footer.toJson());
            }
            if (!isBlank(timestamp)) {
                json.addProperty("timestamp", timestamp);
            }
            if (!isBlank(imageUrl)) {
                JsonObject imageJson = new JsonObject();
                imageJson.addProperty("url", imageUrl);
                json.add("image", imageJson);
            }
            if (!isBlank(thumbnailUrl)) {
                JsonObject thumbnailJson = new JsonObject();
                thumbnailJson.addProperty("url", thumbnailUrl);
                json.add("thumbnail", thumbnailJson);
            }

            return json;
        }

        public boolean hasData() {
            return !isBlank(title)
                    || !isBlank(description)
                    || !isBlank(url)
                    || color != null
                    || !fields.isEmpty()
                    || author.hasData()
                    || footer.hasData()
                    || !isBlank(timestamp)
                    || !isBlank(imageUrl)
                    || !isBlank(thumbnailUrl);
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getColor() {
            return color;
        }

        public void setColor(Integer color) {
            this.color = color;
        }

        public List<EmbedField> getFields() {
            return fields;
        }

        public void addField(EmbedField field) {
            if (field != null && fields.size() < 25) {
                fields.add(new EmbedField(field));
            }
        }

        public void clearFields() {
            fields.clear();
        }

        public EmbedAuthor getAuthor() {
            return author;
        }

        public EmbedFooter getFooter() {
            return footer;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Embed embed)) {
                return false;
            }
            return Objects.equals(title, embed.title)
                    && Objects.equals(description, embed.description)
                    && Objects.equals(url, embed.url)
                    && Objects.equals(color, embed.color)
                    && Objects.equals(fields, embed.fields)
                    && Objects.equals(author, embed.author)
                    && Objects.equals(footer, embed.footer)
                    && Objects.equals(timestamp, embed.timestamp)
                    && Objects.equals(imageUrl, embed.imageUrl)
                    && Objects.equals(thumbnailUrl, embed.thumbnailUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, description, url, color, fields, author, footer, timestamp, imageUrl, thumbnailUrl);
        }
    }

    public static final class EmbedField {
        private String name;
        private String value;
        private boolean inline;

        public EmbedField() {
        }

        public EmbedField(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public EmbedField(EmbedField other) {
            name = other.name;
            value = other.value;
            inline = other.inline;
        }

        public boolean hasData() {
            return !isBlank(name) && !isBlank(value);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("name", name);
            json.addProperty("value", value);
            if (inline) {
                json.addProperty("inline", true);
            }
            return json;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isInline() {
            return inline;
        }

        public void setInline(boolean inline) {
            this.inline = inline;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof EmbedField that)) {
                return false;
            }
            return inline == that.inline
                    && Objects.equals(name, that.name)
                    && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, inline);
        }
    }

    public static final class EmbedAuthor {
        private String name;
        private String url;
        private String iconUrl;

        public EmbedAuthor() {
        }

        public EmbedAuthor(EmbedAuthor other) {
            copyFrom(other);
        }

        public void copyFrom(EmbedAuthor other) {
            name = other.name;
            url = other.url;
            iconUrl = other.iconUrl;
        }

        public boolean hasData() {
            return !isBlank(name) || !isBlank(url) || !isBlank(iconUrl);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (!isBlank(name)) {
                json.addProperty("name", name);
            }
            if (!isBlank(url)) {
                json.addProperty("url", url);
            }
            if (!isBlank(iconUrl)) {
                json.addProperty("icon_url", iconUrl);
            }
            return json;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public void setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof EmbedAuthor that)) {
                return false;
            }
            return Objects.equals(name, that.name)
                    && Objects.equals(url, that.url)
                    && Objects.equals(iconUrl, that.iconUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, url, iconUrl);
        }
    }

    public static final class EmbedFooter {
        private String text;
        private String iconUrl;

        public EmbedFooter() {
        }

        public EmbedFooter(EmbedFooter other) {
            copyFrom(other);
        }

        public void copyFrom(EmbedFooter other) {
            text = other.text;
            iconUrl = other.iconUrl;
        }

        public boolean hasData() {
            return !isBlank(text) || !isBlank(iconUrl);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (!isBlank(text)) {
                json.addProperty("text", text);
            }
            if (!isBlank(iconUrl)) {
                json.addProperty("icon_url", iconUrl);
            }
            return json;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public void setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof EmbedFooter that)) {
                return false;
            }
            return Objects.equals(text, that.text)
                    && Objects.equals(iconUrl, that.iconUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, iconUrl);
        }
    }
}