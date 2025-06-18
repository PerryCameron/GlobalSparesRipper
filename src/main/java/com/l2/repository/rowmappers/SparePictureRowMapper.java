package com.l2.repository.rowmappers;

import com.l2.dto.SparePictureDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SparePictureRowMapper implements RowMapper<SparePictureDTO> {
    @Override
    public SparePictureDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SparePictureDTO(
                rs.getLong("id"),
                rs.getLong("spare_id"),
                rs.getString("spare_name"),
                rs.getBytes("picture")
        );
    }
}