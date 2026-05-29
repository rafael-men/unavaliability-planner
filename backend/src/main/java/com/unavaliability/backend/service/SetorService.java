package com.unavaliability.backend.service;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class SetorService {

    public static final List<String> DEFAULT_SETORES = List.of(
            "Atendimento",
            "Conteúdo",
            "Criação",
            "Social",
            "Performance: CRM/Mídia/SEO",
            "Planejamento",
            "Projetos/Operações",
            "Tecnologia");


    private static final Pattern ITEM_RE = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private final Path primaryFile;
    private final Path tmpFile;

    public SetorService(@Value("${app.setores.file:setores.json}") String fileName,
                        @Value("${java.io.tmpdir:/tmp}") String tmpDir) {
        this.primaryFile = Paths.get(System.getProperty("user.dir"), fileName);
        this.tmpFile = Paths.get(tmpDir, "setores.json");
    }


    public List<String> loadSetores() {
        for (Path f : List.of(primaryFile, tmpFile)) {
            try {
                if (Files.exists(f)) {
                    List<String> parsed = parse(Files.readString(f, StandardCharsets.UTF_8));
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }
        return new ArrayList<>(DEFAULT_SETORES);
    }

    private Path resolveWritableFile() {
        Path dir = primaryFile.getParent();
        if (dir != null && Files.isWritable(dir)) {
            return primaryFile;
        }
        return tmpFile;
    }

    private void saveSetores(List<String> list) {
        Path file = resolveWritableFile();
        try {
            Files.writeString(file, serialize(list), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Nao foi possivel salvar os setores. Verifique as permissoes do sistema de arquivos.");
        }
    }

    public boolean exists(String setor) {
        if (setor == null) {
            return false;
        }
        return loadSetores().stream().anyMatch(s -> s.equalsIgnoreCase(setor));
    }



    public List<String> add(User actor, String name) {
        requireMaster(actor);
        if (name == null || name.trim().isEmpty()) {
            throw ApiException.badRequest("Nome do setor obrigatório.");
        }
        String clean = TextUtils.cleanText(name);
        List<String> list = loadSetores();
        if (list.stream().anyMatch(s -> s.equalsIgnoreCase(clean))) {
            throw ApiException.badRequest("Setor já existe.");
        }
        list.add(clean);
        saveSetores(list);
        return list;
    }

    public List<String> update(User actor, int index, String name) {
        requireMaster(actor);
        List<String> list = loadSetores();
        if (index < 0 || index >= list.size()) {
            throw ApiException.notFound("Setor não encontrado.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw ApiException.badRequest("Nome obrigatório.");
        }
        String clean = TextUtils.cleanText(name);
        for (int i = 0; i < list.size(); i++) {
            if (i != index && list.get(i).equalsIgnoreCase(clean)) {
                throw ApiException.badRequest("Nome já existe.");
            }
        }
        list.set(index, clean);
        saveSetores(list);
        return list;
    }

    public List<String> remove(User actor, int index) {
        requireMaster(actor);
        List<String> list = loadSetores();
        if (index < 0 || index >= list.size()) {
            throw ApiException.notFound("Setor não encontrado.");
        }
        list.remove(index);
        saveSetores(list);
        return list;
    }

    private void requireMaster(User actor) {
        if (!Roles.isMasterAdmin(actor.getRole())) {
            throw ApiException.forbidden("Acesso exclusivo do Admin Master.");
        }
    }



    private List<String> parse(String json) {
        List<String> out = new ArrayList<>();
        int key = json.indexOf("\"setores\"");
        if (key < 0) {
            return out;
        }
        int open = json.indexOf('[', key);
        int close = json.indexOf(']', open);
        if (open < 0 || close < 0) {
            return out;
        }
        Matcher m = ITEM_RE.matcher(json.substring(open + 1, close));
        while (m.find()) {
            out.add(unescape(m.group(1)));
        }
        return out;
    }

    private String serialize(List<String> list) {
        StringBuilder sb = new StringBuilder("{\n  \"setores\": [");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\n    \"").append(escape(list.get(i))).append('"');
            if (i < list.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(list.isEmpty() ? "]" : "\n  ]").append("\n}\n");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
