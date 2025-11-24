package se300.shiftlift;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("view-published-schedule")
public class PublishedScheduleView extends AppLayout implements BeforeEnterObserver {
    private static final String[] days = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };
    
    private final ScheduleService scheduleService;
    private final ShiftService shiftService;
    private final UserService userService;
    private Schedule currentSchedule;
    private int currentWeekIndex = 0;
    private List<Week> availableWeeks;
    private H3 weekLabel;
    private HorizontalLayout calendarHeader;
    private Component scheduleGrid;
    
    private final WorkstationService workstationService;
    private int workstationCount = 5;
    private java.util.Map<Long, Integer> workstationColorMap = new java.util.HashMap<>();
    
    /**
     * Constructs the Published Schedule View with calendar grid display.
     * Initializes services, workstation color mapping, navigation drawer, and calendar components.
     * Sets up different menu options based on admin vs student user roles.
     */
    public PublishedScheduleView(ScheduleService scheduleService, ShiftService shiftService, 
                       WorkstationService workstationService, UserService userService) {
        this.scheduleService = scheduleService;
        this.shiftService = shiftService;
        this.workstationService = workstationService;
        this.userService = userService;
        boolean admin = Auth.isAdmin();
        
        try {
            long count = workstationService.count();
            this.workstationCount = Math.max(1, (int) count);
            
            org.springframework.data.domain.Pageable unpaged = org.springframework.data.domain.Pageable.unpaged();
            List<Workstation> allWorkstations = workstationService.list(unpaged);
            for (int i = 0; i < allWorkstations.size(); i++) {
                workstationColorMap.put(allWorkstations.get(i).getId(), i);
            }
        } catch (Exception e) {
            this.workstationCount = 5;
        }
        
        loadCurrentSchedule();
        
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink manageWorkersLink = new RouterLink("Manage Workers", ListUsersView.class);
            RouterLink manageWorkstationsLink = new RouterLink("Manage Workstations", ListWorkstationsView.class);
            RouterLink manageSchedulesLink = new RouterLink("Manage Schedules", ManageSchedulesView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            
            Button newShiftButton = new Button("Create New Shift");
            newShiftButton.addClickListener(e -> openNewShiftDialog(null));
            styleButton(newShiftButton);
            
            Button downloadPdfButton = createDownloadPdfButton();
            Button downloadHoursReportButton = createDownloadHoursReportButton();
            
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(manageWorkersLink);
            styleRouterLink(manageWorkstationsLink);
            styleRouterLink(manageSchedulesLink);
            styleRouterLink(changePasswordLink);
            
            drawerLayout.add(viewPendingScheduleLink, manageWorkersLink, manageWorkstationsLink, manageSchedulesLink, newShiftButton, downloadPdfButton, downloadHoursReportButton, changePasswordLink);
        }
        else{
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(changePasswordLink);
            drawerLayout.add(viewPendingScheduleLink, downloadPdfButton, changePasswordLink);
        }
        
        addToDrawer(drawerLayout);
        
        setDrawerOpened(true);

        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
            .set("color", "#156fabff")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "4px");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutBtn.addClickListener(e -> Auth.logoutToLogin());

        H2 navTitle = new H2("Published Schedule");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

        var header = new HorizontalLayout(toggle, navTitle, logoutBtn);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
            .set("background-color", "white")
            .set("padding", "16px 20px");
        addToNavbar(header);



        Button prevWeek = new Button(new Icon(VaadinIcon.ANGLE_LEFT));
        Button nextWeek = new Button(new Icon(VaadinIcon.ANGLE_RIGHT));
        
        prevWeek.addClickListener(e -> navigateWeek(-1));
        nextWeek.addClickListener(e -> navigateWeek(1));

        weekLabel = new H3();
        weekLabel.getStyle()
                 .set("color", "#156fabff")
                 .set("font-family", "Poppins, sans-serif")
                 .set("margin", "0 0 24px 0");

        HorizontalLayout weekHeader = new HorizontalLayout(prevWeek, weekLabel, nextWeek);
        weekHeader.setAlignItems(Alignment.CENTER);
        weekHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);
        weekHeader.setWidthFull();

        calendarHeader = new HorizontalLayout();
        calendarHeader.getStyle().set("padding-left", "40px");
        calendarHeader.setWidthFull();
        calendarHeader.setSpacing(false);
        calendarHeader.setPadding(false);
        calendarHeader.setMargin(false);
        calendarHeader.setJustifyContentMode(JustifyContentMode.START);

        scheduleGrid = createScheduleGrid();
        
        Component colorKey = createColorKey();

        VerticalLayout content = new VerticalLayout(weekHeader, calendarHeader, scheduleGrid, colorKey);
        content.setWidthFull();
        content.setAlignItems(Alignment.CENTER);
        content.getStyle().set("padding", "10px 60px 0 60px");
        content.getStyle().set("box-sizing", "border-box");
        content.setPadding(false);
        content.setSpacing(true);
        setContent(content);
        
        updateWeekDisplay();
    }

    /**
     * Creates the main calendar grid with time axis and 5-day columns.
     * Builds a horizontal layout with 30-minute time slots from 8am-5pm.
     * Each day column displays shifts and allows admin users to create new shifts.
     */
    private Component createScheduleGrid() {
    HorizontalLayout grid = new HorizontalLayout();
    grid.setWidthFull();
    grid.setHeight("720px");
    grid.getStyle()
        .set("border", "1px solid #e0e0e0")
        .set("box-sizing", "border-box")
        .set("overflow", "hidden");
    grid.setSpacing(false);
    grid.setPadding(false);

    VerticalLayout timeColumn = new VerticalLayout();
    timeColumn.setWidth("40px");
    timeColumn.setPadding(false);
    timeColumn.setSpacing(false);
    timeColumn.setHeightFull();
    timeColumn.getStyle().set("border-right", "1px solid #e0e0e0");

    LocalTime startTime = LocalTime.of(8, 0);
    LocalTime endTime   = LocalTime.of(17, 0);
    int slotMinutes = 30;
    int pxPerSlot = 40;

    for (LocalTime t = startTime; !t.isAfter(endTime); t = t.plusMinutes(slotMinutes)) {
        Span label = new Span(t.toString());
        label.getStyle()
             .set("font-size", "11px")
             .set("height", pxPerSlot + "px")
             .set("display", "flex")
             .set("align-items", "flex-start");
        timeColumn.setAlignItems(Alignment.CENTER);
        timeColumn.add(label);
    }

    grid.add(timeColumn);

    for (int dayIndex = 0; dayIndex < 5; dayIndex++) {
        final int finalDayIndex = dayIndex;
        
        boolean isDayInSchedule = isDayWithinSchedule(finalDayIndex);
        
        final String originalBgColor = isDayInSchedule ? 
            (dayIndex % 2 == 0 ? "#fafafa" : "#ffffff") : "#e8e8e8";
        
        Div dayCol = new Div();
        dayCol.getStyle()
              .set("flex", "1")
              .set("position", "relative")
              .set("border-left", "2px solid #d0d0d0")
              .set("border-right", dayIndex == 4 ? "2px solid #d0d0d0" : "")
              .set("background-color", originalBgColor)
              .set("cursor", (Auth.isAdmin() && isDayInSchedule) ? "pointer" : "default")
              .set("transition", "background-color 0.2s ease")
              .set("overflow", "hidden")
              .set("opacity", isDayInSchedule ? "1" : "0.5");
        
        if (isDayInSchedule && Auth.isAdmin()) {
            dayCol.getElement().setAttribute("title", "Click to create a new shift for this day");
            
            dayCol.getElement().addEventListener("mouseenter", e -> {
                dayCol.getStyle().set("background-color", "#e3f2fd");
            });
            dayCol.getElement().addEventListener("mouseleave", e -> {
                dayCol.getStyle().set("background-color", originalBgColor);
            });
            
            dayCol.getElement().addEventListener("click", e -> {
                openNewShiftDialogForDay(finalDayIndex);
            });
        } else if (!isDayInSchedule) {
            dayCol.getElement().setAttribute("title", "This day is outside the schedule period");
        }
        
        dayCol.setHeightFull();

        loadUnpublishedShiftsForDay(dayCol, finalDayIndex);

        grid.add(dayCol);
    }

    return grid;
}

