package com.l2.repository.implementations;

import com.l2.repository.interfaces.GlobalSparesRepository;
import com.l2.repository.interfaces.OldRepository;
import com.l2.statictools.DatabaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class OldRepositoryImpl implements OldRepository {
        private static final Logger logger = LoggerFactory.getLogger(OldRepositoryImpl.class);
        private final JdbcTemplate jdbcTemplate;
        NamedParameterJdbcTemplate namedParameterJdbcTemplate;


        public OldRepositoryImpl() {
            this.jdbcTemplate = new JdbcTemplate(DatabaseConnector.getOldSparesDataSource("Old Spares Repo"));
            this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
        }
}
