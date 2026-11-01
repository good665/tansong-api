package kr.tansong.demo.verification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** 사업자등록번호/개업일자 입력값 정리와 형식 검사 (국세청 API 를 부르기 전에 오타를 걸러냄) */
public final class BusinessNumbers {

    private static final int[] WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};

    private BusinessNumbers() {
    }

    /** "123-45-67890" 같은 입력에서 숫자만 남김 */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("[^0-9]", "");
    }

    /** 10자리 + 사업자등록번호 체크섬(마지막 자리) 검사 */
    public static boolean isValid(String digits) {
        if (digits == null || !digits.matches("\\d{10}")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        sum += ((digits.charAt(8) - '0') * 5) / 10;

        return (10 - sum % 10) % 10 == digits.charAt(9) - '0';
    }

    /** "2020-01-31" 또는 "20200131" → LocalDate. 형식이 틀리거나 미래 날짜면 IllegalArgumentException */
    public static LocalDate parseStartDate(String raw, LocalDate today) {
        String digits = normalize(raw);
        LocalDate date;

        try {
            date = LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("개업일자를 YYYY-MM-DD 형식으로 입력해 주세요.");
        }

        if (date.isAfter(today) || date.getYear() < 1900) {
            throw new IllegalArgumentException("개업일자를 확인해 주세요.");
        }
        return date;
    }
}
