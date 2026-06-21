package com.loyalsuit.common.media;

/**
 * Abstraction over the external media store (Cloudinary). Object storage is a
 * cross-cutting capability — product images, vendor logos, proof-of-delivery photos
 * all need it — so the port lives in {@code common}. Keeping it behind a port lets
 * application code be unit-tested without network calls and isolates the vendor SDK
 * to one adapter.
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
