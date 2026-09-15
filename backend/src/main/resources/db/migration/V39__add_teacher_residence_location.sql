-- V39: one optional teacher residence location (province -> ward)
-- This is a display/filter location only; it is not a service-area relation.

ALTER TABLE teacher_profiles
    ADD COLUMN province_code varchar(2),
    ADD COLUMN ward_code varchar(5),
    ADD CONSTRAINT fk_teacher_profiles_province
        FOREIGN KEY (province_code) REFERENCES provinces(code),
    ADD CONSTRAINT fk_teacher_profiles_ward
        FOREIGN KEY (ward_code, province_code) REFERENCES wards(code, province_code),
    ADD CONSTRAINT ck_teacher_profiles_residence_ward_requires_province
        CHECK (ward_code IS NULL OR province_code IS NOT NULL);

CREATE INDEX ix_teacher_profiles_residence_province ON teacher_profiles (province_code);
CREATE INDEX ix_teacher_profiles_residence_ward ON teacher_profiles (province_code, ward_code);
