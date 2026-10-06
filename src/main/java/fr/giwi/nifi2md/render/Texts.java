/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.render;

/**
 * Escaping helpers shared by the Markdown and Mermaid emitters.
 */
public final class Texts {

    private Texts() {
    }

    /**
     * Escapes a value so it is safe inside a Markdown table cell.
     * Pipes become {@code \|}, line breaks become {@code <br>}.
     */
    public static String markdownCell(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        s = s.replace("|", "\\|");
        s = s.replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
        return s;
    }

    /**
     * Truncates a value to {@code max} characters, appending an ellipsis when cut.
     * A {@code max} of zero or less disables truncation.
     */
    public static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (max <= 0 || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "\u2026";
    }

    /**
     * Makes a value safe to embed in a quoted Mermaid node or edge label.
     */
    public static String mermaidLabel(String value) {
        if (value == null || value.isBlank()) {
            return " ";
        }
        String s = value
                .replace("\\", "/")
                .replace("\"", "'")
                .replace("`", "'")
                .replace("\r", " ")
                .replace("\n", "<br/>");
        return s;
    }

    /**
     * Builds a GitHub-flavoured heading anchor.
     *
     * <p>GitHub lowercases, drops anything that is not a letter, digit, space or hyphen,
     * then turns every space into a hyphen without collapsing repeats, so a heading such
     * as {@code "Processor: Foo  Bar"} anchors to {@code processor-foo--bar}.</p>
     */
    public static String slug(String value) {
        if (value == null) {
            return "";
        }
        String s = value.toLowerCase().trim().replaceAll("[^a-z0-9 -]", "");
        return s.replace(' ', '-');
    }

    public static String orDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}