private Component createColorKey() {
    VerticalLayout keyContainer = new VerticalLayout();
    keyContainer.setPadding(false);
    keyContainer.setSpacing(false);
    keyContainer.getStyle()
        .set("margin-top", "20px")
        .set("padding", "16px")
        .set("border", "1px solid #e0e0e0")
        .set("border-radius", "8px")
        .set("background-color", "#fafafa")
        .set("max-width", "600px");
    
    H4 keyTitle = new H4("Workstation Color Key");
    keyTitle.getStyle()
        .set("margin", "0 0 12px 0")
        .set("color", "#156fabff")
        .set("font-family", "Poppins, sans-serif");
    
    HorizontalLayout keyItems = new HorizontalLayout();
    keyItems.setSpacing(true);
    keyItems.setWidthFull();
    keyItems.getStyle().set("flex-wrap", "wrap");
    
    try {
        org.springframework.data.domain.Pageable unpaged = org.springframework.data.domain.Pageable.unpaged();
        List<Workstation> allWorkstations = workstationService.list(unpaged);
        
        for (Workstation workstation : allWorkstations) {
            int colorIndex = workstationColorMap.getOrDefault(workstation.getId(), 0);
            String color = getWorkstationColor(colorIndex);
            
            HorizontalLayout keyItem = new HorizontalLayout();
            keyItem.setSpacing(false);
            keyItem.setAlignItems(Alignment.CENTER);
            keyItem.getStyle()
                .set("margin-right", "16px")
                .set("margin-bottom", "8px");
            
            Div colorBox = new Div();
            colorBox.getStyle()
                .set("width", "20px")
                .set("height", "20px")
                .set("background-color", color)
                .set("border-radius", "4px")
                .set("margin-right", "8px")
                .set("border", "1px solid rgba(0,0,0,0.1)");
            
            Span workstationName = new Span(workstation.getName());
            workstationName.getStyle()
                .set("font-size", "14px")
                .set("color", "#333")
                .set("font-family", "Poppins, sans-serif");
            
            keyItem.add(colorBox, workstationName);
            keyItems.add(keyItem);
        }
    } catch (Exception e) {
        Span errorMsg = new Span("Unable to load workstation colors");
        errorMsg.getStyle().set("color", "#999");
        keyItems.add(errorMsg);
    }
    
    keyContainer.add(keyTitle, keyItems);
    return keyContainer;
}


    /**
     * Adds a visual shift block to the calendar day column at the correct time position.
     * Calculates precise positioning based on shift start/end times and renders with workstation color.
     * Includes hover effects and click handlers for editing shifts (admin only).
     */
    private void addShiftBlock(Div dayCol,
                           LocalTime shiftStart,
                           LocalTime shiftEnd,
                           int workstationIndex,
                           String workerInitials,
                           Shift shift) {

    LocalTime gridStart = LocalTime.of(8, 0);
    int slotMinutes = 30;
    int pxPerSlot   = 40;
    
    double pxPerMinute = pxPerSlot / (double) slotMinutes;

    int minutesFromStart = (int)Duration.between(gridStart, shiftStart).toMinutes();
    int durationMinutes  = (int)Duration.between(shiftStart, shiftEnd).toMinutes();

    double topPx    = minutesFromStart * pxPerMinute;
    double heightPx = durationMinutes * pxPerMinute;

    double wsWidth = 100.0 / workstationCount;
    double leftPercent = workstationIndex * wsWidth;

    Div block = new Div();
    block.getStyle()
         .set("position", "absolute")
         .set("top", topPx + "px")
         .set("left", leftPercent + "%")
         .set("width", wsWidth + "%")
         .set("height", heightPx + "px")
         .set("border-radius", "6px")
         .set("background-color", getWorkstationColor(workstationIndex))
         .set("border", "2px solid rgba(255, 255, 255, 0.3)")
         .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.2)")
         .set("cursor", "pointer")
         .set("transition", "transform 0.2s ease, box-shadow 0.2s ease")
         .set("display", "flex")
         .set("align-items", "center")
         .set("justify-content", "center")
         .set("color", "white")
         .set("font-weight", "bold")
         .set("font-size", "clamp(8px, 0.9vw, 24px)")
         .set("text-align", "center");
    
    block.getElement().addEventListener("mouseenter", e -> {
        block.getStyle()
             .set("transform", "scale(1.05)")
             .set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.3)");
    });
    block.getElement().addEventListener("mouseleave", e -> {
        block.getStyle()
             .set("transform", "scale(1.0)")
             .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.2)");
    });
    
    if (shift != null && shift.getId() != null) {
        if(Auth.isAdmin()) {
            block.getElement().addEventListener("click", e -> {
                openEditShiftDialog(shift.getId());
            }).addEventData("event.stopPropagation()");
            
            block.getElement().setAttribute("title", "Click to edit this shift");
        } else {
            block.getElement().addEventListener("click", e -> {
                Notification notification = Notification.show("Only admins can edit shifts on the published schedule");
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.setDuration(3000);
                notification.setPosition(Notification.Position.TOP_CENTER);
            }).addEventData("event.stopPropagation()");
            block.getStyle().set("cursor", "default");
            block.getElement().setAttribute("title", "Only admins can edit this shift");
        }
    }
    
    if (workerInitials != null && !workerInitials.trim().isEmpty()) {
        block.setText(workerInitials);
    }

    dayCol.add(block);
}

