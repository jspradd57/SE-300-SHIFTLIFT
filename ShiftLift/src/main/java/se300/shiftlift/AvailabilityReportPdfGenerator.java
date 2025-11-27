package se300.shiftlift;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Utility class for generating worker availability report PDFs.
 * Shows the number of available workers per day across the entire schedule.
 */
public class AvailabilityReportPdfGenerator {

    private static final float MARGIN = 40;
    private static final float TITLE_FONT_SIZE = 24;
    private static final float SUBTITLE_FONT_SIZE = 14;
    private static final float TABLE_HEADER_FONT_SIZE = 11;
    private static final float TABLE_FONT_SIZE = 10;
    private static final float ROW_HEIGHT = 20;
    private static final float COL_WIDTH = 100;

    /**
     * Generates an availability report PDF for a schedule.
     * 
     * @param schedule The schedule to generate report for
     * @param userService The user service to get all workers
     * @param studentBlockedDateService The service to check student blocked dates
     * @param blockedDateService The service to check manager blocked dates
     * @param outputPath The file path where the PDF should be saved
     * @throws IOException If there's an error writing the PDF
     */
    public static void generateAvailabilityReportPdf(
            Schedule schedule, 
            UserService userService,
            StudentBlockedDateService studentBlockedDateService,
            BlockedDateService blockedDateService,
            String outputPath) throws IOException {

        try (PDDocument document = new PDDocument()) {
            schedule.generateWeeks();
            List<Week> weeks = schedule.getWeeks();
            
            if (weeks.isEmpty()) {
                throw new IllegalArgumentException("Schedule has no weeks to report");
            }

            // Get all student workers
            org.springframework.data.domain.Pageable unpaged = org.springframework.data.domain.Pageable.unpaged();
            List<User> allUsers = userService.list(unpaged);
            List<StudentWorker> studentWorkers = new java.util.ArrayList<>();
            for (User user : allUsers) {
                if (user instanceof StudentWorker) {
                    studentWorkers.add((StudentWorker) user);
                }
            }

            int totalWorkers = studentWorkers.size();

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float yPosition = PDRectangle.A4.getHeight() - MARGIN;
            
            // Draw title
            yPosition = drawTitle(contentStream, yPosition, schedule);
            yPosition -= 30;

            // Draw total workers info
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), SUBTITLE_FONT_SIZE);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Total Student Workers: " + totalWorkers);
            contentStream.endText();
            yPosition -= 30;

            // Process each week
            for (int weekNum = 0; weekNum < weeks.size(); weekNum++) {
                Week week = weeks.get(weekNum);
                
                // Calculate space needed for this week
                float spaceNeeded = 100 + (5 * ROW_HEIGHT); // Header + 5 days
                
                if (yPosition - spaceNeeded < MARGIN) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = PDRectangle.A4.getHeight() - MARGIN;
                }
                
