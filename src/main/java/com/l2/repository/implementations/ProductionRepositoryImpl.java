package com.l2.repository.implementations;

import com.l2.dto.SparesDTO;
import com.l2.repository.interfaces.ProductionRepository;
import com.l2.statictools.DatabaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class ProductionRepositoryImpl implements ProductionRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProductionRepositoryImpl.class);
    private final JdbcTemplate jdbcTemplate;

    public ProductionRepositoryImpl() {
        this.jdbcTemplate = new JdbcTemplate(DatabaseConnector.getProductionDataSource("Production Repo"));
    }

    @Override
    public void insertSpare(SparesDTO sparesDTO) {
        String sql = """
            INSERT INTO spares (
                pim, spare_item, replacement_item, standard_exchange_item,
                spare_description, catalogue_version, end_of_service_date,
                last_update, added_to_catalogue, removed_from_catalogue,
                comments, keywords, archived, custom_add, last_updated_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                sparesDTO.getPim(),
                sparesDTO.getSpareItem(),
                sparesDTO.getReplacementItem(),
                sparesDTO.getStandardExchangeItem(),
                sparesDTO.getSpareDescription(),
                sparesDTO.getCatalogueVersion(),
                sparesDTO.getProductEndOfServiceDate(),
                sparesDTO.getLastUpdate(),
                sparesDTO.getAddedToCatalogue(),
                sparesDTO.getRemovedFromCatalogue(),
                sparesDTO.getComments(),
                sparesDTO.getKeywords(),
                sparesDTO.getArchived() != null && sparesDTO.getArchived() ? 1 : 0,
                sparesDTO.getCustomAdd() != null && sparesDTO.getCustomAdd() ? 1 : 0,
                sparesDTO.getLastUpdatedBy()
        );
    }
}
