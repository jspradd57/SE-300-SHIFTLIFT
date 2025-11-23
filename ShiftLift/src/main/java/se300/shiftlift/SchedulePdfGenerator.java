package se300.shiftlift;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Utility class for generating graphical PDF calendars of published schedules using Apache PDFBox.
 * This class creates professionally formatted PDF files with a visual calendar grid layout.
 */
public class SchedulePdfGenerator {

    private static final float MARGIN = 30;
    private static final float TITLE_FONT_SIZE = 20;
    private static final float HEADER_FONT_SIZE = 12;
    private static final float DAY_HEADER_FONT_SIZE = 10;
    private static final float SHIFT_FONT_SIZE = 7;
    private static final float DATE_FONT_SIZE = 12;
    
    // Calendar grid dimensions
    private static final int DAYS_IN_WEEK = 5; // Monday through Friday only
    private static final float CELL_PADDING = 10;
    private static final float SHIFT_LINE_HEIGHT = 12;

    /**
     * Generates a PDF document for a published schedule and saves it to the specified file path.
     * Creates a graphical calendar view with a grid layout.
     * 
     * @param schedule The schedule to generate PDF for (must be approved/published)
     * @param outputPath The file path where the PDF should be saved
     * @throws IOException If there's an error writing the PDF
     * @throws IllegalArgumentException If the schedule is not approved
     */
    public static void generateSchedulePdf(Schedule schedule, String outputPath) throws IOException {
        if (schedule.getApproved() == null || !schedule.getApproved()) {
            throw new IllegalArgumentException("Can only generate PDF for published (approved) schedules");
        }

        try (PDDocument document = new PDDocument()) {
            // Ensure weeks are generated and shifts are organized
            schedule.generateWeeks();
            schedule.organizeShiftsIntoWeeks();

            List<Week> weeks = schedule.getWeeks();
            
            // Build workstation color map - assign index to each unique workstation
            Map<Long, Integer> workstationColorMap = buildWorkstationColorMap(weeks);
            
            // Create a page for each week
            for (int i = 0; i < weeks.size(); i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    drawWeekCalendar(contentStream, weeks.get(i), i + 1, schedule, workstationColorMap);
                }
            }

            document.save(new File(outputPath));
        }
    }

    /**
     * Builds a map of workstation IDs to color indices (0-4 cycling through 5 colors).
     */
    private static Map<Long, Integer> buildWorkstationColorMap(List<Week> weeks) {
        Map<Long, Integer> colorMap = new HashMap<>();
        List<Workstation> uniqueWorkstations = new ArrayList<>();
        
        // Collect all unique workstations from all shifts
        for (Week week : weeks) {
            for (Shift shift : week.getShifts()) {
                Workstation ws = shift.getWorkstation();
                if (ws != null && !uniqueWorkstations.stream().anyMatch(w -> w.getId().equals(ws.getId()))) {
                    uniqueWorkstations.add(ws);
                }
            }
        }
        
        // Assign index to each workstation
        for (int i = 0; i < uniqueWorkstations.size(); i++) {
            colorMap.put(uniqueWorkstations.get(i).getId(), i);
        }
        
        return colorMap;
    }

    /**
     * Draws a complete week calendar on a single page.
     */
    private static void drawWeekCalendar(PDPageContentStream cs, Week week, int weekNumber, Schedule schedule, Map<Long, Integer> workstationColorMap) throws IOException {
        float pageWidth = PDRectangle.A4.getWidth();
        float pageHeight = PDRectangle.A4.getHeight();
        
        // Draw title
        float yPosition = pageHeight - MARGIN;
        yPosition = drawTitle(cs, schedule, weekNumber, week, yPosition);
        
        // Calculate grid dimensions
        float gridWidth = pageWidth - (2 * MARGIN);
        float cellWidth = gridWidth / DAYS_IN_WEEK;
        float headerHeight = 30;
        float gridStartY = yPosition - 20;
        
        // Reserve space for legend at bottom
        float legendHeight = 100;
        float availableHeight = gridStartY - MARGIN - headerHeight - legendHeight;
        float cellHeight = availableHeight;
        
        // Draw day headers (Sun, Mon, Tue, etc.)
        drawDayHeaders(cs, MARGIN, gridStartY, cellWidth, headerHeight);
        
        // Draw calendar grid and shifts
        drawWeekGrid(cs, week, MARGIN, gridStartY - headerHeight, cellWidth, cellHeight, workstationColorMap);
        
        // Draw workstation color legend at bottom of page
        float legendY = MARGIN + legendHeight - 20; // Position near bottom
        drawColorLegend(cs, week, MARGIN, legendY, pageWidth - (2 * MARGIN), workstationColorMap);
    }

    /**
     * Draws the title section with schedule info and week number.
     */
    private static float drawTitle(PDPageContentStream cs, Schedule schedule, int weekNumber, Week week, float yPosition) throws IOException {
        // Main title
        cs.setNonStrokingColor(new Color(21, 111, 171)); // Blue color
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Work Schedule - Week " + weekNumber);
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= 25;
        
        // Date range
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), HEADER_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText(week.getWeekRangeString());
        cs.endText();
        
        yPosition -= 20;
        
        // Draw separator line
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
     * Draws the day headers (Monday through Friday).
     */
    private static void drawDayHeaders(PDPageContentStream cs, float x, float y, float cellWidth, float headerHeight) throws IOException {
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        
        cs.setNonStrokingColor(new Color(21, 111, 171));
        
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            float cellX = x + (i * cellWidth);
            
            // Draw header background
            cs.addRect(cellX, y - headerHeight, cellWidth, headerHeight);
            cs.fill();
            
            // Draw day name
            cs.setNonStrokingColor(Color.WHITE);
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), DAY_HEADER_FONT_SIZE);
            float textWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                .getStringWidth(dayNames[i]) / 1000 * DAY_HEADER_FONT_SIZE;
            cs.newLineAtOffset(cellX + (cellWidth - textWidth) / 2, y - headerHeight + 10);
            cs.showText(dayNames[i]);
            cs.endText();
            
            cs.setNonStrokingColor(new Color(21, 111, 171));
        }
        
        cs.setNonStrokingColor(Color.BLACK);
        
        // Draw grid lines for headers
        cs.setStrokingColor(Color.WHITE);
        cs.setLineWidth(2);
        for (int i = 0; i <= DAYS_IN_WEEK; i++) {
            float lineX = x + (i * cellWidth);
            cs.moveTo(lineX, y);
            cs.lineTo(lineX, y - headerHeight);
            cs.stroke();
        }
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(1);
    }

    /**
     * Draws the week grid with shifts.
     */
    private static void drawWeekGrid(PDPageContentStream cs, Week week, float x, float y, float cellWidth, float cellHeight, Map<Long, Integer> workstationColorMap) throws IOException {
        // Group shifts by date
        Map<LocalDate, List<Shift>> shiftsByDate = new HashMap<>();
        for (Shift shift : week.getShifts()) {
            LocalDate date = LocalDate.of(
                shift.getDate().get_year(),
                shift.getDate().get_month(),
                shift.getDate().get_day()
            );
            shiftsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(shift);
        }
        
        // Get week start date (Sunday)
        LocalDate weekStart = getWeekStartDate(week);
        
        // Draw each day cell
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            LocalDate currentDate = weekStart.plusDays(i);
            float cellX = x + (i * cellWidth);
            
            // Draw cell border
            cs.setStrokingColor(new Color(200, 200, 200));
            cs.addRect(cellX, y - cellHeight, cellWidth, cellHeight);
            cs.stroke();
            
            // Draw date number
            cs.setNonStrokingColor(new Color(100, 100, 100));
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), DATE_FONT_SIZE);
            cs.newLineAtOffset(cellX + CELL_PADDING, y - CELL_PADDING - DATE_FONT_SIZE);
            cs.showText(String.valueOf(currentDate.getDayOfMonth()));
            cs.endText();
            cs.setNonStrokingColor(Color.BLACK);
            
            // Draw shifts for this day
            List<Shift> dayShifts = shiftsByDate.get(currentDate);
            if (dayShifts != null) {
                dayShifts.sort(Comparator.comparingInt(s -> s.getTime().getStart_time()));
                drawShiftsInCell(cs, dayShifts, cellX, y, cellWidth, cellHeight, workstationColorMap);
            }
        }
    }

    /**
     * Draws all shifts within a single day cell.
     * Shifts are grouped by workstation and sorted by start time within each group.
     */
    private static void drawShiftsInCell(PDPageContentStream cs, List<Shift> shifts, float cellX, float cellY, float cellWidth, float cellHeight, Map<Long, Integer> workstationColorMap) throws IOException {
        float shiftY = cellY - CELL_PADDING - DATE_FONT_SIZE - 15;
        float maxShiftsToShow = (int) ((cellHeight - CELL_PADDING - DATE_FONT_SIZE - 20) / (SHIFT_LINE_HEIGHT * 2)); // Each shift takes 2 lines now
        
        // Group shifts by workstation, then sort each group by start time
        Map<Long, List<Shift>> shiftsByWorkstation = new HashMap<>();
        for (Shift shift : shifts) {
            Long workstationId = shift.getWorkstation() != null ? shift.getWorkstation().getId() : -1L;
            shiftsByWorkstation.computeIfAbsent(workstationId, k -> new ArrayList<>()).add(shift);
        }
        
        // Sort shifts within each workstation by start time
        for (List<Shift> workstationShifts : shiftsByWorkstation.values()) {
            workstationShifts.sort(Comparator.comparingInt(s -> s.getTime().getStart_time()));
        }
        
        // Get workstations in sorted order (by workstation name for consistency)
        List<Long> sortedWorkstationIds = new ArrayList<>(shiftsByWorkstation.keySet());
        sortedWorkstationIds.sort((id1, id2) -> {
            if (id1.equals(-1L)) return 1; // Put null workstations last
            if (id2.equals(-1L)) return -1;
            
            // Find the workstation names to compare
            Shift shift1 = shiftsByWorkstation.get(id1).get(0);
            Shift shift2 = shiftsByWorkstation.get(id2).get(0);
            String name1 = shift1.getWorkstation() != null ? shift1.getWorkstation().getName() : "ZZZ";
            String name2 = shift2.getWorkstation() != null ? shift2.getWorkstation().getName() : "ZZZ";
            return name1.compareTo(name2);
        });
        
        int shiftCount = 0;
        
        // Draw shifts grouped by workstation
        for (Long workstationId : sortedWorkstationIds) {
            List<Shift> workstationShifts = shiftsByWorkstation.get(workstationId);
            
            for (Shift shift : workstationShifts) {
                if (shiftCount >= maxShiftsToShow) {
                    // Show "+X more" if there are too many shifts
                    int remaining = shifts.size() - shiftCount;
                    cs.setNonStrokingColor(new Color(150, 150, 150));
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), SHIFT_FONT_SIZE - 1);
                    cs.newLineAtOffset(cellX + CELL_PADDING, shiftY);
                    cs.showText("+" + remaining + " more...");
                    cs.endText();
                    cs.setNonStrokingColor(Color.BLACK);
                    return;
                }
                
                // Draw shift box with color based on workstation
                Color workstationColor = getWorkstationColor(shift.getWorkstation(), workstationColorMap);
                cs.setNonStrokingColor(workstationColor);
                float boxHeight = (SHIFT_LINE_HEIGHT * 2.5f);
                float boxWidth = cellWidth - (2 * CELL_PADDING);
                cs.addRect(cellX + CELL_PADDING, shiftY - boxHeight, boxWidth, boxHeight);
                cs.fill();
                
                // Draw shift text - Line 1: Time and Initials
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), SHIFT_FONT_SIZE);
                cs.newLineAtOffset(cellX + CELL_PADDING + 3, shiftY - 10);
                
                String timeStr = shift.getTime().toString();
                String workerName = shift.getStudentWorker() != null ? 
                    shift.getStudentWorker().getInitials() : "???";
                
                String line1 = timeStr + " - " + workerName;
                cs.showText(line1);
                cs.endText();
                
                // Draw shift text - Line 2: Workstation name
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), SHIFT_FONT_SIZE - 0.5f);
                cs.newLineAtOffset(cellX + CELL_PADDING + 3, shiftY - 21);
                
                String workstationName = shift.getWorkstation() != null ?
                    shift.getWorkstation().getName() : "N/A";
                
                // Truncate workstation name if too long
                if (workstationName.length() > 18) {
                    workstationName = workstationName.substring(0, 15) + "...";
                }
                cs.showText(workstationName);
                cs.endText();
                cs.setNonStrokingColor(Color.BLACK);
                
                shiftY -= (SHIFT_LINE_HEIGHT * 2.5f);
                shiftCount++;
            }
        }
    }

    /**
     * Gets the week start date (Monday).
     */
    private static LocalDate getWeekStartDate(Week week) {
        List<Shift> shifts = week.getShifts();
        if (shifts.isEmpty()) {
            return LocalDate.now();
        }
        
        // Find the earliest date in the week
        Shift earliestShift = shifts.stream()
            .min(Comparator.comparing(s -> {
                Date d = s.getDate();
                return LocalDate.of(d.get_year(), d.get_month(), d.get_day());
            }))
            .orElse(shifts.get(0));
        
        LocalDate date = LocalDate.of(
            earliestShift.getDate().get_year(),
            earliestShift.getDate().get_month(),
            earliestShift.getDate().get_day()
        );
        
        // Find the previous Monday
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.minusDays(1);
        }
        
        return date;
    }

    /**
     * Gets a color for a workstation using the same color scheme as the app.
     * Cycles through 5 predefined colors based on workstation index.
     */
    private static Color getWorkstationColor(Workstation workstation, Map<Long, Integer> workstationColorMap) {
        if (workstation == null) {
            return new Color(150, 150, 150); // Gray for no workstation
        }
        
        int colorIndex = workstationColorMap.getOrDefault(workstation.getId(), 0) % 5;
        
        switch (colorIndex) {
            case 0: return hexToColor("#156fabff"); // Blue
            case 1: return hexToColor("#4CAF50");   // Green
            case 2: return hexToColor("#FF9800");   // Orange
            case 3: return hexToColor("#9C27B0");   // Purple
            default: return hexToColor("#F44336");  // Red
        }
    }
    
    /**
     * Converts hex color string to AWT Color.
     */
    private static Color hexToColor(String hex) {
        // Remove # if present
        hex = hex.replace("#", "");
        // Handle 8-digit hex (RGBA) by taking first 6 digits (RGB)
        if (hex.length() == 8) {
            hex = hex.substring(0, 6);
        }
        return new Color(
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        );
    }

    /**
     * Draws a color legend showing workstation colors.
     */
    private static void drawColorLegend(PDPageContentStream cs, Week week, float x, float y, float width, Map<Long, Integer> workstationColorMap) throws IOException {
        // Collect all unique workstations from the week
        Map<String, Workstation> workstations = new HashMap<>();
        for (Shift shift : week.getShifts()) {
            if (shift.getWorkstation() != null) {
                String name = shift.getWorkstation().getName();
                if (!workstations.containsKey(name)) {
                    workstations.put(name, shift.getWorkstation());
                }
            }
        }
        
        if (workstations.isEmpty()) {
            return; // No legend to draw
        }
        
        // Draw legend title
        cs.setNonStrokingColor(Color.BLACK);
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
        cs.newLineAtOffset(x, y + 65);
        cs.showText("Workstation Color Key:");
        cs.endText();
        
        // Sort workstation names
        List<String> sortedNames = new ArrayList<>(workstations.keySet());
        sortedNames.sort(String::compareTo);
        
        // Draw legend items
        float itemWidth = 150;
        float itemHeight = 18;
        float itemsPerRow = Math.max(1, (int) (width / itemWidth));
        float currentX = x;
        float currentY = y + 45;
        int itemCount = 0;
        
        for (String name : sortedNames) {
            Workstation workstation = workstations.get(name);
            Color color = getWorkstationColor(workstation, workstationColorMap);
            
            // Draw color box
            cs.setNonStrokingColor(color);
            cs.addRect(currentX, currentY, 15, 15);
            cs.fill();
            
            // Draw border around color box
            cs.setStrokingColor(Color.BLACK);
            cs.setLineWidth(0.5f);
            cs.addRect(currentX, currentY, 15, 15);
            cs.stroke();
            
            // Draw workstation name
            cs.setNonStrokingColor(Color.BLACK);
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            cs.newLineAtOffset(currentX + 20, currentY + 3);
            
            String displayName = name;
            if (displayName.length() > 17) {
                displayName = displayName.substring(0, 14) + "...";
            }
            cs.showText(displayName);
            cs.endText();
            
            itemCount++;
            currentX += itemWidth;
            
            // Move to next row if needed
            if (itemCount % itemsPerRow == 0) {
                currentX = x;
                currentY -= itemHeight;
            }
        }
    }
}
