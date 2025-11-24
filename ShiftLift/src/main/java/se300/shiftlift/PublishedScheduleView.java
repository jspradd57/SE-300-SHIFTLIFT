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
    private int workstationCount = 5; // Default fallback
    private java.util.Map<Long, Integer> workstationColorMap = new java.util.HashMap<>();
    
    public PublishedScheduleView(ScheduleService scheduleService, ShiftService shiftService, 
                       WorkstationService workstationService, UserService userService) {
        this.scheduleService = scheduleService;
        this.shiftService = shiftService;
        this.workstationService = workstationService;
        this.userService = userService;
        boolean admin = Auth.isAdmin();
        
        // Get dynamic workstation count
        try {
            long count = workstationService.count();
            this.workstationCount = Math.max(1, (int) count); // Ensure at least 1
            
            // Build color map for workstations - get all workstations using unpaged query
            org.springframework.data.domain.Pageable unpaged = org.springframework.data.domain.Pageable.unpaged();
            List<Workstation> allWorkstations = workstationService.list(unpaged);
            for (int i = 0; i < allWorkstations.size(); i++) {
                workstationColorMap.put(allWorkstations.get(i).getId(), i);
            }
        } catch (Exception e) {
            this.workstationCount = 5; // Fallback to 5 if error
        }
        
        // Load current unpublished schedule
        loadCurrentSchedule();
        
        // Create styled drawer menu
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            // Routes that will be in the hamburger for navigation
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
            
            // Apply styling to each link
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
        
        // Set drawer open by default on main menu
        setDrawerOpened(true);

        // Creates a hamburger for navigation to other tabs
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
            .set("color", "#156fabff")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "4px");

        // Logout Button
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutBtn.addClickListener(e -> Auth.logoutToLogin());

        // Title for navbar
        H2 navTitle = new H2("Published Schedule");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

        // Navbar layout (this is the header)
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



        // week navigation
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

        // calendar header (below weekHeader)
        calendarHeader = new HorizontalLayout();
        calendarHeader.getStyle().set("padding-left", "40px");
        calendarHeader.setWidthFull();
        calendarHeader.setSpacing(false);
        calendarHeader.setPadding(false);
        calendarHeader.setMargin(false);
        calendarHeader.setJustifyContentMode(JustifyContentMode.START);

        scheduleGrid = createScheduleGrid();
        
        // Create color key/legend for workstations
        Component colorKey = createColorKey();

        // Place headers into the AppLayout content area
        VerticalLayout content = new VerticalLayout(weekHeader, calendarHeader, scheduleGrid, colorKey);
        content.setWidthFull();
        content.setAlignItems(Alignment.CENTER);
        // top 10px, right 60px, bottom 0px, left 60px
        content.getStyle().set("padding", "10px 60px 0 60px");
        content.getStyle().set("box-sizing", "border-box");
        content.setPadding(false); //we've set padding via CSS
        content.setSpacing(true);
        setContent(content);
        
        // Update display with current week data
        updateWeekDisplay();
    }

    private Component createScheduleGrid() {
    // Whole grid area under the day labels
    HorizontalLayout grid = new HorizontalLayout();
    grid.setWidthFull();
    grid.setHeight("720px"); // 18 half-hour slots × 40px per slot
    grid.getStyle()
        .set("border", "1px solid #e0e0e0")
        .set("box-sizing", "border-box")
        .set("overflow", "hidden");          //clip anything outside
    grid.setSpacing(false);
    grid.setPadding(false);

    //Left time axis
    VerticalLayout timeColumn = new VerticalLayout();
    timeColumn.setWidth("40px");
    timeColumn.setPadding(false);
    timeColumn.setSpacing(false);
    timeColumn.setHeightFull();              //same height as grid
    timeColumn.getStyle().set("border-right", "1px solid #e0e0e0");

    LocalTime startTime = LocalTime.of(8, 0);   //first label (8am)
    LocalTime endTime   = LocalTime.of(17, 0);  //last label (5pm)
    int slotMinutes = 30;                       //30-minute (half-hour) steps
    int pxPerSlot = 40;                         //must match addShiftBlock

    for (LocalTime t = startTime; !t.isAfter(endTime); t = t.plusMinutes(slotMinutes)) {
        Span label = new Span(t.toString());    //08:00, 08:30, 09:00, etc...
        label.getStyle()
             .set("font-size", "11px")
             .set("height", pxPerSlot + "px")
             .set("display", "flex")
             .set("align-items", "flex-start");
        timeColumn.setAlignItems(Alignment.CENTER);
        timeColumn.add(label);
    }

    grid.add(timeColumn);

    //day columns (5 days)
    for (int dayIndex = 0; dayIndex < 5; dayIndex++) {
        final int finalDayIndex = dayIndex; // Make final for lambda usage
        
        // Check if this day is within the schedule
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
              .set("overflow", "hidden") // clip bars inside each column
              .set("opacity", isDayInSchedule ? "1" : "0.5");
        
        // Add tooltip and interactions only for days in schedule
        if (isDayInSchedule && Auth.isAdmin()) {
            dayCol.getElement().setAttribute("title", "Click to create a new shift for this day");
            
            // Add hover effect
            dayCol.getElement().addEventListener("mouseenter", e -> {
                dayCol.getStyle().set("background-color", "#e3f2fd");
            });
            dayCol.getElement().addEventListener("mouseleave", e -> {
                dayCol.getStyle().set("background-color", originalBgColor);
            });
            
            // Add single-click listener to open NewShift dialog with selected date
            dayCol.getElement().addEventListener("click", e -> {
                openNewShiftDialogForDay(finalDayIndex);
            });
        } else if (!isDayInSchedule) {
            dayCol.getElement().setAttribute("title", "This day is outside the schedule period");
        }
        
        dayCol.setHeightFull();           // same height as grid

        // Load real shifts for this day from unpublished schedule
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
        // If error, show a simple message
        Span errorMsg = new Span("Unable to load workstation colors");
        errorMsg.getStyle().set("color", "#999");
        keyItems.add(errorMsg);
    }
    
    keyContainer.add(keyTitle, keyItems);
    return keyContainer;
}


    private void addShiftBlock(Div dayCol,
                           LocalTime shiftStart,
                           LocalTime shiftEnd,
                           int workstationIndex,
                           String workerInitials,
                           Shift shift) {

    LocalTime gridStart = LocalTime.of(8, 0); // 👈 match startTime above (8am)
    int slotMinutes = 30;  // Match the 30-minute time axis slots
    int pxPerSlot   = 40;
    
    // Calculate pixels per minute for granular positioning
    double pxPerMinute = pxPerSlot / (double) slotMinutes;

    int minutesFromStart = (int)Duration.between(gridStart, shiftStart).toMinutes();
    int durationMinutes  = (int)Duration.between(shiftStart, shiftEnd).toMinutes();

    // Use minute-level precision for positioning
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
    
    // Add hover effects
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
    
    // Add click listener to open EditShift dialog (admins only)
    if (shift != null && shift.getId() != null) {
        // Only allow admins to edit shifts on published schedule
        if(Auth.isAdmin()) {
            block.getElement().addEventListener("click", e -> {
                openEditShiftDialog(shift.getId());
            }).addEventData("event.stopPropagation()");
            
            // Add tooltip
            block.getElement().setAttribute("title", "Click to edit this shift");
        } else {
            // Prevent click from bubbling to dayCol for non-admin users
            block.getElement().addEventListener("click", e -> {
                Notification notification = Notification.show("Only admins can edit shifts on the published schedule");
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.setDuration(3000);
                notification.setPosition(Notification.Position.TOP_CENTER);
            }).addEventData("event.stopPropagation()");
            // Different cursor and tooltip for non-admin users
            block.getStyle().set("cursor", "default");
            block.getElement().setAttribute("title", "Only admins can edit this shift");
        }
    }
    
    // Add worker initials as text content
    if (workerInitials != null && !workerInitials.trim().isEmpty()) {
        block.setText(workerInitials);
    }

    dayCol.add(block);
}

