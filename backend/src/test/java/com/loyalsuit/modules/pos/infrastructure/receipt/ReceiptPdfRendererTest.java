package com.loyalsuit.modules.pos.infrastructure.receipt;

import com.loyalsuit.modules.pos.application.dto.ReceiptView;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ReceiptPdfRendererTest {

    private final ReceiptPdfRenderer renderer = new ReceiptPdfRenderer();

    private static ReceiptView view(List<ReceiptView.Line> lines, String cashier) {
        return view(lines, cashier, BigDecimal.ZERO);
    }

    private static ReceiptView view(List<ReceiptView.Line> lines, String cashier, BigDecimal card) {
        BigDecimal total = lines.stream().map(ReceiptView.Line::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int items = lines.stream().mapToInt(ReceiptView.Line::quantity).sum();
        return new ReceiptView(
                "Acme Store", "USD", ZoneId.of("UTC"),
                "POS-ABC-123", Instant.parse("2026-06-09T10:15:30Z"), cashier,
                lines, total, total, total.add(new BigDecimal("5.00")), new BigDecimal("5.00"), card, items);
    }

    private static String textOf(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void render_producesAPdf_carryingTheReceiptContent() throws Exception {
        // Arrange
        ReceiptView view = view(List.of(
                new ReceiptView.Line("Blue Widget", 2, new BigDecimal("5.00"), new BigDecimal("10.00")),
                new ReceiptView.Line("Red Gadget", 1, new BigDecimal("3.50"), new BigDecimal("3.50"))), "Jane Doe");

        // Act
        byte[] pdf = renderer.render(view);

        // Assert — a real PDF whose extracted text shows the header, lines, totals, and footer
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        String text = textOf(pdf);
        assertThat(text)
                .contains("Acme Store")
                .contains("POS-ABC-123")
                .contains("Jane Doe")
                .contains("Blue Widget")
                .contains("Red Gadget")
                .contains("USD 13.50")   // total
                .contains("Thank you!");
    }

    @Test
    void render_showsACardLine_forASplitTender() throws Exception {
        // Arrange — a sale with a $4 card portion
        ReceiptView view = view(List.of(
                new ReceiptView.Line("Blue Widget", 1, new BigDecimal("10.00"), new BigDecimal("10.00"))),
                "Jane Doe", new BigDecimal("4.00"));

        // Act
        String text = textOf(renderer.render(view));

        // Assert
        assertThat(text).contains("Card").contains("USD 4.00");
    }

    @Test
    void render_replacesGlyphsTheFontCannotEncode_insteadOfThrowing() {
        // Arrange — a product name with non-WinAnsi characters (emoji, CJK)
        ReceiptView view = view(List.of(
                new ReceiptView.Line("Café ☕ 日本", 1, new BigDecimal("4.00"), new BigDecimal("4.00"))),
                "Jane Doe");

        // Act & Assert
        assertThatCode(() -> {
            byte[] pdf = renderer.render(view);
            assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        }).doesNotThrowAnyException();
    }

    @Test
    void render_paginates_whenTheSaleIsTooLongForOnePage() throws Exception {
        // Arrange — far more lines than fit on a single 80mm page
        List<ReceiptView.Line> lines = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            lines.add(new ReceiptView.Line("Item " + i, 1, new BigDecimal("1.00"), new BigDecimal("1.00")));
        }

        // Act
        byte[] pdf = renderer.render(view(lines, "Jane Doe"));

        // Assert
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }
}