                yPosition = drawWeekAvailability(
                    contentStream, 
                    week, 
                    weekNum + 1, 
                    studentWorkers,
                    studentBlockedDateService,
                    blockedDateService,
                    totalWorkers,
                    yPosition
                );
                yPosition -= 30;
            }
            
            contentStream.close();
            document.save(new File(outputPath));
        }
    }

    /**
     * Draws the main title of the report.
     */
    private static float drawTitle(PDPageContentStream cs, float yPosition, Schedule schedule) throws IOException {
        cs.setNonStrokingColor(new Color(21, 111, 171));
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Worker Availability Report");
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= 30;
        
        // Draw schedule date range
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), SUBTITLE_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText(String.format("Schedule Period: %s - %s", 
            schedule.getStartDate().toString(), 
            schedule.getEndDate().toString()));
        cs.endText();
        
        yPosition -= 10;
        
        cs.setStrokingColor(new Color(21, 111, 171));
        cs.setLineWidth(2);
        cs.moveTo(MARGIN, yPosition);
        cs.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPosition);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
        
        return yPosition - 10;
    }

    /**
     * Draws availability information for a single week.
     */
    private static float drawWeekAvailability(
            PDPageContentStream cs,
            Week week,
            int weekNumber,
            List<StudentWorker> studentWorkers,
            StudentBlockedDateService studentBlockedDateService,
            BlockedDateService blockedDateService,
            int totalWorkers,
            float yPosition) throws IOException {
        
        // Week header
        cs.setNonStrokingColor(new Color(21, 111, 171));
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TABLE_HEADER_FONT_SIZE + 2);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText(String.format("Week %d: %s - %s", 
            weekNumber, 
            week.getWeekStartDate().toString(),
            week.getWeekEndDate().toString()));
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= 25;

        // Table headers
        cs.setNonStrokingColor(new Color(230, 230, 230));
        cs.addRect(MARGIN, yPosition - ROW_HEIGHT + 5, PDRectangle.A4.getWidth() - (2 * MARGIN), ROW_HEIGHT);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);

        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TABLE_HEADER_FONT_SIZE);
        cs.newLineAtOffset(MARGIN + 5, yPosition - 13);
        cs.showText("Day");
        cs.newLineAtOffset(COL_WIDTH, 0);
        cs.showText("Date");
        cs.newLineAtOffset(COL_WIDTH, 0);
        cs.showText("Available Workers");
        cs.newLineAtOffset(COL_WIDTH, 0);
        cs.showText("Unavailable Workers");
        cs.newLineAtOffset(COL_WIDTH, 0);
        cs.showText("Availability %");
        cs.endText();

        yPosition -= ROW_HEIGHT;

        // Draw table rows for each day (Monday-Friday)
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        
        // Generate dates for the week (Monday-Friday)
        java.time.LocalDate weekStart = java.time.LocalDate.of(
            week.getWeekStartDate().get_year(),
            week.getWeekStartDate().get_month(),
            week.getWeekStartDate().get_day()
        );
        
        // Adjust to Monday of the week
        java.time.LocalDate monday = weekStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        
        for (int i = 0; i < 5; i++) {
            java.time.LocalDate currentDay = monday.plusDays(i);
            Date date = new Date(
                currentDay.getDayOfMonth(),
                currentDay.getMonthValue(),
                currentDay.getYear()
            );
            
            // Count available workers
            int unavailableCount = 0;
            
            // Check if manager blocked this date
            boolean managerBlocked = blockedDateService.isDateBlocked(date);
            if (managerBlocked) {
                unavailableCount = totalWorkers; // All workers unavailable if manager blocked
            } else {
                // Count students who blocked this date
                for (StudentWorker student : studentWorkers) {
                    if (studentBlockedDateService.isDateBlocked(student, date)) {
                        unavailableCount++;
                    }
                }
            }
            
            int availableCount = totalWorkers - unavailableCount;
            double availabilityPercent = totalWorkers > 0 ? (availableCount * 100.0 / totalWorkers) : 0;

            // Alternate row colors
            if (i % 2 == 0) {
                cs.setNonStrokingColor(new Color(250, 250, 250));
                cs.addRect(MARGIN, yPosition - ROW_HEIGHT + 5, PDRectangle.A4.getWidth() - (2 * MARGIN), ROW_HEIGHT);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
            }

            // Highlight low availability
            if (availabilityPercent < 50) {
                cs.setNonStrokingColor(new Color(255, 235, 235));
                cs.addRect(MARGIN, yPosition - ROW_HEIGHT + 5, PDRectangle.A4.getWidth() - (2 * MARGIN), ROW_HEIGHT);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
            }

            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), TABLE_FONT_SIZE);
            cs.newLineAtOffset(MARGIN + 5, yPosition - 13);
            cs.showText(dayNames[i]);
            cs.newLineAtOffset(COL_WIDTH, 0);
            cs.showText(date.toString());
            cs.newLineAtOffset(COL_WIDTH, 0);
            
            // Color code availability
            if (managerBlocked) {
                cs.setNonStrokingColor(Color.RED);
                cs.showText("0 (Blocked)");
                cs.setNonStrokingColor(Color.BLACK);
            } else if (availableCount < totalWorkers / 2) {
                cs.setNonStrokingColor(new Color(200, 0, 0));
                cs.showText(String.valueOf(availableCount));
                cs.setNonStrokingColor(Color.BLACK);
            } else {
                cs.showText(String.valueOf(availableCount));
            }
            
            cs.newLineAtOffset(COL_WIDTH, 0);
            cs.showText(String.valueOf(unavailableCount));
            cs.newLineAtOffset(COL_WIDTH, 0);
            cs.showText(String.format("%.1f%%", availabilityPercent));
            cs.endText();

            yPosition -= ROW_HEIGHT;
        }

        return yPosition;
    }
}
