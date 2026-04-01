package com.l2.repository.interfaces;

import com.l2.dto.SparePictureDTO;
import com.l2.dto.SparesDTO;

import java.util.List;

public interface ProductionRepository {
    List<SparesDTO> getCustomAddedSpares();

    List<String> getSpareItems();

    void insertSpare(SparesDTO sparesDTO);

    void updateSpareAsArchived(SparesDTO sparesDTO);

    boolean existsBySpareItem(String spareItem);

    void appendCommentBySpareItem(String spareItem, String newComment);

    List<SparePictureDTO> findAllSparePictures();

    long countSpares();

    int addSpares(List<SparesDTO> spares);

    int updateSpares(List<SparesDTO> spares);

    SparesDTO getBySpareItem(String spareItem);

    boolean existsBySpareName(String spareName);

    long insertSparePicture(SparePictureDTO sparePictureDTO);

    SparePictureDTO getPictureBySpareName(String spareName);

    int updateSpare(SparesDTO spare);

    List<SparesDTO> getAllSparesWithKeywords();

    List<SparePictureDTO> getAllSparePictures();

    int countSparePictures();
}
