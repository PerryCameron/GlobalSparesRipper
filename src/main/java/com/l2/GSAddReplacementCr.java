package com.l2;

import com.l2.dto.ReplacementCrDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.implementations.ProductionRepositoryImpl;

import java.util.List;

public class GSAddReplacementCr {
    // purpose of this class is to put the old CR's in notes for searching

    public static void main(String[] args) {
        addReplacmentCRstoNotes();
    }

    private static void addReplacmentCRstoNotes() {
        GlobalSparesRepositoryImpl globalSparesRepository = new GlobalSparesRepositoryImpl();
        ProductionRepositoryImpl productionRepository = new ProductionRepositoryImpl();

        List<ReplacementCrDTO> replacements = globalSparesRepository.findAllReplacementCr();
        for (ReplacementCrDTO replacementCrDTO : replacements) {
            String replacement = replacementCrDTO.getReplacement();
            if (replacement == null || replacement.trim().isEmpty()) {
                continue;
            }
            boolean exists = productionRepository.existsBySpareItem(replacement);
            if (exists) {
                System.out.println("Updating " + replacement);
              productionRepository.appendCommentBySpareItem(replacementCrDTO.getReplacement(), "Old P/N: "
                      + replacement + ", "
                      + replacementCrDTO.getComment());
            } else {
                System.out.println("No match found for spare_item: " + replacement);
            }
        }
    }

}
