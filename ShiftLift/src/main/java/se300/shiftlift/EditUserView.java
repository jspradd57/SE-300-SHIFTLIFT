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
    
    /**
     * Constructs the edit user view with form fields and navigation.
     * Initializes value change listeners to track unsaved edits and automatically
     * update username and initials fields when email changes.
     */
    public EditUserView(UserService userService, ScheduleService scheduleService) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        create_elements();
        emailTextField.addValueChangeListener(e -> {
            dirty = true;
            String email = e.getValue();
            if (email != null && !email.isEmpty() && email.contains("@")) {
                String[] emailParts = email.split("@");
                String username = emailParts[0];
                usernameTextField.setValue(username);
                
                String initials = (User.get_first_inital(username) + username.charAt(0)).toUpperCase();
                initialsTextField.setValue(initials);
            }
        });
        passwordField.addValueChangeListener(e -> dirty = true);
    }

    /**
     * Validates user authentication and loads user data before entering the view.
     * Checks for admin access and loads user data based on username query parameter.
     * 
     * @param event navigation event containing routing information and query parameters
     */
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

    /**
     * Prompts user to confirm navigation when there are unsaved changes.
     * Displays confirmation dialog asking if user wants to leave without saving.
     * 
     * @param event navigation event that can be postponed for user confirmation
     */
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

    /**
     * Loads and displays user data for the specified username.
     * Searches for user in database and populates form fields with user data.
     * 
     * @param username the username to search for and load
     */
    public void loadUserByUsername(String username) {
        List<User> users = userService.findByUsername(username);
        if (!users.isEmpty()) {
            this.user = users.get(0);
            setUserData(this.user);
            dirty = false;
        }
    }



    /**
     * Populates form fields with data from the specified user.
     * Shows and sets max hours combo box for StudentWorker users only.
     * 
     * @param user the user whose data will populate the form fields
     */
    private void setUserData(User user) {
        emailTextField.setValue(user.getEmail());
        usernameTextField.setValue(user.getUsername());
        usernameTextField.getStyle().set("color", "#156fabff");
        initialsTextField.setValue(user.getInitials());
        passwordField.setValue(user.getPassword());
        
        if (user instanceof StudentWorker) {
            StudentWorker sw = (StudentWorker) user;
            int maxHours = sw.getMax_hours();
            String selection = "International (20)";
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

    /**
     * Creates and initializes all UI components for the edit user view.
     * Configures drawer navigation, header, form fields, and buttons with appropriate styling.
     */
    private void create_elements() {
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

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutBtn.addClickListener(e -> Auth.logoutToLogin());

        H2 navTitle = new H2("Edit User Data");
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
        layoutRow6.addClassName(Gap.MEDIUM);
        layoutRow6.setWidth("100%");
        layoutRow6.getStyle().set("flex-grow", "1");
        layoutColumn8.setHeightFull();
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
        maxHoursComboBox.setVisible(false);
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
     * Validates all form fields for required values and proper format.
     * Sets error messages on invalid fields.
     * 
     * @return true if all fields are valid, false otherwise
     */
    private boolean validateFields() {
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

    /**
     * Handles save button click by validating and persisting user data.
     * Updates email, password, and max hours (for StudentWorker) and navigates back to user list.
     */
    private void save_button_click_listener() 
    {
        if(validateFields()) {
            if (user != null) {
                try {
                    user.setEmail(emailTextField.getValue().toLowerCase());
                    user.setPassword(passwordField.getValue());
                    
                    if (user instanceof StudentWorker) {
                        StudentWorker sw = (StudentWorker) user;
                        String maxHoursSelection = maxHoursComboBox.getValue();
                        int maxHours = 20;
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
                    UI.getCurrent().navigate("list-users");
                } catch (Exception e) {
                    Notification.show("Error saving user: " + e.getMessage(), 
                        3000, Notification.Position.MIDDLE);
                }
            }
        }   
    }

    /**
     * Handles cancel button click by clearing dirty flag and navigating back to user list.
     */
    private void cancel_button_click_listener() 
    {
        dirty = false;
        UI.getCurrent().navigate("list-users");
    }

    /**
     * Handles delete button click by showing confirmation dialog.
     * Deletes user and associated shifts if confirmed, then navigates back to user list.
     */
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
