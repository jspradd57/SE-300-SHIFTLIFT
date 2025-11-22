package se300.shiftlift;

import java.time.LocalDate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("New Schedule")
@Route("new-schedule")
@RolesAllowed("ADMIN")
public class NewScheduleView extends AppLayout implements BeforeEnterObserver {

    // UI Components
    private VerticalLayout mainContainer = new VerticalLayout();

    private Button createScheduleButton = new Button("Create Schedule");
    private Button cancelButton = new Button("Cancel");
    private DatePicker startDatePicker = new DatePicker("Start Date");
    private DatePicker endDatePicker = new DatePicker("End Date");

    // Services
    private final ScheduleService scheduleService;
    private boolean dirty = false;

    public NewScheduleView(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
        createElements();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn() || !Auth.isAdmin()) {
            Notification.show("Access denied: Admins only", 2000, Notification.Position.MIDDLE);
            event.rerouteTo("");
            return;
        }
    }

    private void createElements() {
        boolean admin = Auth.isAdmin();
        
        // Create styled drawer menu
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            // Routes that will be in the hamburger for navigation
            RouterLink manageWorkersLink = new RouterLink("Manage Workers", ListUsersView.class);
            RouterLink manageWorkstationsLink = new RouterLink("Manage Workstations", ListWorkstationsView.class);
            RouterLink manageSchedulesLink = new RouterLink("Manage Schedules", ManageSchedulesView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink newShiftLink = new RouterLink("Create New Shift", NewShiftView.class);
            RouterLink mainMenuLink = new RouterLink("Main Menu", MainMenuView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            // Apply styling to each link
            styleRouterLink(manageWorkersLink);
            styleRouterLink(manageWorkstationsLink);
            styleRouterLink(manageSchedulesLink);
            styleRouterLink(newShiftLink);
            styleRouterLink(changePasswordLink);
            styleRouterLink(mainMenuLink);
            
            drawerLayout.add(mainMenuLink, manageWorkersLink, manageWorkstationsLink, manageSchedulesLink, newShiftLink, downloadPdfButton, changePasswordLink);
        }
        else{
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink newShiftLink = new RouterLink("Request New Shift", NewShiftView.class);
            RouterLink mainMenuLink = new RouterLink("Main Menu", MainMenuView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(newShiftLink);
            styleRouterLink(changePasswordLink);
            styleRouterLink(mainMenuLink);
            drawerLayout.add(mainMenuLink, newShiftLink, downloadPdfButton, changePasswordLink);
        }
        
        addToDrawer(drawerLayout);
        
        // Set drawer closed by default
        setDrawerOpened(false);

        // Creates a hamburger for navigation to other tabs
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
            .set("color", "#156fabff")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "4px");

        // Logout Button
        Button logoutButton = new Button("Logout");
        logoutButton.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutButton.addClickListener(e -> Auth.logoutToLogin());

        // Title for navbar
        H2 navTitle = new H2("Create New Schedule");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

        // Navbar layout (this is the header)
        var header = new HorizontalLayout(toggle, navTitle, logoutButton);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
            .set("background-color", "white")
            .set("padding", "16px 20px");
        addToNavbar(header);

        // Create content layout
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setWidth("100%");
        contentLayout.getStyle().set("flex-grow", "1");
        contentLayout.setAlignItems(Alignment.CENTER);



        // Main Container Setup
        mainContainer.setMaxWidth("33.33vw");
        mainContainer.setMinWidth("300px");
        mainContainer.setAlignItems(Alignment.STRETCH);
        mainContainer.setJustifyContentMode(JustifyContentMode.CENTER);

        // Start Date Picker
        startDatePicker.setWidthFull();
        startDatePicker.setLabel("Start Date:");
        startDatePicker.setMin(LocalDate.now());
        startDatePicker.getStyle()
            .set("font-family", "Poppins, sans-serif");
        startDatePicker.addValueChangeListener(e -> {
            dirty = true;
            validateDates();
        });

        // End Date Picker
        endDatePicker.setWidthFull();
        endDatePicker.setLabel("End Date:");
        endDatePicker.setMin(LocalDate.now());
        endDatePicker.getStyle()
            .set("font-family", "Poppins, sans-serif");
        endDatePicker.addValueChangeListener(e -> {
            dirty = true;
            validateDates();
        });

        // Add date pickers to main container
        mainContainer.add(startDatePicker, endDatePicker);

        // Buttons Layout
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("gap", "12px");

        // Create Schedule Button
        createScheduleButton.setWidth("calc(50% - 6px)");
        createScheduleButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif")
            .set("transition", "all 0.2s");
        createScheduleButton.addClickListener(e -> saveButtonClickListener());

        // Cancel Button
        cancelButton.setWidth("calc(50% - 6px)");
        cancelButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#666666");
        cancelButton.addClickListener(e -> cancelButtonClickListener());

        buttonLayout.add(createScheduleButton, cancelButton);
        mainContainer.add(buttonLayout);
        
        contentLayout.add(mainContainer);
        contentLayout.setHorizontalComponentAlignment(Alignment.CENTER, mainContainer);
        
        // Set content for AppLayout
        setContent(contentLayout);
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

    private void saveButtonClickListener() {
        if (validateFields()) {
            // Check if an unpublished schedule already exists
            if (scheduleService.hasUnpublishedSchedule()) {
                Notification.show("An unpublished schedule already exists. Please publish or delete it before creating a new one.",
                    4000, Notification.Position.MIDDLE);
                return;
            }

            try {
                // Convert LocalDate to custom Date objects
                LocalDate startLocal = startDatePicker.getValue();
                LocalDate endLocal = endDatePicker.getValue();

                Date startDate = new Date(
                    startLocal.getDayOfMonth(),
                    startLocal.getMonthValue(),
                    startLocal.getYear()
                );

                Date endDate = new Date(
                    endLocal.getDayOfMonth(),
                    endLocal.getMonthValue(),
                    endLocal.getYear()
                );

                // Create and save the schedule
                Schedule schedule = scheduleService.createSchedule(startDate, endDate);
                
                dirty = false;
                Notification.show("Schedule created successfully!", 3000, Notification.Position.BOTTOM_START);
                UI.getCurrent().navigate("main-menu");

            } catch (Exception e) {
                Notification.show("Error creating schedule: " + e.getMessage(),
                    4000, Notification.Position.MIDDLE);
            }
        }
    }

    private void cancelButtonClickListener() {
        dirty = false;
        UI.getCurrent().navigate(MainMenuView.class);
    }

    private boolean validateFields() {
        if (startDatePicker.getValue() == null) {
            Notification.show("Please select a start date", 3000, Notification.Position.MIDDLE);
            return false;
        }
        if (endDatePicker.getValue() == null) {
            Notification.show("Please select an end date", 3000, Notification.Position.MIDDLE);
            return false;
        }
        return validateDates();
    }

    private boolean validateDates() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            return true; // Skip validation if either date is not selected
        }

        LocalDate startLocal = startDatePicker.getValue();
        LocalDate endLocal = endDatePicker.getValue();

        // Check if start date is before end date
        if (!startLocal.isBefore(endLocal)) {
            Notification.show("Start date must be before end date", 3000, Notification.Position.MIDDLE);
            return false;
        }

        // Update end date picker minimum to be after start date
        endDatePicker.setMin(startLocal.plusDays(1));

        return true;
    }
    
    private Button createDownloadPdfButton() {
        Button downloadButton = new Button("Download PDF");
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
                java.util.List<Schedule> allSchedules = scheduleService.getAllSchedules();
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
}
