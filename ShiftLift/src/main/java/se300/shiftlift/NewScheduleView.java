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

    private VerticalLayout mainContainer = new VerticalLayout();

    private Button createScheduleButton = new Button("Create Schedule");
    private Button cancelButton = new Button("Cancel");
    private DatePicker startDatePicker = new DatePicker("Start Date");
    private DatePicker endDatePicker = new DatePicker("End Date");

    private final ScheduleService scheduleService;
    private boolean dirty = false;

    /**
     * Constructs the new schedule view with date pickers and action buttons.
     * Initializes the layout with drawer navigation, header, and schedule creation form.
     */
    public NewScheduleView(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
        createElements();
    }

    /**
     * Validates user authentication before allowing access to the view.
     * Checks for admin access and redirects to login if unauthorized.
     * 
     * @param event navigation event containing routing information
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn() || !Auth.isAdmin()) {
            Notification.show("Access denied: Admins only", 2000, Notification.Position.MIDDLE);
            event.rerouteTo("");
            return;
        }
    }

    /**
     * Creates and initializes all UI components for the new schedule view.
     * Configures drawer navigation, header, date pickers, and action buttons with styling.
     */
    private void createElements() {
        boolean admin = Auth.isAdmin();
        
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            RouterLink manageWorkersLink = new RouterLink("Manage Workers", ListUsersView.class);
            RouterLink manageWorkstationsLink = new RouterLink("Manage Workstations", ListWorkstationsView.class);
            RouterLink manageSchedulesLink = new RouterLink("Manage Schedules", ManageSchedulesView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink mainMenuLink = new RouterLink("Main Menu", MainMenuView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(manageWorkersLink);
            styleRouterLink(manageWorkstationsLink);
            styleRouterLink(manageSchedulesLink);
            styleRouterLink(changePasswordLink);
            styleRouterLink(mainMenuLink);
            
            drawerLayout.add(mainMenuLink, manageWorkersLink, manageWorkstationsLink, manageSchedulesLink, downloadPdfButton, changePasswordLink);
        }
        else{
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink mainMenuLink = new RouterLink("Main Menu", MainMenuView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(changePasswordLink);
            styleRouterLink(mainMenuLink);
            drawerLayout.add(mainMenuLink, downloadPdfButton, changePasswordLink);
        }
        
        addToDrawer(drawerLayout);
        
        setDrawerOpened(false);

        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
            .set("color", "#156fabff")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "4px");

        Button logoutButton = new Button("Logout");
        logoutButton.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutButton.addClickListener(e -> Auth.logoutToLogin());

        H2 navTitle = new H2("Create New Schedule");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

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

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setWidth("100%");
        contentLayout.getStyle().set("flex-grow", "1");
        contentLayout.setAlignItems(Alignment.CENTER);



        mainContainer.setMaxWidth("33.33vw");
        mainContainer.setMinWidth("300px");
        mainContainer.setAlignItems(Alignment.STRETCH);
        mainContainer.setJustifyContentMode(JustifyContentMode.CENTER);

        startDatePicker.setWidthFull();
        startDatePicker.setLabel("Start Date:");
        startDatePicker.setMin(LocalDate.now());
        startDatePicker.getStyle()
            .set("font-family", "Poppins, sans-serif");
        startDatePicker.addValueChangeListener(e -> {
            dirty = true;
            validateDates();
        });

        endDatePicker.setWidthFull();
        endDatePicker.setLabel("End Date:");
        endDatePicker.setMin(LocalDate.now());
        endDatePicker.getStyle()
            .set("font-family", "Poppins, sans-serif");
        endDatePicker.addValueChangeListener(e -> {
            dirty = true;
            validateDates();
        });

        mainContainer.add(startDatePicker, endDatePicker);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("gap", "12px");

        createScheduleButton.setWidth("calc(50% - 6px)");
        createScheduleButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif")
            .set("transition", "all 0.2s");
        createScheduleButton.addClickListener(e -> saveButtonClickListener());

        cancelButton.setWidth("calc(50% - 6px)");
        cancelButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#666666");
        cancelButton.addClickListener(e -> cancelButtonClickListener());

        buttonLayout.add(createScheduleButton, cancelButton);
        mainContainer.add(buttonLayout);
        
        contentLayout.add(mainContainer);
        contentLayout.setHorizontalComponentAlignment(Alignment.CENTER, mainContainer);
        
        setContent(contentLayout);
    }
    
    /**
     * Applies consistent styling to navigation links in the drawer menu.
     * Sets color, font, padding, and display properties for drawer navigation.
     * 
     * @param link RouterLink to be styled
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
     * Handles create schedule button click by validating dates and creating new schedule.
     * Checks for existing unpublished schedules before creating, converts LocalDate to custom Date objects,
     * and navigates to main menu on success.
     */
    private void saveButtonClickListener() {
        if (validateFields()) {
            if (scheduleService.hasUnpublishedSchedule()) {
                Notification.show("An unpublished schedule already exists. Please publish or delete it before creating a new one.",
                    4000, Notification.Position.MIDDLE);
                return;
            }

            try {
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

                scheduleService.createSchedule(startDate, endDate);
                
                dirty = false;
                Notification.show("Schedule created successfully!", 3000, Notification.Position.BOTTOM_START);
                UI.getCurrent().navigate("main-menu");

            } catch (Exception e) {
                Notification.show("Error creating schedule: " + e.getMessage(),
                    4000, Notification.Position.MIDDLE);
            }
        }
    }

    /**
     * Handles cancel button click by clearing dirty flag and navigating to main menu.
     */
    private void cancelButtonClickListener() {
        dirty = false;
        UI.getCurrent().navigate(MainMenuView.class);
    }

    /**
     * Validates that both date fields have values selected.
     * Shows notifications for missing fields.
     * 
     * @return true if all fields are valid, false otherwise
     */
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

    /**
     * Validates that start date is before end date and updates end date picker minimum.
     * Shows notification if dates are invalid.
     * 
     * @return true if dates are valid, false otherwise
     */
    private boolean validateDates() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            return true;
        }

        LocalDate startLocal = startDatePicker.getValue();
        LocalDate endLocal = endDatePicker.getValue();

        if (!startLocal.isBefore(endLocal)) {
            Notification.show("Start date must be before end date", 3000, Notification.Position.MIDDLE);
            return false;
        }

        endDatePicker.setMin(startLocal.plusDays(1));

        return true;
    }
    
    /**
     * Creates a button for downloading the latest published schedule as a PDF.
     * Finds the most recent published schedule, generates a PDF file, and triggers browser download.
     * Shows notifications for success or error conditions.
     * 
     * @return styled download button with click handler
     */
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
}
