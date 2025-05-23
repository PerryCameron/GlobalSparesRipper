package com.l2.statictools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class NoteTools {


    private static final Logger logger = LoggerFactory.getLogger(NoteTools.class);

    public static String normalizeDate(String inputDate) {
        try {
            // Parse the input date in MM-DD-YYYY format
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            LocalDate date = LocalDate.parse(inputDate, inputFormatter);


            // Format it into ISO format (YYYY-MM-DD)
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return date.format(outputFormatter);
        } catch (DateTimeParseException e) {
            // If parsing fails, return the original input (it's not a date)
            return inputDate;
        }
    }

}