/**
 * Returns the color hex code for a workstation based on its index.
 * Cycles through 5 predefined colors for visual distinction between workstations.
 */
private String getWorkstationColor(int idx) {
    int colorIndex = idx % 5;
    switch (colorIndex) {
        case 0: return "#156fabff";
        case 1: return "#4CAF50";   
        case 2: return "#FF9800";   
        case 3: return "#9C27B0";   
        default: return "#F44336";  
    }
}

/**
 * Loads the latest published schedule from the database and initializes week navigation.
 * Sets up the available weeks list for calendar display and navigation.
 */
private void loadCurrentSchedule() {
    try {
        var scheduleOpt = scheduleService.getLatestPublishedSchedule();
        
        if (scheduleOpt.isPresent()) {
            currentSchedule = scheduleOpt.get();
            currentSchedule.generateWeeks();
            availableWeeks = currentSchedule.getWeeks();
            currentWeekIndex = 0;
        } else {
            availableWeeks = new ArrayList<>();
            currentSchedule = null;
        }
    } catch (Exception e) {
        availableWeeks = new ArrayList<>();
        currentSchedule = null;
    }
}

/**
 * Navigates to the previous or next week in the schedule based on direction parameter.
 * Updates the calendar display with the new week's data.
 */
private void navigateWeek(int direction) {
    if (availableWeeks == null || availableWeeks.isEmpty()) {
        return;
    }
    
    int newIndex = currentWeekIndex + direction;
    if (newIndex >= 0 && newIndex < availableWeeks.size()) {
        currentWeekIndex = newIndex;
        updateWeekDisplay();
    }
}

