package se300.shiftlift;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("Manage Schedules")
@Route("manage-schedules")
@RolesAllowed("ADMIN")
public class ManageSchedulesView extends AppLayout implements BeforeEnterObserver {

    private final ScheduleService scheduleService;
    private final ShiftService shiftService;
    private final VerticalLayout listLayout = new VerticalLayout();
    private final Button publishButton = new Button("Publish Schedule");
    private final Button discardButton = new Button("Discard Schedule");
    private Button selectedItem = null;
    private Schedule selectedSchedule = null;

    /**
     * Constructs the schedule management view with publish and discard controls.
     * Initializes the layout with drawer menu, header, and schedule list display.
     */
    public ManageSchedulesView(ScheduleService scheduleService, ShiftService shiftService) {
        this.scheduleService = scheduleService;
        this.shiftService = shiftService;
        
        boolean admin = Auth.isAdmin();
        
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink viewPublishedScheduleLink = new RouterLink("View Published Schedule", PublishedScheduleView.class);
            RouterLink manageWorkersLink = new RouterLink("Manage Workers", ListUsersView.class);
            RouterLink manageWorkstationsLink = new RouterLink("Manage Workstations", ListWorkstationsView.class);
            RouterLink manageSchedulesLink = new RouterLink("Manage Schedules", ManageSchedulesView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(viewPublishedScheduleLink);
            styleRouterLink(manageWorkersLink);
            styleRouterLink(manageWorkstationsLink);
            styleRouterLink(manageSchedulesLink);
            styleRouterLink(changePasswordLink);
            
            drawerLayout.add(viewPendingScheduleLink, viewPublishedScheduleLink, manageWorkersLink, manageWorkstationsLink, manageSchedulesLink, downloadPdfButton, changePasswordLink);
        }
        else{
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink viewPublishedScheduleLink = new RouterLink("View Published Schedule", PublishedScheduleView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(viewPublishedScheduleLink);
            styleRouterLink(changePasswordLink);
            
            drawerLayout.add(viewPendingScheduleLink, viewPublishedScheduleLink, downloadPdfButton, changePasswordLink);
        }
        
        addToDrawer(drawerLayout);
        
        setDrawerOpened(false);

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

        H2 navTitle = new H2("Manage Schedules");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

        var header = new HorizontalLayout(toggle, navTitle, logoutBtn);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
            .set("background-color", "white")
            .set("padding", "16px 20px");
        addToNavbar(header);

        Button returnButton = new Button("Return");

        publishButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif")
            .set("transition", "all 0.2s");
        publishButton.setEnabled(false);
        publishButton.getStyle().set("opacity", "0.5");

        returnButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#666666");

        discardButton.getStyle()
            .set("background-color", "#dc3545")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif")
            .set("transition", "all 0.2s");
        discardButton.setEnabled(false);
        discardButton.getStyle().set("opacity", "0.5");

        publishButton.addClickListener(e -> publishSelectedSchedule());
        discardButton.addClickListener(e -> confirmDiscardSchedule());
        returnButton.addClickListener(e -> UI.getCurrent().navigate(MainMenuView.class));

        Button addScheduleButton = new Button("+");
        addScheduleButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif")
            .set("font-size", "20px")
            .set("font-weight", "bold")
            .set("border", "none")
            .set("border-radius", "50%")
            .set("min-width", "40px")
            .set("width", "40px")
            .set("height", "40px")
            .set("padding", "0")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("margin", "16px auto")
            .set("transition", "all 0.2s");
        addScheduleButton.addClickListener(e -> UI.getCurrent().navigate("new-schedule"));

        HorizontalLayout addButtonLayout = new HorizontalLayout(addScheduleButton);
        addButtonLayout.setWidthFull();
        addButtonLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        addButtonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        HorizontalLayout actionLayout = new HorizontalLayout(publishButton, discardButton, returnButton);
        actionLayout.setWidthFull();
        actionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        actionLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        actionLayout.setSpacing(true);

        listLayout.setWidthFull();
        listLayout.setSpacing(true);
        listLayout.setPadding(false);
        listLayout.getStyle()
            .set("gap", "16px")
            .set("margin-top", "16px");

        VerticalLayout container = new VerticalLayout(listLayout, addButtonLayout, actionLayout);
        container.setWidthFull();
        container.setMaxWidth("max-content");
        container.setPadding(false);
        container.setSpacing(true);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);

        VerticalLayout contentLayout = new VerticalLayout(container);
        contentLayout.setWidthFull();
        contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        contentLayout.setPadding(true);
        contentLayout.setSpacing(true);

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
     * Validates user authentication before allowing access to the view.
     * Redirects to login page if user is not authenticated.
     * 
     * @param event navigation event containing routing information
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn()) {
            Notification.show("Please Log-in", 2000, Notification.Position.MIDDLE);
            event.rerouteTo("");
            return;
        }

