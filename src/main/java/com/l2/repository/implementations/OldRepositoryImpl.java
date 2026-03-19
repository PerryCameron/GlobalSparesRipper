package com.l2.repository.implementations;

import com.l2.dto.SparesDTO;
import com.l2.repository.interfaces.OldRepository;
import com.l2.repository.rowmappers.SparesRowMapper;
import com.l2.statictools.DatabaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OldRepositoryImpl implements OldRepository {
    private static final Logger logger = LoggerFactory.getLogger(OldRepositoryImpl.class);
    private JdbcTemplate jdbcTemplate =null;
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    public OldRepositoryImpl() {
        Optional<SQLiteDataSource> dataSource = DatabaseConnector.getOldSparesDataSource("Old Spares Repo");
        if (dataSource.isPresent()) {
            this.jdbcTemplate = new JdbcTemplate(dataSource.get());
        }
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
    }

    @Override
    public Map<String, Integer> countArchived() {
        Map<String, Integer> globalSpares = new HashMap<>();
        String sql = "SELECT spare_item, archived FROM spares";
        try {
            jdbcTemplate.query(sql, rs -> {
                globalSpares.put(rs.getString("spare_item"), rs.getInt("archived"));
            });
        }  catch (Exception e) {
            logger.error(e.getMessage());
        }
        return globalSpares;
    }

    @Override
    public Map<String, SparesDTO> getAllBySpareItem() {
        Map<String, SparesDTO> spares = new HashMap<>();
        String sql = "SELECT * FROM spares WHERE custom_add = 0"; // we don't want to compare custom adds
        try {
            spares = jdbcTemplate.query(sql, new SparesRowMapper())
                    .stream()
                    .collect(Collectors.toMap(SparesDTO::getSpareItem, dto -> dto));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return spares;
    }
}
