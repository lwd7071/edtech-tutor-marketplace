package com.edtech.platform.catalog.repository;

import com.edtech.platform.catalog.dto.AdministrativeProvince;
import com.edtech.platform.catalog.dto.AdministrativeWard;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdministrativeLocationRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<AdministrativeProvince> findActiveProvinces() {
        return jdbcTemplate.query("SELECT code, name FROM provinces WHERE is_active = true ORDER BY name, code",
                (rs, rowNum) -> new AdministrativeProvince(rs.getString("code"), rs.getString("name")));
    }

    public List<AdministrativeWard> findActiveWards(String provinceCode) {
        return jdbcTemplate.query("SELECT code, province_code, name FROM wards WHERE province_code = ? AND is_active = true ORDER BY name, code",
                (rs, rowNum) -> new AdministrativeWard(rs.getString("code"), rs.getString("province_code"), rs.getString("name")), provinceCode);
    }

    public String findProvinceName(String code) {
        return jdbcTemplate.query("select name from provinces where code = ? and is_active = true", rs -> rs.next() ? rs.getString(1) : null, code);
    }

    public String findWardName(String code, String provinceCode) {
        return jdbcTemplate.query("select name from wards where code = ? and province_code = ? and is_active = true", rs -> rs.next() ? rs.getString(1) : null, code, provinceCode);
    }
}
