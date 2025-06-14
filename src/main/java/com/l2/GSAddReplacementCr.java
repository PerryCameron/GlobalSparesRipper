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
        int count = 0;
        for (ReplacementCrDTO replacementCrDTO : replacements) {
            count++;
            String replacement = replacementCrDTO.getReplacement();
            if (replacement == null || replacement.trim().isEmpty()) {
                continue;
            }
            boolean exists = globalSparesRepository.existsBySpareItem(replacement);
            if (exists) {
                String note = "Old P/N: " + replacementCrDTO.getItem() + ", " + replacementCrDTO.getComment();
                System.out.println("Updating " + replacement + " note as: " + note);
              globalSparesRepository.appendCommentBySpareItem(replacementCrDTO.getReplacement(), note);
            } else {
                System.out.println("No match found for spare_item: " + replacement);
            }
        }
        System.out.println("Number of replacements: " + count);
    }

}
