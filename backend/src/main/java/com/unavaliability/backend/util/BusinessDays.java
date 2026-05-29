package com.unavaliability.backend.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class BusinessDays {

    private BusinessDays() {
    }

    public static int count(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            DayOfWeek dow = cur.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            cur = cur.plusDays(1);
        }
        return count;
    }
}
