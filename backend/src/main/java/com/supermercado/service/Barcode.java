package com.supermercado.service;

/** Keeps leading zeroes and non-numeric HID codes; removes only outer whitespace. */
public final class Barcode {
    private Barcode() { }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        String code = value.strip();
        if (code.length() > 255 || code.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("El código de barras debe tener hasta 255 caracteres y no incluir controles");
        }
        return code;
    }
}
