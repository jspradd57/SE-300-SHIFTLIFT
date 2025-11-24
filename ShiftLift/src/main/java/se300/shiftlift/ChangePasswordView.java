package se300.shiftlift;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("change-password")
public class ChangePasswordView extends AppLayout implements BeforeEnterObserver {

    @SuppressWarnings("unused")
    private final UserService userService;
    private final ScheduleService scheduleService;

    public ChangePasswordView(UserService userService, ScheduleService scheduleService) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        
        boolean admin = Auth.isAdmin();
        
        // Create styled drawer menu
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            // Routes that will be in the hamburger for navigation
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink viewPublishedScheduleLink = new RouterLink("View Published Schedule", PublishedScheduleView.class);
            RouterLink manageWorkersLink = new RouterLink("Manage Workers", ListUsersView.class);
            RouterLink manageWorkstationsLink = new RouterLink("Manage Workstations", ListWorkstationsView.class);
            RouterLink manageSchedulesLink = new RouterLink("Manage Schedules", ManageSchedulesView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink newShiftLink = new RouterLink("Create New Shift", NewShiftView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            // Apply styling to each link
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(viewPublishedScheduleLink);
            styleRouterLink(manageWorkersLink);
            styleRouterLink(manageWorkstationsLink);
            styleRouterLink(manageSchedulesLink);
            styleRouterLink(newShiftLink);
            styleRouterLink(changePasswordLink);
            
            drawerLayout.add(viewPendingScheduleLink, viewPublishedScheduleLink, manageWorkersLink, manageWorkstationsLink, manageSchedulesLink, newShiftLink, downloadPdfButton, changePasswordLink);
        }
        else{
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink viewPublishedScheduleLink = new RouterLink("View Published Schedule", PublishedScheduleView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink newShiftLink = new RouterLink("Request New Shift", NewShiftView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(viewPublishedScheduleLink);
            styleRouterLink(newShiftLink);
            styleRouterLink(changePasswordLink);
            
            drawerLayout.add(viewPendingScheduleLink, viewPublishedScheduleLink, newShiftLink, downloadPdfButton, changePasswordLink);
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
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutBtn.addClickListener(e -> Auth.logoutToLogin());

        // Title for navbar
        H2 navTitle = new H2("Change Password");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

        // Navbar layout (this is the header)
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

        // Create content layout
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(true);
        contentLayout.setSpacing(true);
        contentLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);



        PasswordField currentPassword = new PasswordField("Current Password");
        currentPassword.setWidth("300px");
        PasswordField newPassword = new PasswordField("New Password");
        newPassword.setWidth("300px");
        PasswordField confirmPassword = new PasswordField("Confirm New Password");
        confirmPassword.setWidth("300px");

        Button saveBtn = new Button("Save");
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background-color", "#156fabff").set("color", "white").set("transition", "all 0.2s");
        saveBtn.setWidth("300px");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyle().set("color", "#666666");
        cancelBtn.setWidth("300px");

        VerticalLayout form = new VerticalLayout(currentPassword, newPassword, confirmPassword, saveBtn, cancelBtn);
        form.setPadding(false);
        form.setSpacing(true);
        form.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        contentLayout.add(form);
        
        // Set content for AppLayout
        setContent(contentLayout);

        saveBtn.addClickListener(e -> {
            if (!Auth.isLoggedIn()) {
                Notification.show("Please log in first").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            String curr = currentPassword.getValue();
            String next = newPassword.getValue();
            String confirm = confirmPassword.getValue();

            if (curr == null || curr.isEmpty() || next == null || next.isEmpty() || confirm == null || confirm.isEmpty()) {
                Notification.show("All fields are required").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (!next.equals(confirm)) {
                Notification.show("New passwords do not match").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            User current = Auth.getCurrentUser();
            try {
                userService.changePassword(current, curr, next);
                Notification.show("Password updated successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate(MainMenuView.class));
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        cancelBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(MainMenuView.class)));
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn()) {
            event.rerouteTo("");
        }
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