        if (!Auth.isAdmin()) {
            Notification.show("Admin access required", 2000, Notification.Position.MIDDLE);
            event.rerouteTo("main-menu");
            return;
        }

        loadSchedules();
    }

    /**
     * Loads and displays all schedules from the database.
     * Cleans up expired schedules before displaying the list.
     */
    private void loadSchedules() {
        listLayout.removeAll();
        selectedItem = null;
        selectedSchedule = null;
        publishButton.setEnabled(false);
        publishButton.getStyle().set("opacity", "0.5");

        cleanupExpiredSchedules();

        List<Schedule> schedules = scheduleService.getAllSchedules();

        if (schedules.isEmpty()) {
            Span emptyMessage = new Span("No schedules found");
            emptyMessage.getStyle()
                .set("font-family", "Poppins, sans-serif")
                .set("color", "#666666")
                .set("font-size", "16px");
            listLayout.add(emptyMessage);
            return;
        }

        for (Schedule schedule : schedules) {
            Button scheduleButton = createScheduleButton(schedule);
            listLayout.add(scheduleButton);
        }
    }

    /**
     * Creates a button representing a schedule in the list.
     * Displays schedule date range and publish status with appropriate colors.
     */
    private Button createScheduleButton(Schedule schedule) {
        Button button = new Button();
        button.setWidth("100%");

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.getStyle().set("gap", "8px");

        Span dateRange = new Span(formatScheduleDateRange(schedule));
        dateRange.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("font-size", "18px")
            .set("font-weight", "600");

        String statusText = schedule.getApproved() != null && schedule.getApproved() 
            ? "Published" : "Unpublished";
        Span status = new Span("Status: " + statusText);
        status.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("font-size", "14px");

        textLayout.add(dateRange, status);

        Div wrapper = new Div(textLayout);
        wrapper.getStyle()
            .set("width", "100%")
            .set("padding", "20px 16px");
        button.getElement().appendChild(wrapper.getElement());

        boolean isPublished = schedule.getApproved() != null && schedule.getApproved();
        String backgroundColor = isPublished ? "#28a745" : "#156fabff";
        
        button.getStyle()
            .set("background-color", backgroundColor)
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif")
            .set("border", "none")
            .set("border-radius", "8px")
            .set("padding", "0")
            .set("cursor", "pointer")
            .set("transition", "all 0.2s")
            .set("min-height", "80px")
            .set("display", "flex")
            .set("align-items", "center");

        button.getElement().setProperty("_scheduleId", schedule.getId().toString());

        button.addClickListener(e -> selectSchedule(button, schedule));

        return button;
    }

    /**
     * Handles schedule selection and deselection.
     * Updates button states and enables/disables publish and discard buttons.
     */
    private void selectSchedule(Button button, Schedule schedule) {
        if (selectedItem == button) {
            boolean isPublished = schedule.getApproved() != null && schedule.getApproved();
            String backgroundColor = isPublished ? "#28a745" : "#156fabff";
            button.getStyle()
                .set("background-color", backgroundColor)
                .set("border", "none");
            
            selectedItem = null;
            selectedSchedule = null;
            publishButton.setEnabled(false);
            publishButton.getStyle().set("opacity", "0.5");
            discardButton.setEnabled(false);
            discardButton.getStyle().set("opacity", "0.5");
            return;
        }
        
        if (selectedItem != null) {
            Schedule prevSchedule = scheduleService.getScheduleById(
                Long.parseLong(selectedItem.getElement().getProperty("_scheduleId"))
            ).orElse(null);
            
            if (prevSchedule != null) {
                boolean isPrevPublished = prevSchedule.getApproved() != null && prevSchedule.getApproved();
                String prevBackgroundColor = isPrevPublished ? "#28a745" : "#156fabff";
                selectedItem.getStyle()
                    .set("background-color", prevBackgroundColor)
                    .set("border", "none");
            }
        }

        selectedItem = button;
        selectedSchedule = schedule;

        boolean isPublished = schedule.getApproved() != null && schedule.getApproved();
        String backgroundColor = isPublished ? "#28a745" : "#156fabff";
        button.getStyle()
            .set("background-color", backgroundColor)
            .set("border", "3px solid #ffc107");

        if (!isPublished) {
            publishButton.setEnabled(true);
            publishButton.getStyle().set("opacity", "1");
            discardButton.setEnabled(false);
            discardButton.getStyle().set("opacity", "0.5");
        } else {
            publishButton.setEnabled(false);
            publishButton.getStyle().set("opacity", "0.5");
            discardButton.setEnabled(true);
            discardButton.getStyle().set("opacity", "1");
        }
    }

    /**
     * Publishes the selected schedule after validation.
     * Prevents publishing if another schedule is already published.
     */
    private void publishSelectedSchedule() {
        if (selectedSchedule == null) {
            Notification.show("Please select a schedule to publish", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (selectedSchedule.getApproved() != null && selectedSchedule.getApproved()) {
            Notification.show("This schedule is already published", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (scheduleService.hasPublishedSchedule()) {
            Notification.show("A published schedule already exists. Please discard it before publishing a new one.",
                4000, Notification.Position.MIDDLE);
            return;
        }

        try {
            selectedSchedule.setApproved(true);
            scheduleService.save(selectedSchedule);
            Notification.show("Schedule published successfully!", 3000, Notification.Position.BOTTOM_START);
            loadSchedules();
            UI.getCurrent().navigate("manage-schedules");
        } catch (Exception e) {
            Notification.show("Error publishing schedule: " + e.getMessage(), 
                4000, Notification.Position.MIDDLE);
        }
    }

    /**
     * Shows confirmation dialog for discarding a published schedule.
     * Warns user that the action cannot be undone.
     */
    private void confirmDiscardSchedule() {
        if (selectedSchedule == null) {
            Notification.show("Please select a schedule to discard", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (selectedSchedule.getApproved() == null || !selectedSchedule.getApproved()) {
            Notification.show("Only published schedules can be discarded", 3000, Notification.Position.MIDDLE);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Discard Published Schedule");
        dialog.setText("Are you sure you want to discard this published schedule? " +
                      "This will permanently delete all shifts within the schedule dates " +
                      "and remove the schedule from the database. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Discard");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> discardSelectedSchedule());
        dialog.open();
    }

    /**
     * Discards the selected published schedule.
     * Deletes all associated shifts and the schedule from the database.
     */
    private void discardSelectedSchedule() {
        if (selectedSchedule == null) {
            return;
        }

        try {
            int deletedShifts = scheduleService.deleteScheduleWithShifts(selectedSchedule, shiftService);
            
            String message = "Schedule discarded successfully!";
            if (deletedShifts > 0) {
                message += " (" + deletedShifts + " shifts deleted)";
            }
            
            Notification.show(message, 3000, Notification.Position.BOTTOM_START);
            
            loadSchedules();
            
        } catch (Exception e) {
            Notification.show("Error discarding schedule: " + e.getMessage(), 
                4000, Notification.Position.MIDDLE);
        }
    }

    /**
     * Formats schedule date range for display.
     * Returns MM/DD/YYYY - MM/DD/YYYY format.
     */
    private String formatScheduleDateRange(Schedule schedule) {
        Date startDate = schedule.getStartDate();
        Date endDate = schedule.getEndDate();
        
        if (startDate == null || endDate == null) {
            return "Invalid Date Range";
        }

        return String.format("%02d/%02d/%04d - %02d/%02d/%04d",
            startDate.get_month(), startDate.get_day(), startDate.get_year(),
            endDate.get_month(), endDate.get_day(), endDate.get_year());
    }

    /**
     * Automatically removes schedules that have passed their end date.
     * Deletes all associated shifts and shows notification if cleanup was performed.
     */
    private void cleanupExpiredSchedules() {
        try {
            List<Schedule> allSchedules = scheduleService.getAllSchedules();
            LocalDate today = LocalDate.now();
            List<Schedule> expiredSchedules = new ArrayList<>();
            
            for (Schedule schedule : allSchedules) {
                Date endDate = schedule.getEndDate();
                if (endDate != null) {
                    LocalDate scheduleEndDate = LocalDate.of(
                        endDate.get_year(), 
                        endDate.get_month(), 
                        endDate.get_day()
                    );
                    
                    if (scheduleEndDate.isBefore(today)) {
                        expiredSchedules.add(schedule);
                    }
                }
            }
            
            int totalShiftsDeleted = 0;
            for (Schedule expiredSchedule : expiredSchedules) {
                int deletedShifts = scheduleService.deleteScheduleWithShifts(expiredSchedule, shiftService);
                totalShiftsDeleted += deletedShifts;
            }
            
            if (!expiredSchedules.isEmpty()) {
                String message = String.format("Automatically removed %d expired schedule(s) and %d associated shift(s)", 
                    expiredSchedules.size(), totalShiftsDeleted);
                Notification.show(message, 4000, Notification.Position.BOTTOM_START);
            }
            
        } catch (Exception e) {
            System.err.println("Error during schedule cleanup: " + e.getMessage());
        }
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
}
