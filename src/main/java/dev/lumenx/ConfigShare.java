package dev.lumenx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.lumen.core.Config;
import dev.lumen.feature.Configs;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Скачивание, импорт и экспорт конфигов Lumen через клиентскую команду /lumencfg.
 * Скачанный конфиг сохраняется в config/lumen и сразу появляется во вкладке «Конфиги».
 */
public final class ConfigShare {
    private static final int MAX_BYTES = 1_000_000;

    private ConfigShare() { }

    static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("lumencfg")
                        .executes(c -> { help(); return 1; })
                        .then(ClientCommandManager.literal("download")
                                .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                                        .executes(c -> {
                                            String[] a = StringArgumentType.getString(c, "args").trim().split("\\s+", 2);
                                            download(a[0], a.length > 1 ? a[1] : null);
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("paste")
                                .executes(c -> { paste(null); return 1; })
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(c -> { paste(StringArgumentType.getString(c, "name")); return 1; })))
                        .then(ClientCommandManager.literal("export").executes(c -> { export(); return 1; }))
                        .then(ClientCommandManager.literal("list").executes(c -> { list(); return 1; }))
                        .then(ClientCommandManager.literal("load")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(c -> { load(StringArgumentType.getString(c, "name")); return 1; })))));
    }

    // ---------- вывод ----------
    private static void say(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Lumen] " + s), false);
    }

    private static void help() {
        say("/lumencfg download <ссылка> [имя] — скачать конфиг по ссылке");
        say("/lumencfg paste [имя] — взять конфиг (ссылку или JSON) из буфера обмена");
        say("/lumencfg export — скопировать ваш конфиг в буфер (без меток)");
        say("/lumencfg list | load <имя> — список и применение конфигов");
    }

    private static void list() {
        List<String> names = Configs.list();
        say(names.isEmpty() ? "Конфигов пока нет." : "Конфиги: " + String.join(", ", names));
    }

    private static void load(String name) {
        say(Configs.load(Configs.clean(name)) ? "Конфиг применён: " + name : "Конфиг не найден: " + name);
    }

    // ---------- экспорт / буфер ----------
    private static void export() {
        MinecraftClient mc = MinecraftClient.getInstance();
        JsonObject o = Config.toJson();
        o.remove("waypoints");                       // не раскрываем координаты ваших меток
        String s = Config.GSON.toJson(o);
        GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), s);
        say("Конфиг скопирован в буфер обмена (" + s.length() + " симв.). Вставьте его на pastebin/gist и поделитесь ссылкой.");
    }

    private static void paste(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String text = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (text == null || text.isBlank()) { say("Буфер обмена пуст."); return; }
        text = text.trim();
        if (text.startsWith("http")) download(text, name);
        else store(text, name, "буфер");
    }

    // ---------- скачивание ----------
    private static void download(String url, String name) {
        String u = normalize(url.trim());
        if (!u.startsWith("https://")) { say("Нужна ссылка, начинающаяся с https://"); return; }
        say("Скачиваю конфиг...");
        Thread t = new Thread(() -> {
            String body = null, err = null;
            try {
                body = fetch(u);
            } catch (Exception ex) {
                err = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            }
            final String b = body, e = err;
            MinecraftClient.getInstance().execute(() -> {
                if (e != null) say("Не удалось скачать: " + e);
                else store(b, name != null ? name : baseName(u), "ссылка");
            });
        }, "lumenx-config-download");
        t.setDaemon(true);
        t.start();
    }

    private static String fetch(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "LumenAddons")
                .GET().build();
        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) throw new java.io.IOException("ответ сервера " + resp.statusCode());
        try (InputStream in = resp.body()) {
            byte[] data = in.readNBytes(MAX_BYTES + 1);
            if (data.length > MAX_BYTES) throw new java.io.IOException("файл слишком большой");
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /** github.com/.../blob/... → raw.githubusercontent.com/..., pastebin.com/ID → pastebin.com/raw/ID. */
    private static String normalize(String u) {
        if (u.startsWith("https://github.com/") && u.contains("/blob/")) {
            return u.replace("https://github.com/", "https://raw.githubusercontent.com/").replace("/blob/", "/");
        }
        if (u.startsWith("https://pastebin.com/") && !u.contains("/raw/")) {
            return u.replace("https://pastebin.com/", "https://pastebin.com/raw/");
        }
        return u;
    }

    private static String baseName(String url) {
        String s = url;
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        s = s.substring(s.lastIndexOf('/') + 1);
        if (s.endsWith(".json")) s = s.substring(0, s.length() - 5);
        return s.isBlank() ? "cfg" : s;
    }

    /** Проверяет JSON и кладёт его в config/lumen как новый конфиг (без перезаписи существующих). */
    private static void store(String body, String name, String from) {
        try {
            JsonElement el = JsonParser.parseString(body);
            if (!el.isJsonObject() || !el.getAsJsonObject().has("modules")) {
                say("Это не конфиг Lumen (нет раздела modules).");
                return;
            }
            JsonObject obj = el.getAsJsonObject();
            obj.remove("waypoints");                 // чужие метки не импортируем
            obj.remove("accounts");

            String base = Configs.clean(name == null || name.isBlank() ? "cfg" : name);
            if (base.isEmpty()) base = "cfg";
            Path dir = Config.dir();
            Files.createDirectories(dir);
            String n = base;
            int i = 2;
            while (Configs.isPreset(n) || Files.exists(dir.resolve(n + ".json"))) n = base + i++;

            Files.writeString(dir.resolve(n + ".json"), Config.GSON.toJson(obj), StandardCharsets.UTF_8);
            say("Конфиг сохранён: " + n + " (" + from + "). Откройте меню → «Конфиги» или введите /lumencfg load " + n);
        } catch (Exception ex) {
            say("Не удалось прочитать конфиг: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }
}
