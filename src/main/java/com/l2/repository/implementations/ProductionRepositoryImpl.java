package com.l2.repository.implementations;

import com.l2.dto.SparePictureDTO;
import com.l2.dto.SparesDTO;
import com.l2.repository.interfaces.ProductionRepository;
import com.l2.repository.rowmappers.SparePictureRowMapper;
import com.l2.statictools.DatabaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

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

    @Override
    public void updateSpareAsArchived(SparesDTO sparesDTO) {
        String sql = """
            UPDATE spares
            SET last_update = ?,
                removed_from_catalogue = ?,
                archived = ?
            WHERE id = ?
            """;

        jdbcTemplate.update(sql,
                sparesDTO.getRemovedFromCatalogue(),
                sparesDTO.getRemovedFromCatalogue(),
                sparesDTO.getArchived() != null && sparesDTO.getArchived() ? 1 : 0,
                sparesDTO.getId()
        );
    }

    @Override
    public boolean existsBySpareItem(String spareItem) {
        String sql = """
            SELECT COUNT(*) 
            FROM spares 
            WHERE spare_item = ?
            """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, spareItem);
        return count != null && count > 0;
    }

    @Override
    public void appendCommentBySpareItem(String spareItem, String newComment) {
        String sql = """
        UPDATE spares
        SET comments = COALESCE(comments, '') || ? || ?
        WHERE spare_item = ?
        """;
        jdbcTemplate.update(sql, newComment, "\r\n", spareItem);
    }

    @Override
    public List<SparePictureDTO> findAllSparePictures() {
        String sql = """
        SELECT sp.id, sp.spare_id, s.spare_item AS spare_name, sp.picture
        FROM spare_pictures sp
        INNER JOIN spares s ON s.id = sp.spare_id
        """;
        try {
            return jdbcTemplate.query(sql, new SparePictureRowMapper());
        } catch (DataAccessException e) {
            logger.error("Error retrieving spare pictures", e);
            throw new RuntimeException("Failed to retrieve spare pictures", e);
        }
    }

}
