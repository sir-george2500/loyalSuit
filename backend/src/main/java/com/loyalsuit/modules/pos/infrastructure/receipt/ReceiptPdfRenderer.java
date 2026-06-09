package com.loyalsuit.modules.pos.infrastructure.receipt;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.pos.application.dto.ReceiptView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link ReceiptView} into an 80 mm thermal-receipt-style PDF using PDFBox —
 * a tall, narrow page written top-down. Content longer than a page spills onto a new one.
 *
 * <p>Only the standard Helvetica fonts are used (no embedding); any character the font
 * can't encode is replaced with '?' so a stray glyph in a product name can never make
 * receipt generation throw.
 */
@Component
public class ReceiptPdfRenderer {

    private static final float WIDTH = 226.772f;   // 80 mm
    private static final float HEIGHT = 800f;       // tall; a long sale paginates
    private static final float MARGIN = 12f;
    private static final float CONTENT_WIDTH = WIDTH - 2 * MARGIN;

    private static final float BODY_SIZE = 8f;
    private static final float TITLE_SIZE = 12f;
    private static final float LEADING = 12f;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] render(ReceiptView view) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Cursor cursor = new Cursor(doc);

            cursor.centered(view.storeName(), bold, TITLE_SIZE);
            cursor.centered("Sales Receipt", regular, BODY_SIZE);
            cursor.gap();

            cursor.left("Receipt: " + view.orderNumber(), regular, BODY_SIZE);
            cursor.left("Date: " + DATE_FMT.format(view.soldAt().atZone(view.timezone())), regular, BODY_SIZE);
            if (view.cashierName() != null) {
                cursor.left("Cashier: " + view.cashierName(), regular, BODY_SIZE);
            }
            cursor.rule();

            for (ReceiptView.Line line : view.lines()) {
                cursor.left(line.name(), regular, BODY_SIZE);
                String qty = line.quantity() + " x " + money(view.currency(), line.unitPrice());
                cursor.spread(qty, money(view.currency(), line.lineTotal()), regular, BODY_SIZE);
            }
            cursor.rule();

            cursor.spread("Subtotal", money(view.currency(), view.subtotal()), regular, BODY_SIZE);
            cursor.spread("Total", money(view.currency(), view.total()), bold, BODY_SIZE);
            if (view.cardAmount() != null && view.cardAmount().signum() > 0) {
                cursor.spread("Card", money(view.currency(), view.cardAmount()), regular, BODY_SIZE);
            }
            cursor.spread("Cash", money(view.currency(), view.amountTendered()), regular, BODY_SIZE);
            cursor.spread("Change", money(view.currency(), view.changeGiven()), regular, BODY_SIZE);
            cursor.left("Items: " + view.itemCount(), regular, BODY_SIZE);
            cursor.rule();

            cursor.gap();
            cursor.centered("Thank you!", regular, BODY_SIZE);
            cursor.close();

            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Could not generate the receipt PDF");
        }
    }

    private static String money(String currency, BigDecimal amount) {
        return currency + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Replaces characters Helvetica (WinAnsi) can't encode so {@code showText} never throws. */
    private static String safe(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean encodable = (c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF);
            sb.append(encodable ? c : '?');
        }
        return sb.toString();
    }

    /** A top-down writer over one or more pages; starts a fresh page when it runs out of room. */
    private final class Cursor {
        private final PDDocument doc;
        private PDPageContentStream stream;
        private float y;

        private Cursor(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(new PDRectangle(WIDTH, HEIGHT));
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = HEIGHT - MARGIN - LEADING;
        }

        private void ensureRoom() throws IOException {
            if (y < MARGIN + LEADING) {
                newPage();
            }
        }

        private void show(String text, PDType1Font font, float size, float x) throws IOException {
            ensureRoom();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, y);
            stream.showText(safe(text));
            stream.endText();
            y -= LEADING;
        }

        /** Left-aligned, wrapped to the content width. */
        private void left(String text, PDType1Font font, float size) throws IOException {
            for (String row : wrap(text, font, size, CONTENT_WIDTH)) {
                show(row, font, size, MARGIN);
            }
        }

        private void centered(String text, PDType1Font font, float size) throws IOException {
            for (String row : wrap(text, font, size, CONTENT_WIDTH)) {
                show(row, font, size, MARGIN + (CONTENT_WIDTH - width(row, font, size)) / 2);
            }
        }

        /** {@code label} on the left, {@code value} hard against the right margin, one row. */
        private void spread(String label, String value, PDType1Font font, float size) throws IOException {
            ensureRoom();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(safe(label));
            stream.endText();

            float valueX = WIDTH - MARGIN - width(value, font, size);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(valueX, y);
            stream.showText(safe(value));
            stream.endText();
            y -= LEADING;
        }

        private void rule() throws IOException {
            ensureRoom();
            stream.setLineWidth(0.4f);
            stream.moveTo(MARGIN, y + LEADING - 4);
            stream.lineTo(WIDTH - MARGIN, y + LEADING - 4);
            stream.stroke();
            y -= LEADING / 2;
        }

        private void gap() {
            y -= LEADING / 2;
        }

        private void close() throws IOException {
            stream.close();
        }
    }

    private static float width(String text, PDType1Font font, float size) {
        try {
            return font.getStringWidth(safe(text)) / 1000 * size;
        } catch (IOException e) {
            return 0f;
        }
    }

    /** Greedy word-wrap; a single word longer than the width is broken character by character. */
    private static List<String> wrap(String text, PDType1Font font, float size, float maxWidth) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = row.isEmpty() ? word : row + " " + word;
            if (width(candidate, font, size) <= maxWidth) {
                row.setLength(0);
                row.append(candidate);
                continue;
            }
            if (!row.isEmpty()) {
                rows.add(row.toString());
                row.setLength(0);
            }
            while (width(word, font, size) > maxWidth && word.length() > 1) {
                int cut = word.length();
                while (cut > 1 && width(word.substring(0, cut), font, size) > maxWidth) {
                    cut--;
                }
                rows.add(word.substring(0, cut));
                word = word.substring(cut);
            }
            row.append(word);
        }
        if (!row.isEmpty()) {
            rows.add(row.toString());
        }
        return rows.isEmpty() ? List.of("") : rows;
    }
}
