package com.unavaliability.backend.service;

import java.util.ArrayList;
import java.util.List;


public final class ReportToHelper {

    private ReportToHelper() {
    }

    public static List<String> parseReportTo(String reportTo) {
        List<String> out = new ArrayList<>();
        if (reportTo == null || reportTo.isBlank()) {
            return out;
        }
        for (String part : reportTo.split("[,;]")) {
            String s = part.trim().toLowerCase();
            if (!s.isBlank()) {
                out.add(s);
            }
        }
        return out;
    }

    public static boolean reportToMatchesLider(String reportTo, String liderEmail, String liderName) {
        List<String> parts = parseReportTo(reportTo);
        if (parts.isEmpty()) {
            return false;
        }
        String emailLower = liderEmail == null ? null : liderEmail.toLowerCase();
        String nameLower = liderName == null ? null : liderName.toLowerCase();
        for (String p : parts) {
            if (emailLower != null && p.equals(emailLower)) {
                return true;
            }
            if (nameLower != null && p.equals(nameLower)) {
                return true;
            }
        }
        return false;
    }
}
