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
    
    private static final int DAYS_IN_WEEK = 5;
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
            schedule.generateWeeks();
            schedule.organizeShiftsIntoWeeks();

            List<Week> weeks = schedule.getWeeks();
            
            Map<Long, Integer> workstationColorMap = buildWorkstationColorMap(weeks);
            
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
        
        for (Week week : weeks) {
            for (Shift shift : week.getShifts()) {
                Workstation ws = shift.getWorkstation();
                if (ws != null && !uniqueWorkstations.stream().anyMatch(w -> w.getId().equals(ws.getId()))) {
                    uniqueWorkstations.add(ws);
                }
            }
        }
        
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
        
        float yPosition = pageHeight - MARGIN;
        yPosition = drawTitle(cs, schedule, weekNumber, week, yPosition);
        
        float gridWidth = pageWidth - (2 * MARGIN);
        float cellWidth = gridWidth / DAYS_IN_WEEK;
        float headerHeight = 30;
        float gridStartY = yPosition - 20;
        
        float legendHeight = 100;
        float availableHeight = gridStartY - MARGIN - headerHeight - legendHeight;
        float cellHeight = availableHeight;
        
        drawDayHeaders(cs, MARGIN, gridStartY, cellWidth, headerHeight);
        
        drawWeekGrid(cs, week, MARGIN, gridStartY - headerHeight, cellWidth, cellHeight, workstationColorMap);
        
        float legendY = MARGIN + legendHeight - 20;
        drawColorLegend(cs, week, MARGIN, legendY, pageWidth - (2 * MARGIN), workstationColorMap);
    }

    /**
     * Draws the title section with schedule info and week number.
     */
    private static float drawTitle(PDPageContentStream cs, Schedule schedule, int weekNumber, Week week, float yPosition) throws IOException {
        cs.setNonStrokingColor(new Color(21, 111, 171));
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Work Schedule - Week " + weekNumber);
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
        
        yPosition -= 25;
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), HEADER_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText(week.getWeekRangeString());
        cs.endText();
        
        yPosition -= 20;
        
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
            
            cs.addRect(cellX, y - headerHeight, cellWidth, headerHeight);
            cs.fill();
            
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
        Map<LocalDate, List<Shift>> shiftsByDate = new HashMap<>();
        for (Shift shift : week.getShifts()) {
            LocalDate date = LocalDate.of(
                shift.getDate().get_year(),
                shift.getDate().get_month(),
                shift.getDate().get_day()
            );
            shiftsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(shift);
        }
        
        LocalDate weekStartDate = getWeekStartDate(week);
        
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            LocalDate currentDate = weekStartDate.plusDays(i);
            float cellX = x + (i * cellWidth);
            
            cs.setStrokingColor(new Color(200, 200, 200));
            cs.addRect(cellX, y - cellHeight, cellWidth, cellHeight);
            cs.stroke();
            
            cs.setNonStrokingColor(new Color(100, 100, 100));
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), DATE_FONT_SIZE);
            cs.newLineAtOffset(cellX + CELL_PADDING, y - CELL_PADDING - DATE_FONT_SIZE);
            cs.showText(String.valueOf(currentDate.getDayOfMonth()));
            cs.endText();
            cs.setNonStrokingColor(Color.BLACK);
            
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
        float maxShiftsToShow = (int) ((cellHeight - CELL_PADDING - DATE_FONT_SIZE - 20) / (SHIFT_LINE_HEIGHT * 2));
        
        Map<Long, List<Shift>> shiftsByWorkstation = new HashMap<>();
        for (Shift shift : shifts) {
            Long workstationId = shift.getWorkstation() != null ? shift.getWorkstation().getId() : -1L;
            shiftsByWorkstation.computeIfAbsent(workstationId, k -> new ArrayList<>()).add(shift);
        }
        
        for (List<Shift> workstationShifts : shiftsByWorkstation.values()) {
            workstationShifts.sort(Comparator.comparing(s -> s.getTime().getStart_time()));
        }
        
        List<Long> sortedWorkstationIds = new ArrayList<>(shiftsByWorkstation.keySet());
        sortedWorkstationIds.sort((id1, id2) -> {
            if (id1.equals(-1L)) return 1;
            if (id2.equals(-1L)) return -1;
            
            Shift shift1 = shiftsByWorkstation.get(id1).get(0);
            Shift shift2 = shiftsByWorkstation.get(id2).get(0);
            String name1 = shift1.getWorkstation() != null ? shift1.getWorkstation().getName() : "ZZZ";
            String name2 = shift2.getWorkstation() != null ? shift2.getWorkstation().getName() : "ZZZ";
            return name1.compareTo(name2);
        });
        
        int shiftCount = 0;
        
        for (Long workstationId : sortedWorkstationIds) {
            List<Shift> workstationShifts = shiftsByWorkstation.get(workstationId);
            
            for (Shift shift : workstationShifts) {
                if (shiftCount >= maxShiftsToShow) {
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
                
                Color workstationColor = getWorkstationColor(shift.getWorkstation(), workstationColorMap);
                cs.setNonStrokingColor(workstationColor);
                float boxHeight = (SHIFT_LINE_HEIGHT * 2.5f);
                float boxWidth = cellWidth - (2 * CELL_PADDING);
                cs.addRect(cellX + CELL_PADDING, shiftY - boxHeight, boxWidth, boxHeight);
                cs.fill();
                
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
                
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), SHIFT_FONT_SIZE - 0.5f);
                cs.newLineAtOffset(cellX + CELL_PADDING + 3, shiftY - 21);
                
                String workstationName = shift.getWorkstation() != null ?
                    shift.getWorkstation().getName() : "N/A";
                
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
            return new Color(150, 150, 150);
        }
        
        int colorIndex = workstationColorMap.getOrDefault(workstation.getId(), 0) % 5;
        
        switch (colorIndex) {
            case 0: return hexToColor("#156fabff");
            case 1: return hexToColor("#4CAF50");
            case 2: return hexToColor("#FF9800");
            case 3: return hexToColor("#9C27B0");
            default: return hexToColor("#F44336");
        }
    }
    
    /**
     * Converts hex color string to AWT Color.
     */
    private static Color hexToColor(String hex) {
        hex = hex.replace("#", "");
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
            return;
        }
        
        cs.setNonStrokingColor(Color.BLACK);
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
        cs.newLineAtOffset(x, y + 65);
        cs.showText("Workstation Color Key:");
        cs.endText();
        
        List<String> sortedNames = new ArrayList<>(workstations.keySet());
        sortedNames.sort(String::compareTo);
        
        float itemWidth = 150;
        float itemHeight = 18;
        float itemsPerRow = Math.max(1, (int) (width / itemWidth));
        float currentX = x;
        float currentY = y + 45;
        int itemCount = 0;
        
        for (String name : sortedNames) {
            Workstation workstation = workstations.get(name);
            Color color = getWorkstationColor(workstation, workstationColorMap);
            
            cs.setNonStrokingColor(color);
            cs.addRect(currentX, currentY, 15, 15);
            cs.fill();
            
            cs.setStrokingColor(Color.BLACK);
            cs.setLineWidth(0.5f);
            cs.addRect(currentX, currentY, 15, 15);
            cs.stroke();
            
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
            
            if (itemCount % itemsPerRow == 0) {
                currentX = x;
                currentY -= itemHeight;
            }
        }
    }
}
