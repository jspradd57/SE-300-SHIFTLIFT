package se300.shiftlift;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Utility class for generating hours report PDFs showing total hours per user for each week.
 */
public class HoursReportPdfGenerator {

    private static final float MARGIN = 40;
    private static final float TITLE_FONT_SIZE = 24;
    private static final float WEEK_HEADER_FONT_SIZE = 16;
    private static final float TABLE_HEADER_FONT_SIZE = 11;
    private static final float TABLE_FONT_SIZE = 10;
    private static final float ROW_HEIGHT = 20;

    /**
     * Generates a hours report PDF for a published schedule.
     * 
     * @param schedule The schedule to generate report for (must be approved/published)
     * @param outputPath The file path where the PDF should be saved
     * @throws IOException If there's an error writing the PDF
     * @throws IllegalArgumentException If the schedule is not approved
     */
    public static void generateHoursReportPdf(Schedule schedule, String outputPath) throws IOException {
        if (schedule.getApproved() == null || !schedule.getApproved()) {
            throw new IllegalArgumentException("Can only generate PDF for published (approved) schedules");
        }

        try (PDDocument document = new PDDocument()) {
            schedule.generateWeeks();
            schedule.organizeShiftsIntoWeeks();

            List<Week> weeks = schedule.getWeeks();
            
            if (weeks.isEmpty()) {
                throw new IllegalArgumentException("Schedule has no weeks to report");
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float yPosition = PDRectangle.A4.getHeight() - MARGIN;
            
            yPosition = drawMainTitle(contentStream, yPosition);
            yPosition -= 20;
            
            for (int i = 0; i < weeks.size(); i++) {
                Week week = weeks.get(i);
                
                Map<String, Float> userHours = calculateUserHours(week);
                
                float spaceNeeded = 80 + (userHours.size() * ROW_HEIGHT);
                
                if (yPosition - spaceNeeded < MARGIN) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = PDRectangle.A4.getHeight() - MARGIN;
                }
                
                yPosition = drawWeekSection(contentStream, week, i + 1, userHours, yPosition);
                yPosition -= 30;
            }
            
            contentStream.close();
            document.save(new File(outputPath));
        }
    }

    /**
     * Draws the main title of the report.
     */
    private static float drawMainTitle(PDPageContentStream cs, float yPosition) throws IOException {
        cs.setNonStrokingColor(new Color(21, 111, 171));
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Hours Report by Week");
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= 30;
        
        cs.setStrokingColor(new Color(21, 111, 171));
        cs.setLineWidth(2);
        cs.moveTo(MARGIN, yPosition);
        cs.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPosition);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(1);
        
        return yPosition - 10;
    }

    /**
     * Draws a week section with hours table.
     */
    private static float drawWeekSection(PDPageContentStream cs, Week week, int weekNumber, 
                                         Map<String, Float> userHours, float yPosition) throws IOException {
        float pageWidth = PDRectangle.A4.getWidth();
        
        cs.setNonStrokingColor(new Color(21, 111, 171));
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), WEEK_HEADER_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Week " + weekNumber + ": " + week.getWeekRangeString());
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= 30;
        
        cs.setNonStrokingColor(new Color(21, 111, 171));
        cs.addRect(MARGIN, yPosition - ROW_HEIGHT, pageWidth - (2 * MARGIN), ROW_HEIGHT);
        cs.fill();
        
        cs.setNonStrokingColor(Color.WHITE);
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TABLE_HEADER_FONT_SIZE);
        cs.newLineAtOffset(MARGIN + 10, yPosition - 14);
        cs.showText("Student Worker");
        cs.endText();
        
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TABLE_HEADER_FONT_SIZE);
        cs.newLineAtOffset(pageWidth - MARGIN - 100, yPosition - 14);
        cs.showText("Total Hours");
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= ROW_HEIGHT;
        
        List<Map.Entry<String, Float>> sortedUsers = new ArrayList<>(userHours.entrySet());
        sortedUsers.sort(Map.Entry.comparingByKey());
        
        boolean alternateRow = false;
        float totalHours = 0f;
        
        for (Map.Entry<String, Float> entry : sortedUsers) {
            if (alternateRow) {
                cs.setNonStrokingColor(new Color(245, 245, 245));
                cs.addRect(MARGIN, yPosition - ROW_HEIGHT, pageWidth - (2 * MARGIN), ROW_HEIGHT);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
            }
            
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), TABLE_FONT_SIZE);
            cs.newLineAtOffset(MARGIN + 10, yPosition - 14);
            cs.showText(entry.getKey());
            cs.endText();
            
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), TABLE_FONT_SIZE);
            cs.newLineAtOffset(pageWidth - MARGIN - 80, yPosition - 14);
            cs.showText(String.format("%.1f", entry.getValue()));
            cs.endText();
            
            totalHours += entry.getValue();
            yPosition -= ROW_HEIGHT;
            alternateRow = !alternateRow;
        }
        
        cs.setNonStrokingColor(new Color(21, 111, 171));
        cs.addRect(MARGIN, yPosition - ROW_HEIGHT, pageWidth - (2 * MARGIN), ROW_HEIGHT);
        cs.fill();
        
        cs.setNonStrokingColor(Color.WHITE);
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TABLE_HEADER_FONT_SIZE);
        cs.newLineAtOffset(MARGIN + 10, yPosition - 14);
        cs.showText("Total");
        cs.endText();
        
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TABLE_HEADER_FONT_SIZE);
        cs.newLineAtOffset(pageWidth - MARGIN - 80, yPosition - 14);
        cs.showText(String.format("%.1f", totalHours));
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= ROW_HEIGHT;
        
        return yPosition;
    }

    /**
     * Calculates total hours scheduled for each user in a week.
     */
    private static Map<String, Float> calculateUserHours(Week week) {
        Map<String, Float> userHours = new HashMap<>();
        
        for (Shift shift : week.getShifts()) {
            if (shift.getStudentWorker() != null) {
                String userName = shift.getStudentWorker().getUsername();
                
                float hours = (float) shift.getTime().getDurationInHours();
                userHours.put(userName, userHours.getOrDefault(userName, 0f) + hours);
            }
        }
        
        return userHours;
    }
}