/**
 * Updates the calendar header and grid to display the current week's dates and shifts.
 * Shows the full schedule date range in the week label.
 */
private void updateWeekDisplay() {
    if (currentSchedule == null) {
        weekLabel.setText("No Unpublished Schedule Found");
        updateCalendarHeader(new String[]{"N/A", "N/A", "N/A", "N/A", "N/A"});
        updateScheduleGrid();
        return;
    }
    
    String scheduleStart = formatDate(currentSchedule.getStartDate());
    String scheduleEnd = formatDate(currentSchedule.getEndDate());
    weekLabel.setText(scheduleStart + " - " + scheduleEnd);
    
    if (availableWeeks == null || availableWeeks.isEmpty()) {
        String[] scheduleWeekDates = getScheduleWeekDates();
        updateCalendarHeader(scheduleWeekDates);
        updateScheduleGrid();
        return;
    }
    
    Week currentWeek = availableWeeks.get(currentWeekIndex);
    String[] weekDates = getWeekDates(currentWeek);
    updateCalendarHeader(weekDates);
    updateScheduleGrid();
}

/**
 * Generates an array of formatted date strings for Monday through Friday of the given week.
 * Returns dates in MM/DD/YY format for display in the calendar header.
 */
private String[] getWeekDates(Week week) {
    String[] dates = new String[5];
    Date startDate = week.getWeekStartDate();
    
    java.time.LocalDate localStart = java.time.LocalDate.of(
        startDate.get_year(), startDate.get_month(), startDate.get_day()
    );
    
    java.time.LocalDate monday = localStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    
    for (int i = 0; i < 5; i++) {
        java.time.LocalDate day = monday.plusDays(i);
        dates[i] = String.format("%d/%d/%02d", 
            day.getMonthValue(), 
            day.getDayOfMonth(), 
            day.getYear() % 100
        );
    }
    
    return dates;
}