private String getWorkstationColor(int idx) {
    // Cycle through colors if there are more workstations than colors
    int colorIndex = idx % 5;
    switch (colorIndex) {
        case 0: return "#156fabff";
        case 1: return "#4CAF50";   
        case 2: return "#FF9800";   
        case 3: return "#9C27B0";   
        default: return "#F44336";  
    }
}

private void loadCurrentSchedule() {
    try {
        var scheduleOpt = scheduleService.getLatestPublishedSchedule();
        
        if (scheduleOpt.isPresent()) {
            currentSchedule = scheduleOpt.get();
            currentSchedule.generateWeeks();
            availableWeeks = currentSchedule.getWeeks();
            currentWeekIndex = 0;
        } else {
            // No unpublished schedule found
            availableWeeks = new ArrayList<>();
            currentSchedule = null;
        }
    } catch (Exception e) {
        availableWeeks = new ArrayList<>();
        currentSchedule = null;
    }
}

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

private void updateWeekDisplay() {
    if (currentSchedule == null) {
        weekLabel.setText("No Unpublished Schedule Found");
        updateCalendarHeader(new String[]{"N/A", "N/A", "N/A", "N/A", "N/A"});
        updateScheduleGrid();
        return;
    }
    
    // Always show the full unpublished schedule date range
    String scheduleStart = formatDate(currentSchedule.getStartDate());
    String scheduleEnd = formatDate(currentSchedule.getEndDate());
    weekLabel.setText(scheduleStart + " - " + scheduleEnd);
    
    if (availableWeeks == null || availableWeeks.isEmpty()) {
        // Generate dates for the first week of the schedule
        String[] scheduleWeekDates = getScheduleWeekDates();
        updateCalendarHeader(scheduleWeekDates);
        updateScheduleGrid();
        return;
    }
    
    // Get dates for the current week (Monday-Friday)
    Week currentWeek = availableWeeks.get(currentWeekIndex);
    String[] weekDates = getWeekDates(currentWeek);
    updateCalendarHeader(weekDates);
    updateScheduleGrid();
}

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

