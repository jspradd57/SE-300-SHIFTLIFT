package se300.shiftlift;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("NewWorkStationView")
@Route("new-workstation")
@RolesAllowed("ADMIN")
public class NewWorkstationView extends Composite<VerticalLayout> implements BeforeEnterObserver 
{   
    private final HorizontalLayout layoutRow3 = new HorizontalLayout();
    private final HorizontalLayout layoutRow5 = new HorizontalLayout();
    private final VerticalLayout layoutColumn5 = new VerticalLayout();
    private final VerticalLayout layoutColumn7 = new VerticalLayout();
    private final VerticalLayout layoutColumn3 = new VerticalLayout();
    private final H1 h12 = new H1();
    private final HorizontalLayout layoutRow6 = new HorizontalLayout();
    private final VerticalLayout layoutColumn8 = new VerticalLayout();
    private final TextField nameTextField = new TextField();
    private final ComboBox<String> openingTimeComboBox = new ComboBox<>();
    private final ComboBox<String> closingTimeComboBox = new ComboBox<>();
    private final HorizontalLayout layoutRow7 = new HorizontalLayout();
    private final Button button_save = new Button();
    private final Button button_cancel = new Button();
    private final VerticalLayout layoutColumn9 = new VerticalLayout();
    private final HorizontalLayout layoutRow8 = new HorizontalLayout();
    private final VerticalLayout layoutRowButtons = new VerticalLayout();

    private Workstation workstation;
    private boolean dirty = false;
    private final WorkstationService workstationService;
  
    /**
     * Constructs a new workstation view for creating or editing workstations.
     * Initializes the workstation service and creates the UI elements.
     * 
     * @param workstationService the service for managing workstation persistence
     */
    public NewWorkstationView(WorkstationService workstationService)
    {
        this.workstationService = workstationService;
        create_elements();
    }

    /**
     * Validates admin authentication before allowing access to the view.
     * Redirects non-admin users to the login page.
     * 
     * @param event the navigation event containing routing information
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
     * Creates and configures all UI elements for the workstation form.
     * Sets up the layout, input fields, time dropdowns, and action buttons.
     */
    private  void create_elements()
    {
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
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
        h12.setText("Edit Workstation Data");
        h12.setWidth("max-content");
        h12.getStyle().set("font-family", "Poppins, sans-serif");
        h12.getStyle().set("color", "#156fabff");
        layoutRow6.setWidthFull();
        layoutRow6.addClassName(Gap.MEDIUM);
        layoutRow6.setWidth("100%");
        layoutRow6.getStyle().set("flex-grow", "1");
        layoutColumn8.setHeightFull();
        layoutColumn8.setWidth("100%");
        layoutColumn8.getStyle().set("flex-grow", "1");
        nameTextField.setLabel("Workstation Name:");
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, nameTextField);
        nameTextField.setWidth("min-content");
        nameTextField.setErrorMessage("Please enter a valid workstation name");
        nameTextField.setClearButtonVisible(true);
        
