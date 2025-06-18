package com.l2.repository.interfaces;

import com.l2.dto.SparePictureDTO;
import com.l2.dto.SparesDTO;

import java.util.List;

public interface ProductionRepository {
    void insertSpare(SparesDTO sparesDTO);

    void updateSpareAsArchived(SparesDTO sparesDTO);

    boolean existsBySpareItem(String spareItem);

    void appendCommentBySpareItem(String spareItem, String newComment);

    List<SparePictureDTO> findAllSparePictures();
}
