package com.l2.repository.implementations;

import com.l2.dto.SparePictureDTO;
import com.l2.dto.SparesDTO;
import com.l2.repository.interfaces.ChangeSetRepository;
import com.l2.repository.rowmappers.SparePictureRowMapper;
import com.l2.repository.rowmappers.SparesRowMapper;
import com.l2.statictools.DatabaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ChangeSetRepositoryImpl implements ChangeSetRepository {
    private static final Logger logger = LoggerFactory.getLogger(ChangeSetRepositoryImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ChangeSetRepositoryImpl() {
        this.jdbcTemplate = new JdbcTemplate(DatabaseConnector.getChangeSetDataSource("Change Set Repo"));
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public List<String> getSpareItems() {
        String sql = "SELECT spare_item FROM spares";
        List<String> spareItems = jdbcTemplate.queryForList(sql, String.class);
        logger.info("Found {} spare_item values in new database", spareItems.size());
        return spareItems;
    }

    public List<SparesDTO> getAllSpares() {
        String sql = "SELECT * FROM spares";
        List<SparesDTO> spares = namedParameterJdbcTemplate.query(sql, new SparesRowMapper());
        logger.info("Retrieved {} SparesDTO objects from new database", spares.size());
        return spares;
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

    @Override
    public long countSpares() {
        try {
            String sql = "SELECT COUNT(*) FROM spares";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error counting records in spares table", e);
            throw new RuntimeException("Failed to count spares", e);
        }
    }

    @Override
    public int addSpares(List<SparesDTO> spares) {
        if (spares == null || spares.isEmpty()) {
            logger.info("No spares provided to add to production database.");
            return 0;
        }

        String sql = "INSERT INTO spares (pim, spare_item, replacement_item, standard_exchange_item, " +
                "spare_description, catalogue_version, end_of_service_date, last_update, " +
                "added_to_catalogue, removed_from_catalogue, comments, keywords, archived, " +
                "custom_add, last_updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int[] rowsAffected = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SparesDTO spare = spares.get(i);
                ps.setString(1, spare.getPim());
                ps.setString(2, spare.getSpareItem());
                ps.setString(3, spare.getReplacementItem());
                ps.setString(4, spare.getStandardExchangeItem());
                ps.setString(5, spare.getSpareDescription());
                ps.setString(6, spare.getCatalogueVersion());
                ps.setString(7, spare.getProductEndOfServiceDate());
                ps.setString(8, spare.getLastUpdate());
                ps.setString(9, spare.getAddedToCatalogue());
                ps.setString(10, spare.getRemovedFromCatalogue());
                ps.setString(11, spare.getComments());
                ps.setString(12, spare.getKeywords());
                ps.setInt(13, spare.getArchived() != null && spare.getArchived() ? 1 : 0);
                ps.setInt(14, spare.getCustomAdd() != null && spare.getCustomAdd() ? 1 : 0);
                ps.setString(15, spare.getLastUpdatedBy());
            }

            @Override
            public int getBatchSize() {
                return spares.size();
            }
        });

        int totalRowsAffected = Arrays.stream(rowsAffected).sum();
        logger.info("Inserted {} spares into production database.", totalRowsAffected);
        return totalRowsAffected;
    }

    @Override
    public int updateSpares(List<SparesDTO> spares) {
        if (spares == null || spares.isEmpty()) {
            logger.info("No spares provided to update in production database.");
            return 0;
        }

        String sql = "UPDATE spares SET pim = ?, replacement_item = ?, standard_exchange_item = ?, " +
                "spare_description = ?, catalogue_version = ?, end_of_service_date = ?, " +
                "last_update = ?, added_to_catalogue = ?, removed_from_catalogue = ?, " +
                "comments = ?, keywords = ?, archived = ?, custom_add = ?, last_updated_by = ? " +
                "WHERE spare_item = ?";

        int[] rowsAffected = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SparesDTO spare = spares.get(i);
                ps.setString(1, spare.getPim());
                ps.setString(2, spare.getReplacementItem());
                ps.setString(3, spare.getStandardExchangeItem());
                ps.setString(4, spare.getSpareDescription());
                ps.setString(5, spare.getCatalogueVersion());
                ps.setString(6, spare.getProductEndOfServiceDate());
                ps.setString(7, spare.getLastUpdate());
                ps.setString(8, spare.getAddedToCatalogue());
                ps.setString(9, spare.getRemovedFromCatalogue());
                ps.setString(10, spare.getComments());
                ps.setString(11, spare.getKeywords());
                ps.setInt(12, spare.getArchived() != null && spare.getArchived() ? 1 : 0);
                ps.setInt(13, spare.getCustomAdd() != null && spare.getCustomAdd() ? 1 : 0);
                ps.setString(14, spare.getLastUpdatedBy());
                ps.setString(15, spare.getSpareItem());
            }

            @Override
            public int getBatchSize() {
                return spares.size();
            }
        });

        int totalRowsAffected = Arrays.stream(rowsAffected).sum();
        logger.info("Updated {} spares in production database.", totalRowsAffected);
        return totalRowsAffected;
    }

    @Override
    public boolean existsBySpareName(String spareName) {
        String sql = "SELECT COUNT(*) FROM spare_pictures WHERE spare_name = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, spareName);
        return count != null && count > 0;
    }

    @Override
    public SparePictureDTO getPictureBySpareName(String spareName) {
        String sql = "SELECT * FROM spare_pictures WHERE spare_name = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new SparePictureRowMapper(), spareName);
        } catch (EmptyResultDataAccessException e) {
            return null; // or throw new SparePictureNotFoundException("Spare picture not found for spare_name: " + spareName);
        }
    }

    @Override
    public long insertSparePicture(SparePictureDTO sparePictureDTO) {
        String sql = "INSERT INTO spare_pictures (spare_name, picture) VALUES (?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, sparePictureDTO.getSpareName());
            ps.setBytes(2, sparePictureDTO.getPicture());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

}
