package com.l2.repository.rowmappers;

import com.l2.dto.RangesDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RangesRowMapper implements RowMapper<RangesDTO> {
    @Override
    public RangesDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new RangesDTO(
                rs.getInt("id"),
                rs.getString("range"),
                rs.getString("range_additional"),
                rs.getString("range_type"),
                rs.getString("last_update"),
                rs.getString("last_updated_by")
        );
    }
}