/**
 * Generates date strings for the first week of the schedule starting from the first Monday.
 * Used when week data is not available in the schedule object.
 */
private String[] getScheduleWeekDates() {
    String[] dates = new String[5];
    
    if (currentSchedule == null || currentSchedule.getStartDate() == null) {
        for (int i = 0; i < 5; i++) {
            dates[i] = "N/A";
        }
        return dates;
    }
    
    Date scheduleStart = currentSchedule.getStartDate();
    java.time.LocalDate localStart = java.time.LocalDate.of(
        scheduleStart.get_year(), 
        scheduleStart.get_month(), 
        scheduleStart.get_day()
    );
    
    java.time.LocalDate firstMonday = localStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
    
    for (int i = 0; i < 5; i++) {
        java.time.LocalDate day = firstMonday.plusDays(i);
        dates[i] = String.format("%d/%d/%02d", 
            day.getMonthValue(), 
            day.getDayOfMonth(), 
            day.getYear() % 100
        );
    }
    
    return dates;
}

/**
 * Updates the calendar header row with day names and dates.
 * Creates column headers for Monday through Friday with responsive font sizing.
 */
private void updateCalendarHeader(String[] dates) {
    calendarHeader.removeAll();
    
    for (int i = 0; i < 5; i++) {
        VerticalLayout dayCol = new VerticalLayout();
        dayCol.setWidth("20%");
        
        H4 dayName = new H4(days[i]);
        dayName.getStyle()
            .set("font-size", "clamp(10px, 1.2vw, 20px)")
            .set("margin", "0")
            .set("white-space", "nowrap")
            .set("overflow", "hidden")
            .set("text-overflow", "ellipsis")
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#156fabff");
        
        Span date = new Span(dates[i]);
        date.getStyle()
            .set("font-size", "clamp(8px, 1vw, 16px)")
            .set("white-space", "nowrap")
            .set("overflow", "hidden")
            .set("text-overflow", "ellipsis")
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#666");
        
        dayCol.add(dayName, date);
        dayCol.setAlignItems(Alignment.CENTER);

        dayCol.setPadding(false);
        dayCol.setSpacing(false);
        dayCol.setMargin(false);

        dayCol.getStyle().set("border", "1px solid #d9d9d9");
        dayCol.getStyle().set("padding", "8px");
        dayCol.getStyle().set("box-sizing", "border-box");

        calendarHeader.add(dayCol);
    }
}

/**
 * Recreates the schedule grid component with current week's shift data.
 * Replaces the old grid with a freshly generated one to reflect any updates.
 */
private void updateScheduleGrid() {
    VerticalLayout content = (VerticalLayout) getContent();
    if (content != null && scheduleGrid != null) {
        Component newGrid = createScheduleGrid();
        content.replace(scheduleGrid, newGrid);
        scheduleGrid = newGrid;
    }
}

/**
 * Checks if the day at the given index falls within the current schedule's date range.
 * Prevents interaction with days outside the published schedule period.
 */
private boolean isDayWithinSchedule(int dayIndex) {
    if (currentSchedule == null) {
        return false;
    }
    
    try {
        java.time.LocalDate targetDate;
        
        if (availableWeeks != null && !availableWeeks.isEmpty()) {
            Week currentWeek = availableWeeks.get(currentWeekIndex);
            Date startDate = currentWeek.getWeekStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                startDate.get_year(), startDate.get_month(), startDate.get_day()
            );
            
            java.time.LocalDate monday = localStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            targetDate = monday.plusDays(dayIndex);
        } else {
            Date scheduleStart = currentSchedule.getStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                scheduleStart.get_year(), 
                scheduleStart.get_month(), 
                scheduleStart.get_day()
            );
            
            java.time.LocalDate firstMonday = localStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            if (firstMonday.isBefore(localStart.minusDays(6))) {
                firstMonday = firstMonday.plusWeeks(1);
            }
            targetDate = firstMonday.plusDays(dayIndex);
        }
        
        Date targetDateObj = new Date(
            targetDate.getDayOfMonth(),
            targetDate.getMonthValue(),
            targetDate.getYear()
        );
        
        return dateIsWithinSchedule(targetDateObj, currentSchedule);
    } catch (Exception e) {
        return false;
    }
}

