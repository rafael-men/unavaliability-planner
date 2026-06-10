package com.unavaliability.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Unavailability;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class ExportService {


    public enum Formato { CSV, XLSX, PDF }

    private static final String[] HEADERS =
            {"ID", "Colaborador", "Setor", "Tipo", "Início", "Fim", "Dias úteis", "Status"};

    private final UnavailabilityRepository unavailabilityRepository;
    private final UserRepository userRepository;

    public ExportService(UnavailabilityRepository unavailabilityRepository, UserRepository userRepository) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.userRepository = userRepository;
    }

    private void requireViewAll(User actor) {
        if (!Roles.canViewAll(actor.getRole())) {
            throw ApiException.forbidden("Acesso restrito a administradores.");
        }
    }


    public record Arquivo(byte[] conteudo, String contentType, String nomeArquivo) {
    }


    @Transactional(readOnly = true)
    public Arquivo exportarRelatorio(User actor, Formato formato) {
        requireViewAll(actor);
        List<Unavailability> registros =
                unavailabilityRepository.findAllByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(0, 5000));
        List<String[]> linhas = toLinhas(registros);
        return gerar("relatorio-indisponibilidades", "Relatório de Indisponibilidades", linhas, formato);
    }


    @Transactional(readOnly = true)
    public Arquivo exportarCalendario(User actor, Formato formato, LocalDate hoje) {
        requireViewAll(actor);
        List<Unavailability> ativos = unavailabilityRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
                        "approved", hoje, hoje);
        List<String[]> linhas = toLinhas(ativos);
        return gerar("calendario-ativos", "Calendário — Indisponibilidades Ativas", linhas, formato);
    }


    private List<String[]> toLinhas(List<Unavailability> registros) {
        Map<Long, User> userById = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        registros.forEach(u -> ids.add(u.getUserId()));
        if (!ids.isEmpty()) {
            userRepository.findAllById(ids).forEach(u -> userById.put(u.getId(), u));
        }
        List<String[]> linhas = new java.util.ArrayList<>();
        for (Unavailability u : registros) {
            User ru = userById.get(u.getUserId());
            String nome = ru != null && ru.getNome() != null ? ru.getNome() : u.getFullName();
            linhas.add(new String[]{
                    str(u.getId()), nome, str(u.getDepartment()), str(u.getUnavailabilityType()),
                    str(u.getStartDate()), str(u.getEndDate()), str(u.getTotalDays()), str(u.getStatus())
            });
        }
        return linhas;
    }

    private Arquivo gerar(String nomeBase, String titulo, List<String[]> linhas, Formato formato) {
        return switch (formato) {
            case CSV -> new Arquivo(csv(linhas), "text/csv; charset=UTF-8", nomeBase + ".csv");
            case XLSX -> new Arquivo(xlsx(titulo, linhas),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", nomeBase + ".xlsx");
            case PDF -> new Arquivo(pdf(titulo, linhas), "application/pdf", nomeBase + ".pdf");
        };
    }


    private byte[] csv(List<String[]> linhas) {
        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // BOM
        appendCsvLine(sb, HEADERS);
        for (String[] l : linhas) {
            appendCsvLine(sb, l);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvLine(StringBuilder sb, String[] campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(';'); // ; — padrão pt-BR no Excel
            }
            sb.append(escapeCsv(campos[i]));
        }
        sb.append("\r\n");
    }

    private String escapeCsv(String v) {
        String s = v == null ? "" : v;
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }


    private byte[] xlsx(String titulo, List<String[]> linhas) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(titulo.length() > 31 ? titulo.substring(0, 31) : titulo);
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int r = 1;
            for (String[] l : linhas) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < l.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(l[c] == null ? "" : l[c]);
                }
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private byte[] pdf(String titulo, List<String[]> linhas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph p = new Paragraph(titulo, tituloFont);
        p.setSpacingAfter(12f);
        doc.add(p);

        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Element.ALIGN_CENTER);
        for (String h : HEADERS) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (String[] l : linhas) {
            for (String campo : l) {
                table.addCell(new PdfPCell(new Paragraph(campo == null ? "" : campo, cellFont)));
            }
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
