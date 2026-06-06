package com.loyalsuit.modules.catalog.application.port;

/**
 * Abstraction over the external media store (Cloudinary). Keeping it behind a port
 * lets the application layer be unit-tested without network calls, and isolates the
 * vendor SDK to one adapter.
 */
public interface MediaStorage {

    /** A stored asset: the provider's id (for deletion) and its secure delivery URL. */
    record StoredAsset(String publicId, String secureUrl) {}

    /**
     * Uploads raw image bytes into the given folder and returns the stored asset.
     *
     * @throws com.loyalsuit.common.exception.BusinessException if the upload fails
     */
    StoredAsset upload(byte[] data, String folder);

    /** Deletes the asset by its provider public id. Best-effort; never throws. */
    void delete(String publicId);
}