        openingTimeComboBox.setLabel("Opening Time:");
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, openingTimeComboBox);
        openingTimeComboBox.setWidth("min-content");
        openingTimeComboBox.setAllowCustomValue(true);
        openingTimeComboBox.setItems(generateTimeOptions());
        openingTimeComboBox.setPlaceholder("Select opening time");
        openingTimeComboBox.setValue(formatTimeForDisplay(Time.OPENING_TIME));
        
        closingTimeComboBox.setLabel("Closing Time:");
        layoutColumn3.setAlignSelf(FlexComponent.Alignment.CENTER, closingTimeComboBox);
        closingTimeComboBox.setWidth("min-content");
        closingTimeComboBox.setAllowCustomValue(true);
        closingTimeComboBox.setItems(generateTimeOptions());
        closingTimeComboBox.setPlaceholder("Select closing time");
        closingTimeComboBox.setValue(formatTimeForDisplay(Time.CLOSING_TIME));
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
        button_save.setText("Save Workstation");
        button_save.setWidth("min-content");
        button_save.getStyle().set("background-color", "#156fabff").set("transition", "all 0.2s");
        button_save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button_save.addClickListener(e -> {
            create_button_click_listener();
        });
        button_cancel.setText("Cancel");
        button_cancel.getStyle().set("color", "grey");
        button_cancel.setWidth("min-content");
        button_cancel.addClickListener(e -> {
            cancel_button_click_listener();
        });
        layoutColumn9.getStyle().set("flex-grow", "1");
        layoutRow8.addClassName(Gap.MEDIUM);
        layoutRow8.setWidth("100%");
        layoutRow8.setHeight("min-content");
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyle().set("color", "#666666");
        logoutBtn.addClickListener(e -> Auth.logoutToLogin());
        HorizontalLayout topBar = new HorizontalLayout(logoutBtn);
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.CENTER);
        topBar.setJustifyContentMode(JustifyContentMode.END);
        topBar.setPadding(false);
        topBar.setSpacing(false);
        topBar.getStyle().set("margin", "0");
        getContent().add(topBar);

        getContent().add(layoutRow3);
        getContent().add(layoutRow5);
        layoutRow5.add(layoutColumn5);
        layoutRow5.add(layoutColumn7);
        h12.getStyle().set("margin", "0 0 24px 0");
        layoutColumn7.add(h12);
        layoutColumn7.add(layoutRow6);
        layoutRow6.add(layoutColumn8);
        layoutColumn8.add(nameTextField);
        layoutColumn8.add(openingTimeComboBox);
        layoutColumn8.add(closingTimeComboBox);
        layoutColumn8.add(layoutRowButtons);
        
        layoutRow7.add(button_save);
        layoutRow7.add(button_cancel);
        layoutRowButtons.add(layoutRow7);
        layoutRow5.add(layoutColumn9);
        getContent().add(layoutRow8);
    }

    /**
     * Formats an integer time value to a display string.
     * 
     * @param time the time value (e.g., 800 for 8:00, 1730 for 17:30)
     * @return the formatted time string in HH:mm format
     */
    private String formatTimeForDisplay(int time) {
        int hours = time / 100;
        int minutes = time % 100;
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Parses a time string into an integer time value.
     * Returns the default opening time if the string is null or empty.
     * 
     * @param timeStr the time string in HH:mm format
     * @return the integer time value (e.g., 800 for 8:00)
     */
    private int parseTimeFromString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return Time.OPENING_TIME;
        }
        String[] parts = timeStr.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours * 100 + minutes;
    }

    /**
     * Generates a list of time options for the time selection dropdowns.
     * Creates 30-minute intervals between opening and closing times.
     * 
     * @return list of time strings in HH:mm format
     */
    private java.util.List<String> generateTimeOptions() {
        java.util.List<String> timeOptions = new java.util.ArrayList<>();
        int startHour = Time.OPENING_TIME / 100;
        int endHour = Time.CLOSING_TIME / 100;
        
        for (int hour = startHour; hour <= endHour; hour++) {
            timeOptions.add(String.format("%02d:00", hour));
            if (hour < endHour) {
                timeOptions.add(String.format("%02d:30", hour));
            }
        }
        return timeOptions;
    }

    /**
     * Validates all form fields for completeness and logical consistency.
     * Checks workstation name, opening time, closing time, and time range validity.
     * 
     * @return true if all fields are valid, false otherwise
     */
    private boolean validateFields() {
        if(nameTextField.isEmpty() || nameTextField.getValue().trim().isEmpty()) {
            nameTextField.setErrorMessage("Workstation name cannot be empty");
            nameTextField.setInvalid(true);
            return false;
        }
        
        if(openingTimeComboBox.isEmpty() || openingTimeComboBox.getValue() == null) {
            openingTimeComboBox.setErrorMessage("Opening time must be selected");
            openingTimeComboBox.setInvalid(true);
            return false;
        }
        
        if(closingTimeComboBox.isEmpty() || closingTimeComboBox.getValue() == null) {
            closingTimeComboBox.setErrorMessage("Closing time must be selected");
            closingTimeComboBox.setInvalid(true);
            return false;
        }
        
        try {
            int openingTime = parseTimeFromString(openingTimeComboBox.getValue());
            int closingTime = parseTimeFromString(closingTimeComboBox.getValue());
            
            if (openingTime >= closingTime) {
                closingTimeComboBox.setErrorMessage("Closing time must be after opening time");
                closingTimeComboBox.setInvalid(true);
                return false;
            }
            
            if (openingTime < Time.OPENING_TIME || closingTime > Time.CLOSING_TIME) {
                openingTimeComboBox.setErrorMessage("Operating hours must be between " + 
                    formatTimeForDisplay(Time.OPENING_TIME) + " and " + formatTimeForDisplay(Time.CLOSING_TIME));
                openingTimeComboBox.setInvalid(true);
                return false;
            }
        } catch (Exception e) {
            openingTimeComboBox.setErrorMessage("Invalid time format");
            openingTimeComboBox.setInvalid(true);
            return false;
        }
        
        return true;
    }

    /**
     * Handles the save button click to create or update a workstation.
     * Validates fields, parses time values, and persists the workstation.
     */
    private void create_button_click_listener() 
    {
        if(validateFields()) {
            if (workstation != null) {
                try {
                    workstation.setName(nameTextField.getValue().trim());
                    
                    int openingTime = parseTimeFromString(openingTimeComboBox.getValue());
                    int closingTime = parseTimeFromString(closingTimeComboBox.getValue());
                    
                    workstation.setOperation_hours(new Time(openingTime, closingTime));
                    
                    workstationService.save(workstation);
                    dirty = false;
                    Notification.show("Workstation saved", 2000, Notification.Position.BOTTOM_START);
                    UI.getCurrent().navigate("list-workstations");
                } catch (Exception e) {
                    Notification.show("Error saving workstation: " + e.getMessage(), 
                        3000, Notification.Position.MIDDLE);
                }
            } else {
                try {
                    workstation = new Workstation(nameTextField.getValue().trim());
                    
                    int openingTime = parseTimeFromString(openingTimeComboBox.getValue());
                    int closingTime = parseTimeFromString(closingTimeComboBox.getValue());
                    
                    workstation.setOperation_hours(new Time(openingTime, closingTime));
                    
                    workstationService.save(workstation);
                    dirty = false;
                    Notification.show("Workstation created", 2000, Notification.Position.BOTTOM_START);
                    UI.getCurrent().navigate("list-workstations");
                } catch (Exception e) {
                    Notification.show("Error creating workstation: " + e.getMessage(), 
                        3000, Notification.Position.MIDDLE);
                }
            }
        }   
    }

    /**
     * Handles the cancel button click to abort workstation creation/editing.
     * Clears the dirty flag and navigates back to the workstation list.
     */
    private void cancel_button_click_listener() 
    {
        dirty = false;
        UI.getCurrent().navigate("list-workstations");
    }
    
}
