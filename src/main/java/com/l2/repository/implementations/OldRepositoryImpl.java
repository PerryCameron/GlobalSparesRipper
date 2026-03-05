package com.l2.repository.implementations;

import com.l2.dto.SparesDTO;
import com.l2.repository.interfaces.OldRepository;
import com.l2.repository.rowmappers.SparesRowMapper;
import com.l2.statictools.DatabaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OldRepositoryImpl implements OldRepository {
    private static final Logger logger = LoggerFactory.getLogger(OldRepositoryImpl.class);
    private final JdbcTemplate jdbcTemplate;
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    public OldRepositoryImpl() {
        this.jdbcTemplate = new JdbcTemplate(DatabaseConnector.getOldSparesDataSource("Old Spares Repo"));
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
    }

    @Override
    public Map<String, Integer> countArchived() {
        Map<String, Integer> globalSpares = new HashMap<>();
        String sql = "SELECT spare_item, archived FROM spares";
        jdbcTemplate.query(sql, rs -> {
            globalSpares.put(rs.getString("spare_item"), rs.getInt("archived"));
        });
        return globalSpares;
    }

    @Override
    public Map<String, SparesDTO> getAllBySpareItem() {
        String sql = "SELECT * FROM spares";
        return jdbcTemplate.query(sql, new SparesRowMapper())
                .stream()
                .collect(Collectors.toMap(SparesDTO::getSpareItem, dto -> dto));
    }
}
