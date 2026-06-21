-- Track the Cloudinary public id of a vendor's logo so that replacing the logo can
-- delete the previously stored asset (the logo_url alone can't be turned back into a
-- public id for deletion). Nullable: existing vendors simply have no tracked asset yet.
ALTER TABLE vendors ADD COLUMN logo_public_id VARCHAR(255);
