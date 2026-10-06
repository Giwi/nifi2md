/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.eip;

/**
 * A pattern from Gregor Hohpe's Enterprise Integration Patterns catalogue.
 *
 * <p>The canonical reference is <a href="https://www.enterpriseintegrationpatterns.com/">enterpriseintegrationpatterns.com</a>,
 * which documents the 65 patterns from the book <em>Enterprise Integration Patterns</em>.</p>
 *
 * @param id          stable key, also the anchor used in generated documentation
 * @param name        human readable pattern name, e.g. "Content-Based Router"
 * @param category    the book section the pattern belongs to, e.g. "Message Routing"
 * @param page        path of the pattern page relative to {@link #BASE_URL}
 * @param summary     one sentence describing the pattern in our own words
 */
public record EipPattern(
        String id,
        String name,
        String category,
        String page,
        String summary) {

    /** Base URL of the Enterprise Integration Patterns site. */
    public static final String BASE_URL = "https://www.enterpriseintegrationpatterns.com/patterns/messaging/";

    /** Full link to the pattern page on enterpriseintegrationpatterns.com. */
    public String url() {
        return BASE_URL + page;
    }
}
