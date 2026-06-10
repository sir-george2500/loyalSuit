-- Proof of delivery (Phase 7d). Captured when a delivery is completed: who received it,
-- an optional note, and an optional proof image URL (signature/photo uploaded to
-- Cloudinary). All nullable — a delivery has no proof until it's marked delivered.
ALTER TABLE deliveries ADD COLUMN recipient_name  VARCHAR(255);
ALTER TABLE deliveries ADD COLUMN pod_note        VARCHAR(500);
ALTER TABLE deliveries ADD COLUMN proof_image_url VARCHAR(500);
