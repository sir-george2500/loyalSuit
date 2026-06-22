package com.loyalsuit.common.media;

import com.loyalsuit.common.exception.BusinessException;

/**
 * Shared validation for image uploads (untrusted input). Used by every feature that
 * accepts an image — product gallery, vendor logo, and so on — so the security posture
 * is identical everywhere:
 * <ul>
 *   <li>Bytes are validated by <b>magic number</b>, never the client-supplied MIME type:
 *       a payload is accepted only if it really is a JPEG/PNG/GIF/WEBP.</li>
 *   <li>Size is capped server-side regardless of any client claim.</li>
 * </ul>
 */
public final class ImageBytes {

    /** Hard server-side cap, independent of any client-supplied size. */
    public static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private ImageBytes() {}

    /**
     * Validates that {@code data} is a non-empty, within-size, real image.
     *
     * @throws BusinessException if the bytes are empty, too large, or not an accepted image
     */
    public static void validate(byte[] data) {
        if (data == null || data.length == 0) {
            throw new BusinessException("The uploaded file is empty");
        }
        if (data.length > MAX_BYTES) {
            throw new BusinessException("Image is too large (max 5 MB)");
        }
        if (detect(data) == null) {
            throw new BusinessException("Only JPEG, PNG, GIF, or WEBP images are allowed");
        }
    }

    /**
     * Returns the detected image format ({@code jpeg}/{@code png}/{@code gif}/{@code webp}),
     * or {@code null} if the bytes are not one of the accepted image types. Sniffs the
     * leading magic bytes — never trusts a client-supplied content type.
     */
    public static String detect(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "png";
        }
        if (d.length >= 6 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8'
                && (d[4] == '7' || d[4] == '9') && d[5] == 'a') {
            return "gif";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