private String[] getScheduleWeekDates() {
    String[] dates = new String[5];
    
    if (currentSchedule == null || currentSchedule.getStartDate() == null) {
        for (int i = 0; i < 5; i++) {
            dates[i] = "N/A";
        }
        return dates;
    }
    
    // Use the schedule's start date to find the first Monday
    Date scheduleStart = currentSchedule.getStartDate();
    java.time.LocalDate localStart = java.time.LocalDate.of(
        scheduleStart.get_year(), 
        scheduleStart.get_month(), 
        scheduleStart.get_day()
    );
    
    // Find the first Monday on or after the schedule start date
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

private void updateCalendarHeader(String[] dates) {
    calendarHeader.removeAll();
    
    for (int i = 0; i < 5; i++) {
        VerticalLayout dayCol = new VerticalLayout();
        dayCol.setWidth("20%");
        
        H4 dayName = new H4(days[i]);
        // Use responsive font sizing with clamp: min 10px, preferred 1.2vw, max 20px
        dayName.getStyle()
            .set("font-size", "clamp(10px, 1.2vw, 20px)")
            .set("margin", "0")
            .set("white-space", "nowrap")
            .set("overflow", "hidden")
            .set("text-overflow", "ellipsis")
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#156fabff");
        
        Span date = new Span(dates[i]);
        // Date also scales responsively but slightly smaller
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

private void updateScheduleGrid() {
    VerticalLayout content = (VerticalLayout) getContent();
    if (content != null && scheduleGrid != null) {
        Component newGrid = createScheduleGrid();
        content.replace(scheduleGrid, newGrid);
        scheduleGrid = newGrid;
    }
}

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

private void loadUnpublishedShiftsForDay(Div dayCol, int dayIndex) {
    if (currentSchedule == null) {
        return;
    }
    
    try {
        java.time.LocalDate targetDate;
        
        if (availableWeeks != null && !availableWeeks.isEmpty()) {
            // Use week-based calculation
            Week currentWeek = availableWeeks.get(currentWeekIndex);
            Date startDate = currentWeek.getWeekStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                startDate.get_year(), startDate.get_month(), startDate.get_day()
            );
            
            java.time.LocalDate monday = localStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            targetDate = monday.plusDays(dayIndex);
        } else {
            // Use schedule start date for calculation
            Date scheduleStart = currentSchedule.getStartDate();
            java.time.LocalDate localStart = java.time.LocalDate.of(
                scheduleStart.get_year(), 
                scheduleStart.get_month(), 
                scheduleStart.get_day()
            );
            
            // Find the first Monday on or after the schedule start date
            java.time.LocalDate firstMonday = localStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
            targetDate = firstMonday.plusDays(dayIndex);
        }
        
        Date targetDateObj = new Date(
            targetDate.getDayOfMonth(),
            targetDate.getMonthValue(),
            targetDate.getYear()
        );
        
        // Get unpublished shifts for this specific date
        List<Shift> dayShifts = getUnpublishedShiftsForDate(targetDateObj);
        
        for (Shift shift : dayShifts) {
            addShiftBlockFromShift(dayCol, shift);
        }
        
    } catch (Exception e) {
        // Handle errors silently
    }
}

private List<Shift> getUnpublishedShiftsForDate(Date targetDate) {
    // Get shifts within the current unpublished schedule's date range
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

private boolean dateIsWithinSchedule(Date date, Schedule schedule) {
    if (schedule.getStartDate() == null || schedule.getEndDate() == null) {
        return false;
    }
    
    int dateInt = date.get_Date();
    int startInt = schedule.getStartDate().get_Date();
    int endInt = schedule.getEndDate().get_Date();
    
    return dateInt >= startInt && dateInt <= endInt;
}

private void addShiftBlockFromShift(Div dayCol, Shift shift) {
    if (shift.getTime() == null || shift.getWorkstation() == null) {
        return;
    }
    
    int startTimeInt = shift.getTime().getStart_time();
    int endTimeInt = shift.getTime().getEnd_time();
    
    LocalTime shiftStart = LocalTime.of(startTimeInt / 100, startTimeInt % 100);
    LocalTime shiftEnd = LocalTime.of(endTimeInt / 100, endTimeInt % 100);
    
    int workstationIndex = getWorkstationColorIndex(shift.getWorkstation().getId());
    
    // Get worker initials
    String workerInitials = "";
    if (shift.getStudentWorker() != null && shift.getStudentWorker().getInitials() != null) {
        workerInitials = shift.getStudentWorker().getInitials();
    }
    
    addShiftBlock(dayCol, shiftStart, shiftEnd, workstationIndex, workerInitials, shift);
}

private int getWorkstationColorIndex(Long workstationId) {
    if (workstationId == null) {
        return 0;
    }
    // Use the color map to get consistent color index for each workstation
    return workstationColorMap.getOrDefault(workstationId, 0);
}

private String formatDate(Date date) {
    if (date == null) return "N/A";
    return String.format("%d/%d/%04d", 
        date.get_month(), 
        date.get_day(), 
        date.get_year()
    );
}

private void openNewShiftDialog(java.time.LocalDate selectedDate) {
    Dialog dialog = new Dialog();
    dialog.setModal(true);
    dialog.setDraggable(true);
    dialog.setResizable(true);
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");
    
    NewShiftDialogContent content = new NewShiftDialogContent(
        userService, workstationService, shiftService, scheduleService, selectedDate,
        () -> {
            dialog.close();
            updateScheduleGrid();
        }
    );
    
    dialog.add(content);
    dialog.open();
}

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
            
            // Find the first Monday on or after the schedule start date
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

private void openEditShiftDialog(Long shiftId) {
    Dialog dialog = new Dialog();
    dialog.setModal(true);
    dialog.setDraggable(true);
    dialog.setResizable(true);
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");
    
    EditShiftDialogContent content = new EditShiftDialogContent(
        userService, workstationService, shiftService, scheduleService, shiftId,
        () -> {
            dialog.close();
            updateScheduleGrid();
        }
    );
    
    dialog.add(content);
    dialog.open();
}

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

private void styleRouterLink(RouterLink link) {
    link.getStyle()
        .set("color", "#156fabff")
        .set("font-family", "Poppins, sans-serif")
        .set("text-decoration", "none")
        .set("padding", "8px 0")
        .set("display", "block")
        .set("font-size", "16px");
}

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
            // Find the latest published schedule
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
            
            // Generate PDF to temporary file
            String tempDir = System.getProperty("java.io.tmpdir");
            String pdfPath = tempDir + "/schedule-" + schedule.getId() + ".pdf";
            SchedulePdfGenerator.generateSchedulePdf(schedule, pdfPath);
            
            // Trigger download
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
            // Find the latest published schedule
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
            
            // Generate Hours Report PDF to temporary file
            String tempDir = System.getProperty("java.io.tmpdir");
            String pdfPath = tempDir + "/hours-report-" + schedule.getId() + ".pdf";
            HoursReportPdfGenerator.generateHoursReportPdf(schedule, pdfPath);
            
            // Trigger download
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


    @Override
        public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn()) {
            event.rerouteTo(LoginView.class);
        }
        }
}