/**
 * Loads and displays all shifts for a specific day column in the calendar grid.
 * Queries shifts from the database and renders them as visual blocks on the day column.
 */
private void loadUnpublishedShiftsForDay(Div dayCol, int dayIndex) {
    if (currentSchedule == null) {
        return;
    }
    
    try {
        java.time.LocalDate targetDate;
        
        if (availableWeeks != null && !availableWeeks.isEmpty()) {
            Week currentWeek = availableWeeks.get(currentWeekIndex);
            Date startDate = currentWeek.getWeekStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                startDate.get_year(), startDate.get_month(), startDate.get_day()
            );
            
            java.time.LocalDate monday = localStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            targetDate = monday.plusDays(dayIndex);
        } else {
            Date scheduleStart = currentSchedule.getStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                scheduleStart.get_year(), 
                scheduleStart.get_month(), 
                scheduleStart.get_day()
            );
            
            java.time.LocalDate firstMonday = localStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
            targetDate = firstMonday.plusDays(dayIndex);
        }
        
        Date targetDateObj = new Date(
            targetDate.getDayOfMonth(),
            targetDate.getMonthValue(),
            targetDate.getYear()
        );
        
        List<Shift> dayShifts = getUnpublishedShiftsForDate(targetDateObj);
        
        for (Shift shift : dayShifts) {
            addShiftBlockFromShift(dayCol, shift);
        }
        
    } catch (Exception e) {
    }
}

/**
 * Retrieves all shifts for a specific date that fall within the current schedule's range.
 * Filters shifts by date equality and schedule boundary validation.
 */
private List<Shift> getUnpublishedShiftsForDate(Date targetDate) {
    if (currentSchedule == null) {
        return new ArrayList<>();
    }
    
    List<Shift> allShifts = shiftService.getAllShifts();
    return allShifts.stream()
        .filter(shift -> shift.getDate() != null && 
                       shift.getDate().get_Date() == targetDate.get_Date())
        .filter(shift -> dateIsWithinSchedule(shift.getDate(), currentSchedule))
        .toList();
}

/**
 * Checks if a given date falls within a schedule's start and end date range.
 * Compares date integers to determine if the date is within bounds.
 */
private boolean dateIsWithinSchedule(Date date, Schedule schedule) {
    if (schedule.getStartDate() == null || schedule.getEndDate() == null) {
        return false;
    }
    
    int dateInt = date.get_Date();
    int startInt = schedule.getStartDate().get_Date();
    int endInt = schedule.getEndDate().get_Date();
    
    return dateInt >= startInt && dateInt <= endInt;
}

/**
 * Converts a Shift entity into a visual block and adds it to the day column.
 * Extracts shift time, workstation, and worker information for rendering.
 */
private void addShiftBlockFromShift(Div dayCol, Shift shift) {
    if (shift.getTime() == null || shift.getWorkstation() == null) {
        return;
    }
    
    int startTimeInt = shift.getTime().getStart_time();
    int endTimeInt = shift.getTime().getEnd_time();
    
    LocalTime shiftStart = LocalTime.of(startTimeInt / 100, startTimeInt % 100);
    LocalTime shiftEnd = LocalTime.of(endTimeInt / 100, endTimeInt % 100);
    
    int workstationIndex = getWorkstationColorIndex(shift.getWorkstation().getId());
    
    String workerInitials = "";
    if (shift.getStudentWorker() != null && shift.getStudentWorker().getInitials() != null) {
        workerInitials = shift.getStudentWorker().getInitials();
    }
    
    addShiftBlock(dayCol, shiftStart, shiftEnd, workstationIndex, workerInitials, shift);
}

/**
 * Maps a workstation ID to its color index for consistent color assignment.
 * Uses the workstation color map to ensure each workstation always gets the same color.
 */
private int getWorkstationColorIndex(Long workstationId) {
    if (workstationId == null) {
        return 0;
    }
    return workstationColorMap.getOrDefault(workstationId, 0);
}

