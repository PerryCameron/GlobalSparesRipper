package com.l2.repository.rowmappers;

import com.l2.dto.ReplacementCrDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReplacementCrRowMapper implements RowMapper<ReplacementCrDTO> {
    @Override
    public ReplacementCrDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReplacementCrDTO(
                rs.getInt("id"),
                rs.getString("item"),
                rs.getString("replacement"),
                rs.getString("comment"),
                rs.getDouble("old_qty"),
                rs.getDouble("new_qty"),
                rs.getString("last_update"),
                rs.getString("last_updated_by")
        );
    }
}
