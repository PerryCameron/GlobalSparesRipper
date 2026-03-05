package com.l2.repository.interfaces;

import com.l2.dto.SparesDTO;

import java.util.Map;

public interface OldRepository {
    Map<String, Integer>  countArchived();

    Map<String, SparesDTO> getAllBySpareItem();
}
