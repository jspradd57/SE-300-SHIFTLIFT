package se300.shiftlift;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;

import jakarta.annotation.security.RolesAllowed;


@PageTitle("EditUserView")
@Route("EditUserView")
@RolesAllowed("ADMIN")
public class EditUserView extends AppLayout implements BeforeEnterObserver, com.vaadin.flow.router.BeforeLeaveObserver {

    
    private VerticalLayout contentLayout = new VerticalLayout();
    private HorizontalLayout layoutRow3 = new HorizontalLayout();
    private HorizontalLayout layoutRow5 = new HorizontalLayout();
    private VerticalLayout layoutColumn5 = new VerticalLayout();
    private VerticalLayout layoutColumn7 = new VerticalLayout();
    private VerticalLayout layoutColumn3 = new VerticalLayout();

    private HorizontalLayout layoutRow6 = new HorizontalLayout();
    private VerticalLayout layoutColumn8 = new VerticalLayout();
    private TextField emailTextField = new TextField();
    private TextField usernameTextField = new TextField();
    private TextField initialsTextField = new TextField();
    private PasswordField passwordField = new PasswordField();
    private ComboBox<String> maxHoursComboBox = new ComboBox<>();
    private HorizontalLayout layoutRow7 = new HorizontalLayout();
    private Button button_save = new Button();
    private Button button_cancel = new Button();
    private Button button_delete = new Button();
    private VerticalLayout layoutColumn9 = new VerticalLayout();
    private HorizontalLayout layoutRow8 = new HorizontalLayout();
    private VerticalLayout layoutRowButtons = new VerticalLayout();

    private User user;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private boolean dirty = false;
    