/**
 * Formats a Date object into a readable string in MM/DD/YYYY format.
 * Returns 'N/A' if the date is null.
 */
private String formatDate(Date date) {
    if (date == null) return "N/A";
    return String.format("%d/%d/%04d", 
        date.get_month(), 
        date.get_day(), 
        date.get_year()
    );
}

/**
 * Opens a dialog for creating a new shift with an optional pre-selected date.
 * Displays the NewShiftDialogContent component in a modal dialog.
 */
private void openNewShiftDialog(java.time.LocalDate selectedDate) {
    Dialog dialog = new Dialog();
    dialog.setModal(true);
    dialog.setDraggable(true);
    dialog.setResizable(true);
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");
    
    NewShiftDialogContent content = new NewShiftDialogContent(
        userService, workstationService, shiftService, scheduleService, selectedDate, false,
        () -> {
            dialog.close();
            updateScheduleGrid();
        }
    );
    
    dialog.add(content);
    dialog.open();
}

/**
 * Opens the new shift dialog with the date calculated from the day index in the current week.
 * Converts the day column index to an actual date within the schedule.
 */
private void openNewShiftDialogForDay(int dayIndex) {
    try {
        java.time.LocalDate targetDate;
        
        if (availableWeeks != null && !availableWeeks.isEmpty()) {
            Week currentWeek = availableWeeks.get(currentWeekIndex);
            Date startDate = currentWeek.getWeekStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                startDate.get_year(), startDate.get_month(), startDate.get_day()
            );
            
            java.time.LocalDate monday = localStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            targetDate = monday.plusDays(dayIndex);
        } else if (currentSchedule != null) {
            Date scheduleStart = currentSchedule.getStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                scheduleStart.get_year(), 
                scheduleStart.get_month(), 
                scheduleStart.get_day()
            );
            
            java.time.LocalDate firstMonday = localStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
            targetDate = firstMonday.plusDays(dayIndex);
        } else {
            targetDate = java.time.LocalDate.now();
        }
        
        openNewShiftDialog(targetDate);
    } catch (Exception e) {
        openNewShiftDialog(null);
    }
}

/**
 * Opens a dialog for editing an existing shift identified by its ID.
 * Displays the EditShiftDialogContent component in a modal dialog.
 */
private void openEditShiftDialog(Long shiftId) {
    Dialog dialog = new Dialog();
    dialog.setModal(true);
    dialog.setDraggable(true);
    dialog.setResizable(true);
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");
    
    EditShiftDialogContent content = new EditShiftDialogContent(
        userService, workstationService, shiftService, scheduleService, shiftId, false,
        () -> {
            dialog.close();
            updateScheduleGrid();
        }
    );
    
    dialog.add(content);
    dialog.open();
}

/**
 * Applies consistent styling to buttons in the navigation drawer.
 * Sets ShiftLift blue color, font family, and layout properties.
 */
private void styleButton(Button button) {
    button.getStyle()
        .set("color", "#156fabff")
        .set("font-family", "Poppins, sans-serif")
        .set("text-decoration", "none")
        .set("padding", "8px 0")
        .set("display", "block")
        .set("font-size", "16px")
        .set("background", "transparent")
        .set("border", "none")
        .set("cursor", "pointer")
        .set("text-align", "left");
}

/**
 * Applies consistent styling to router links in the navigation drawer.
 * Sets ShiftLift blue color, font family, and layout properties.
 */
private void styleRouterLink(RouterLink link) {
    link.getStyle()
        .set("color", "#156fabff")
        .set("font-family", "Poppins, sans-serif")
        .set("text-decoration", "none")
        .set("padding", "8px 0")
        .set("display", "block")
        .set("font-size", "16px");
}

/**
 * Creates a button for downloading the published schedule as a PDF document.
 * Generates and triggers download of the latest published schedule in PDF format.
 */
