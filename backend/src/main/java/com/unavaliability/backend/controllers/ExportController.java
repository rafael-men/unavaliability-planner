package com.unavaliability.backend.controllers;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.ExportService;
import com.unavaliability.backend.service.ExportService.Arquivo;
import com.unavaliability.backend.service.ExportService.Formato;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/export")
@Tag(name = "Export", description = "Exportação de relatórios e calendário (CSV, Excel, PDF)")
public class ExportController {

    private final ExportService exportService;
    private final CurrentUserProvider currentUser;

    public ExportController(ExportService exportService, CurrentUserProvider currentUser) {
        this.exportService = exportService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Exporta o relatório de indisponibilidades",
            description = "formato = csv | xlsx | pdf (padrão csv).")
    @GetMapping("/relatorio")
    public ResponseEntity<byte[]> relatorio(@RequestParam(defaultValue = "csv") String formato) {
        Arquivo a = exportService.exportarRelatorio(currentUser.require(), parse(formato));
        return resposta(a);
    }

    @Operation(summary = "Exporta o calendário de indisponibilidades ativas",
            description = "formato = csv | xlsx | pdf (padrão csv).")
    @GetMapping("/calendario")
    public ResponseEntity<byte[]> calendario(@RequestParam(defaultValue = "csv") String formato) {
        Arquivo a = exportService.exportarCalendario(currentUser.require(), parse(formato), LocalDate.now());
        return resposta(a);
    }

    private Formato parse(String formato) {
        try {
            return Formato.valueOf(formato.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Formato inválido. Use csv, xlsx ou pdf.");
        }
    }

    private ResponseEntity<byte[]> resposta(Arquivo a) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.nomeArquivo() + "\"")
                .contentType(MediaType.parseMediaType(a.contentType()))
                .body(a.conteudo());
    }
}
