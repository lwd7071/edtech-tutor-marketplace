-- Normalize known legacy labels while preserving unknown values for investigation.
UPDATE subjects SET education_level = CASE
    WHEN education_level IN ('PRIMARY_SCHOOL', 'PRIMARY', 'Cấp 1', 'Tiểu học') THEN 'ELEMENTARY'
    WHEN education_level IN ('SECONDARY', 'SECONDARY_SCHOOL', 'Cấp 2', 'THCS') THEN 'MIDDLE_SCHOOL'
    WHEN education_level IN ('Cấp 3', 'THPT') THEN 'HIGH_SCHOOL'
    WHEN education_level IN ('Đại học') THEN 'UNIVERSITY'
    WHEN education_level IN ('Khác') THEN 'OTHER'
    ELSE education_level
END
WHERE education_level IS NOT NULL
  AND education_level NOT IN ('ELEMENTARY', 'MIDDLE_SCHOOL', 'HIGH_SCHOOL', 'UNIVERSITY', 'OTHER');