    public EditUserView(UserService userService, ScheduleService scheduleService) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        create_elements();
        // fields are created; if opened directly with a username query param, beforeEnter will load
        // track changes to detect unsaved edits
        emailTextField.addValueChangeListener(e -> {
            dirty = true;
            // Preview the username and initials changes
            String email = e.getValue();
            if (email != null && !email.isEmpty() && email.contains("@")) {
                String[] emailParts = email.split("@");
                String username = emailParts[0];
                usernameTextField.setValue(username);
                
                // Use the same initials logic as the User class
                String initials = (User.get_first_inital(username) + username.charAt(0)).toUpperCase();
                initialsTextField.setValue(initials);
            }
        });
        passwordField.addValueChangeListener(e -> dirty = true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn() || !Auth.isAdmin()) {
            Notification.show("Access denied: Admins only", 2000, Notification.Position.MIDDLE);
            event.rerouteTo("");
            return;
        }
        java.util.List<String> params = event.getLocation().getQueryParameters().getParameters().get("username");
        if (params != null && !params.isEmpty()) {
            String username = params.get(0);
            if (username != null && !username.isEmpty()) {
                loadUserByUsername(username);
            }
        }
    }

    @Override
    public void beforeLeave(com.vaadin.flow.router.BeforeLeaveEvent event) {
        if (!dirty) return;
        final com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
        Dialog confirm = new Dialog();
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(Alignment.CENTER);
        
        Span message = new Span("You have unsaved changes. Leave without saving?");
        message.getStyle().set("margin", "16px 0");
        dialogLayout.add(message);
        
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        
        com.vaadin.flow.component.button.Button leave = new com.vaadin.flow.component.button.Button("Leave", ev -> {
            confirm.close();
            action.proceed();
        });
        leave.getStyle()
            .set("margin-right", "16px")
            .set("color", "#666666");
        
        com.vaadin.flow.component.button.Button stay = new com.vaadin.flow.component.button.Button("Stay", ev -> {
            confirm.close();
        });
        stay.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        stay.getStyle().set("background-color", "#156fabff");
        
        buttonLayout.add(leave, stay);
        dialogLayout.add(buttonLayout);
        confirm.add(dialogLayout);
        confirm.open();
    }

    public void loadUserByUsername(String username) {
        List<User> users = userService.findByUsername(username);
        if (!users.isEmpty()) {
            this.user = users.get(0);
            setUserData(this.user);
            dirty = false;
        }
    }



    private void setUserData(User user) {
        emailTextField.setValue(user.getEmail());
        usernameTextField.setValue(user.getUsername());
        usernameTextField.getStyle().set("color", "#156fabff");
        initialsTextField.setValue(user.getInitials());
        passwordField.setValue(user.getPassword());
        
        // Show and set max hours combo box only for StudentWorker
        if (user instanceof StudentWorker) {
            StudentWorker sw = (StudentWorker) user;
            int maxHours = sw.getMax_hours();
            String selection = "International (20)"; // default
            if (maxHours == 25) {
                selection = "Domestic (25)";
            } else if (maxHours == 29) {
                selection = "University Break (29)";
            }
            maxHoursComboBox.setValue(selection);
            maxHoursComboBox.setVisible(true);
        } else {
            maxHoursComboBox.setVisible(false);
        }
    }

    private void create_elements() {
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
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutBtn.addClickListener(e -> Auth.logoutToLogin());

        // Title for navbar
        H2 navTitle = new H2("Edit User Data");
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

        contentLayout.setWidth("100%");
        contentLayout.getStyle().set("flex-grow", "1");
        layoutRow3.addClassName(Gap.MEDIUM);
        layoutRow3.setWidth("100%");
        layoutRow3.setHeight("min-content");
        layoutRow5.addClassName(Gap.MEDIUM);
        layoutRow5.setWidth("100%");
        layoutRow5.getStyle().set("flex-grow", "1");
        layoutColumn5.getStyle().set("flex-grow", "1");
        layoutColumn7.setWidth("100%");
        layoutColumn7.getStyle().set("flex-grow", "1");
        layoutColumn7.setJustifyContentMode(JustifyContentMode.CENTER);
        layoutColumn7.setAlignItems(Alignment.CENTER);

        layoutRow6.setWidthFull();
        //layoutColumn2.setFlexGrow(1.0, layoutRow6);
        layoutRow6.addClassName(Gap.MEDIUM);
        layoutRow6.setWidth("100%");
        layoutRow6.getStyle().set("flex-grow", "1");
        layoutColumn8.setHeightFull();
        //layoutRow2.setFlexGrow(1.0, layoutColumn8);
        layoutColumn8.setWidth("100%");
        layoutColumn8.getStyle().set("flex-grow", "1");
        emailTextField.setLabel("Email:");
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, emailTextField);
        emailTextField.setWidth("min-content");
        emailTextField.setErrorMessage("Please enter a valid email address (@my.erau.edu or @erau.edu)");
        emailTextField.setClearButtonVisible(true);
        emailTextField.setPattern("^.+@(my\\.)?erau\\.edu$");
        usernameTextField.setLabel("Username:");
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, usernameTextField);
        usernameTextField.setWidth("min-content");
        initialsTextField.setLabel("Initials:");
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, initialsTextField);
        initialsTextField.setWidth("min-content");
        passwordField.setLabel("Password:");
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, passwordField);
        passwordField.setWidth("min-content");
        
        maxHoursComboBox.setLabel("Max Hours:");
        maxHoursComboBox.setItems("International (20)", "Domestic (25)", "University Break (29)");
        maxHoursComboBox.setValue("International (20)");
        maxHoursComboBox.setWidth("min-content");
        maxHoursComboBox.setVisible(false); // Hidden by default, shown only for StudentWorker
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, maxHoursComboBox);
        
        layoutRow7.setWidthFull();
        layoutRowButtons.setWidthFull();
        layoutColumn3.setFlexGrow(1.0, layoutRow7);
        layoutRow7.addClassName(Gap.MEDIUM);
        layoutRow7.setWidth("100%");
        layoutRow7.getStyle().set("flex-grow", "1");
        layoutRow7.setAlignItems(Alignment.CENTER);
        layoutRow7.setJustifyContentMode(JustifyContentMode.CENTER);
        layoutRowButtons.addClassName(Gap.MEDIUM);
        layoutRowButtons.setWidth("100%");
        layoutRowButtons.getStyle().set("flex-grow", "1");
        layoutRowButtons.setAlignItems(Alignment.CENTER);
        layoutRowButtons.setJustifyContentMode(JustifyContentMode.CENTER);
        button_save.setText("Save Changes");
        button_save.setWidth("min-content");
        button_save.getStyle().set("background-color", "#156fabff").set("transition", "all 0.2s");
        button_save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button_save.addClickListener(e -> {
            save_button_click_listener();
        });
        button_cancel.setText("Cancel Changes");
        button_cancel.getStyle().set("color", "grey");
        button_cancel.setWidth("min-content");
        button_cancel.addClickListener(e -> {
            cancel_button_click_listener();
        });
        button_delete.setText("Delete User");
        button_delete.setWidth("min-content");
        button_delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button_delete.getStyle().set("background-color", "#9b0000ff").set("transition", "all 0.2s");
        button_delete.addClickListener(e -> {
            delete_button_click_listener();
        });
        layoutColumn9.getStyle().set("flex-grow", "1");
        layoutRow8.addClassName(Gap.MEDIUM);
        layoutRow8.setWidth("100%");
    layoutRow8.setHeight("min-content");

    contentLayout.add(layoutRow3);
    contentLayout.add(layoutRow5);
        layoutRow5.add(layoutColumn5);
        layoutRow5.add(layoutColumn7);

        layoutColumn7.add(layoutRow6);
        layoutRow6.add(layoutColumn8);
        layoutColumn8.add(emailTextField);
        layoutColumn8.add(usernameTextField);
    
        layoutColumn8.add(initialsTextField);
        layoutColumn8.add(passwordField);
        layoutColumn8.add(maxHoursComboBox);
        layoutColumn8.add(layoutRowButtons);
        
    layoutRow7.add(button_save);
    layoutRow7.add(button_cancel);
    layoutRowButtons.add(button_delete);
    layoutRowButtons.add(layoutRow7);
    layoutRow5.add(layoutColumn9);
    contentLayout.add(layoutRow8);
    
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

    private boolean validateFields() {
        // Implement field validation logic here
        if(emailTextField.isInvalid() || emailTextField.getValue().isEmpty()) {
            emailTextField.setErrorMessage("Invalid Email");
            emailTextField.setInvalid(true);
            return false;
        }
        if(usernameTextField.getValue().isEmpty()) {
            usernameTextField.setErrorMessage("Username cannot be empty");
            usernameTextField.setInvalid(true);
            return false;
        }
        if(initialsTextField.getValue().isEmpty() || initialsTextField.getValue().length() > 3) {
            initialsTextField.setErrorMessage("Initials cannot be empty or longer than 3 characters");
            initialsTextField.setInvalid(true);
            return false;
        }
        if(passwordField.getValue().isEmpty())
        {
            passwordField.setErrorMessage("Password cannot be empty");
            passwordField.setInvalid(true);
            return false;
        }
        return true;
    }

    private void save_button_click_listener() 
    {
        if(validateFields()) {
            if (user != null) {
                try {
                    // Email update will automatically update username and initials
                    user.setEmail(emailTextField.getValue().toLowerCase());
                    user.setPassword(passwordField.getValue());
                    
                    // If user is StudentWorker, update max hours
                    if (user instanceof StudentWorker) {
                        StudentWorker sw = (StudentWorker) user;
                        String maxHoursSelection = maxHoursComboBox.getValue();
                        int maxHours = 20; // default
                        if (maxHoursSelection != null) {
                            if (maxHoursSelection.contains("25")) {
                                maxHours = 25;
                            } else if (maxHoursSelection.contains("29")) {
                                maxHours = 29;
                            }
                        }
                        sw.setMax_hours(maxHours);
                    }
                    
                    userService.save(user);
                    dirty = false;
                    Notification.show("User saved", 2000, Notification.Position.BOTTOM_START);
                    // Navigate back to list-users after successful save
                    UI.getCurrent().navigate("list-users");
                } catch (Exception e) {
                    Notification.show("Error saving user: " + e.getMessage(), 
                        3000, Notification.Position.MIDDLE);
                }
            }
        }   
    }

    private void cancel_button_click_listener() 
    {
        // Clear dirty flag and navigate back
        dirty = false;
        UI.getCurrent().navigate("list-users");
    }

    private void delete_button_click_listener() 
    {
        if (user == null) return;
        Dialog confirm = new Dialog();
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(Alignment.CENTER);
        
        Span message = new Span("Are you sure you want to delete user '" + user.getUsername() + "'?");
        message.getStyle()
            .set("margin", "16px 0")
            .set("font-family", "Poppins, sans-serif");
        dialogLayout.add(message);
        
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        
        com.vaadin.flow.component.button.Button no = new com.vaadin.flow.component.button.Button("Cancel", ev -> confirm.close());
        no.getStyle()
            .set("margin-right", "16px")
            .set("color", "#666666");
        
        com.vaadin.flow.component.button.Button yes = new com.vaadin.flow.component.button.Button("Delete", ev -> {
            int deletedShifts = userService.delete(user);
            confirm.close();
            dirty = false;
            String deleteMessage = "User deleted";
            if (deletedShifts > 0) {
                deleteMessage += " (" + deletedShifts + " associated shift(s) also removed)";
            }
            Notification.show(deleteMessage, 4000, Notification.Position.BOTTOM_START);
            UI.getCurrent().navigate("list-users");
        });
        yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        yes.getStyle()
            .set("background-color", "#9b0000ff")
            .set("color", "white");
        
        buttonLayout.add(no, yes);
        dialogLayout.add(buttonLayout);
        confirm.add(dialogLayout);
        confirm.open();
        confirm.open();
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
}
