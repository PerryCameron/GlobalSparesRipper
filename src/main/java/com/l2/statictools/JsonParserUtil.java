package com.l2.statictools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.l2.GSChangeSet;
import com.l2.dto.UpdatedByDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonParserUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonParserUtil.class);

    public static List<UpdatedByDTO> parseUpdatedByJson(String json)  {
        try {
            // Define the collection type for List<UpdatedByDTO>
            CollectionType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, UpdatedByDTO.class);

            // Deserialize the JSON string into a List<UpdatedByDTO>
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            logger.error("Failed to parse JSON: {}", e.getMessage(), e);
        }
        return List.of();
    }
}