private Button createDownloadPdfButton() {
    Button downloadButton = new Button("Download Schedule PDF");
    downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    downloadButton.getStyle()
        .set("color", "#156fabff")
        .set("font-family", "Poppins, sans-serif")
        .set("padding", "8px 0")
        .set("font-size", "16px")
        .set("text-align", "left")
        .set("justify-content", "flex-start");
    
    downloadButton.addClickListener(e -> {
        try {
            List<Schedule> allSchedules = scheduleService.getAllSchedules();
            java.util.Optional<Schedule> latestPublished = allSchedules.stream()
                .filter(s -> s.getApproved() != null && s.getApproved())
                .max(java.util.Comparator.comparing(Schedule::getId));
            
            if (latestPublished.isEmpty()) {
                Notification.show("No published schedule available to download", 
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            Schedule schedule = latestPublished.get();
            scheduleService.loadShiftsForSchedule(schedule);
            
            String tempDir = System.getProperty("java.io.tmpdir");
            String pdfPath = tempDir + "/schedule-" + schedule.getId() + ".pdf";
            SchedulePdfGenerator.generateSchedulePdf(schedule, pdfPath);
            
            java.io.File pdfFile = new java.io.File(pdfPath);
            com.vaadin.flow.server.StreamResource resource = 
                new com.vaadin.flow.server.StreamResource("schedule.pdf", 
                    () -> {
                        try {
                            return new java.io.FileInputStream(pdfFile);
                        } catch (java.io.FileNotFoundException ex) {
                            return null;
                        }
                    });
            
            com.vaadin.flow.component.html.Anchor downloadLink = 
                new com.vaadin.flow.component.html.Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.setId("pdf-download-" + System.currentTimeMillis());
            
            getElement().appendChild(downloadLink.getElement());
            com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                "document.getElementById($0).click()", downloadLink.getId().get()
            );
            
            Notification.show("PDF download started", 2000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception ex) {
            Notification.show("Error generating PDF: " + ex.getMessage(), 
                5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    });
    
    return downloadButton;
}

/**
 * Creates a button for downloading a hours report PDF showing worker hours from the published schedule.
 * Generates and triggers download of hours worked summary for all workers.
 */
private Button createDownloadHoursReportButton() {
    Button downloadButton = new Button("Download Hours Report");
    downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    downloadButton.getStyle()
        .set("color", "#156fabff")
        .set("font-family", "Poppins, sans-serif")
        .set("padding", "8px 0")
        .set("font-size", "16px")
        .set("text-align", "left")
        .set("justify-content", "flex-start");
    
    downloadButton.addClickListener(e -> {
        try {
            List<Schedule> allSchedules = scheduleService.getAllSchedules();
            java.util.Optional<Schedule> latestPublished = allSchedules.stream()
                .filter(s -> s.getApproved() != null && s.getApproved())
                .max(java.util.Comparator.comparing(Schedule::getId));
            
            if (latestPublished.isEmpty()) {
                Notification.show("No published schedule available to download", 
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            Schedule schedule = latestPublished.get();
            scheduleService.loadShiftsForSchedule(schedule);
            
            String tempDir = System.getProperty("java.io.tmpdir");
            String pdfPath = tempDir + "/hours-report-" + schedule.getId() + ".pdf";
            HoursReportPdfGenerator.generateHoursReportPdf(schedule, pdfPath);
            
            java.io.File pdfFile = new java.io.File(pdfPath);
            com.vaadin.flow.server.StreamResource resource = 
                new com.vaadin.flow.server.StreamResource("hours-report.pdf", 
                    () -> {
                        try {
                            return new java.io.FileInputStream(pdfFile);
                        } catch (java.io.FileNotFoundException ex) {
                            return null;
                        }
                    });
            
            com.vaadin.flow.component.html.Anchor downloadLink = 
                new com.vaadin.flow.component.html.Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.setId("hours-pdf-download-" + System.currentTimeMillis());
            
            getElement().appendChild(downloadLink.getElement());
            com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                "document.getElementById($0).click()", downloadLink.getId().get()
            );
            
            Notification.show("Hours report download started", 2000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception ex) {
            Notification.show("Error generating hours report: " + ex.getMessage(), 
                5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    });
    
    return downloadButton;
}

/**
 * Validates user authentication before allowing access to the published schedule view.
 * Redirects to login if user is not authenticated.
 */
@Override
public void beforeEnter(BeforeEnterEvent event) {
    if (!Auth.isLoggedIn()) {
        event.rerouteTo(LoginView.class);
    }
}
}