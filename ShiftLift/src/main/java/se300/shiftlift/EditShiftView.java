package se300.shiftlift;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("Edit Shift")
@Route("edit-shift")
@RolesAllowed("ALL")
public class EditShiftView extends Composite<VerticalLayout> implements BeforeEnterObserver, BeforeLeaveObserver {

    //Add Attribute Components
    private VerticalLayout mainContainer = new VerticalLayout();

    private Button logoutButton = new Button("Logout");
    private Button addShiftButton = new Button("Add Shift");
    private Button cancelButton = new Button("Cancel");
    private Button deleteButton = new Button("Delete");
    private DatePicker shiftDatePicker = new DatePicker("Shift Date");
    private ComboBox<User> workerComboBox = new ComboBox<>("Select Worker");
    private ComboBox<Workstation> workstationComboBox = new ComboBox<>("Select Workstation");
    private ComboBox<String> startTimeComboBox = new ComboBox<>("Start Time");
    private ComboBox<String> endTimeComboBox = new ComboBox<>("End Time");



    //Attribute Objects
    private User currentUser;
    private Shift currentShift = null;
    private final UserService userService;
    private final WorkstationService workstationService;
    private final ShiftService shiftService;
    private final ScheduleService scheduleService;
    private final BlockedDateService blockedDateService;
    private final StudentBlockedDateService studentBlockedDateService;
    private boolean dirty = false;




    public EditShiftView(UserService userService, WorkstationService workstationService, ShiftService shiftService, ScheduleService scheduleService, BlockedDateService blockedDateService, StudentBlockedDateService studentBlockedDateService) {
        this.userService = userService;
        this.workstationService = workstationService;
        this.shiftService = shiftService;
        this.scheduleService = scheduleService;
        this.blockedDateService = blockedDateService;
        this.studentBlockedDateService = studentBlockedDateService;
        currentUser = Auth.getCurrentUser();//Get Current user from Vaadin Session

        createElements();

    }

    public void createElements()
    {
        //Setup
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        getContent().setAlignItems(Alignment.CENTER); // Center all content horizontally

        // Title for topBar
        H2 topTitle = new H2("Edit Shift");
        topTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

        //Logout Button
        logoutButton.getStyle()
            .set("color", "#666666")
            .set("font-faminly", "Poppins, sans-serif");
        logoutButton.addClickListener(e -> Auth.logoutToLogin());
        HorizontalLayout topBar = new HorizontalLayout(topTitle, logoutButton);
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.CENTER);
        topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        topBar.setPadding(true);
        topBar.setSpacing(true);
        topBar.getStyle()
            .set("margin", "0")
            .set("background-color", "white")
            .set("padding", "16px 20px");
        getContent().add(topBar);


        
        //Main Container Setup - limit to 1/3 of screen width and center
        mainContainer.setMaxWidth("33.33vw"); // Max 1/3 of screen width
        mainContainer.setMinWidth("300px"); // Minimum width for mobile/small screens
        mainContainer.setAlignItems(Alignment.STRETCH);
        mainContainer.setJustifyContentMode(JustifyContentMode.CENTER);

        //Elements setup
        shiftDatePicker.setWidthFull();
        shiftDatePicker.setLabel("Date:");
        setDatePickerConstraints();
        shiftDatePicker.getStyle()
            .set("font-family", "Poppins, sans-serif");


        
        //Populate Worker ComboBox based on user role
        workerComboBox.setWidthFull();
        if (Auth.isAdmin()) {
            // Admin can select any user
            workerComboBox.setItems(userService.list(Pageable.unpaged()));
        } else {
            // Non-admin can only select themselves
            workerComboBox.setItems(Collections.singletonList(currentUser));
        }
        workerComboBox.setItemLabelGenerator(User::getUsername); // Display only username
        workerComboBox.setValue(currentUser); // Preselect the current user
        workerComboBox.setLabel("Worker: ");
        workerComboBox.setAllowCustomValue(false); // Disable text editing
        workerComboBox.getStyle()
            .set("font-family", "Poppins, sans-serif");
        
        //Populate Workstation ComboBox
        workstationComboBox.setWidthFull();
        workstationComboBox.setItems(workstationService.list(Pageable.unpaged()));
        workstationComboBox.setItemLabelGenerator(Workstation::getName); // Display only workstation name
        workstationComboBox.setAllowCustomValue(false); // Disable text editing
        workstationComboBox.getStyle()
            .set("font-family", "Poppins, sans-serif");
        workstationComboBox.setLabel("Workstation: ");
        
        // Add listener to update time options when workstation changes
        workstationComboBox.addValueChangeListener(e -> updateTimeOptionsIfNeeded(e.getOldValue(), e.getValue()));

        //Setup Start time ComboBox
        startTimeComboBox.setWidthFull();
        startTimeComboBox.setLabel("Start Time:");
        startTimeComboBox.setItems(generateTimeOptions());
        startTimeComboBox.setAllowCustomValue(true); // Allow manual time entry
        startTimeComboBox.setPlaceholder("Select or enter start time (HH:MM)");
        startTimeComboBox.getStyle()
            .set("font-family", "Poppins, sans-serif");
        startTimeComboBox.addValueChangeListener(e -> validateTimes());
        startTimeComboBox.addCustomValueSetListener(e -> {
            String customValue = e.getDetail();
            startTimeComboBox.setValue(customValue);
        });

        //Setup End time ComboBox
        endTimeComboBox.setWidthFull();
        endTimeComboBox.setLabel("End Time:");
        endTimeComboBox.setItems(generateTimeOptions());
        endTimeComboBox.setAllowCustomValue(true); // Allow manual time entry
        endTimeComboBox.setPlaceholder("Select or enter end time (HH:MM)");
        endTimeComboBox.getStyle()
            .set("font-family", "Poppins, sans-serif");
        endTimeComboBox.addValueChangeListener(e -> validateTimes());
        endTimeComboBox.addCustomValueSetListener(e -> {
            String customValue = e.getDetail();
            endTimeComboBox.setValue(customValue);
        });

        //Add components to main container
        mainContainer.add(shiftDatePicker, workerComboBox, workstationComboBox, startTimeComboBox, endTimeComboBox);

        //Add buttons and routes - make buttons together as wide as combo boxes
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("gap", "12px"); // Add spacing between buttons
        
        // Style buttons to each take half the available width
        addShiftButton.setWidth("calc(50% - 6px)"); // Subtract half the gap
        addShiftButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif");
        addShiftButton.addClickListener(e -> save_button_click_listener());
        
        cancelButton.setWidth("calc(50% - 6px)"); // Subtract half the gap
        cancelButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#666666");
        cancelButton.addClickListener(e -> cancel_button_click_listener());

       
        deleteButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "white")
            .set("background-color", "#9b0000ff");
        deleteButton.addClickListener(e -> delete_button_click_listener());

        
        buttonLayout.add(addShiftButton, cancelButton);
        mainContainer.add(deleteButton);
        mainContainer.add(buttonLayout);
        getContent().add(mainContainer);
        getContent().setHorizontalComponentAlignment(Alignment.CENTER, mainContainer); // Center the main container

        
    }

    private void setDatePickerConstraints() {
        try {
            var scheduleOpt = scheduleService.getLatestUnpublishedSchedule();
            
            if (scheduleOpt.isEmpty()) {
                // No unpublished schedule found - disable date picker
                shiftDatePicker.setMin(LocalDate.now().plusYears(100)); // Effectively disable
                shiftDatePicker.setMax(LocalDate.now().plusYears(100));
                shiftDatePicker.setHelperText("No unpublished schedule found. Please create a schedule first.");
                return;
            }
            
            Schedule currentSchedule = scheduleOpt.get();
            Date startDate = currentSchedule.getStartDate();
            Date endDate = currentSchedule.getEndDate();
            
            if (startDate != null && endDate != null) {
                LocalDate minDate = LocalDate.of(startDate.get_year(), startDate.get_month(), startDate.get_day());
                LocalDate maxDate = LocalDate.of(endDate.get_year(), endDate.get_month(), endDate.get_day());
                
                shiftDatePicker.setMin(minDate);
                shiftDatePicker.setMax(maxDate);
                shiftDatePicker.setHelperText(String.format("Select date between %s and %s", 
                    minDate.toString(), maxDate.toString()));
            } else {
                // Fallback if dates are null
                shiftDatePicker.setMin(LocalDate.now());
                shiftDatePicker.setMax(LocalDate.now().plusDays(30));
                shiftDatePicker.setHelperText("Schedule dates not properly set.");
            }
        } catch (Exception e) {
            // Fallback on error
            shiftDatePicker.setMin(LocalDate.now());
            shiftDatePicker.setMax(LocalDate.now().plusDays(30));
            shiftDatePicker.setHelperText("Error loading schedule dates.");
        }
    }

    private void delete_button_click_listener() {
        if (currentShift == null) {
            Notification.show("No shift to delete", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        try {
            shiftService.deleteShift(currentShift);
            dirty = false;
            Notification.show("Shift deleted successfully!", 3000, Notification.Position.BOTTOM_START);
            UI.getCurrent().navigate("main-menu");
        } catch (Exception e) {
            Notification.show("Error deleting shift: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void loadShiftById(Long shiftId) {
        try {
            List<Shift> allShifts = shiftService.getAllShifts();
            currentShift = allShifts.stream()
                .filter(s -> s.getId().equals(shiftId))
                .findFirst()
                .orElse(null);
            
            if (currentShift != null) {
                populateFormWithShift(currentShift);
            } else {
                Notification.show("Shift not found", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show("Error loading shift: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void populateFormWithShift(Shift shift) {
        // Set date
        Date shiftDate = shift.getDate();
        shiftDatePicker.setValue(LocalDate.of(
            shiftDate.get_year(),
            shiftDate.get_month(),
            shiftDate.get_day()
        ));
        
        // Set worker
        workerComboBox.setValue(shift.getStudentWorker());
        
        // Set workstation
        workstationComboBox.setValue(shift.getWorkstation());
        
        // Set times
        Time shiftTime = shift.getTime();
        startTimeComboBox.setValue(formatTimeForDisplay(shiftTime.getStart_time()));
        endTimeComboBox.setValue(formatTimeForDisplay(shiftTime.getEnd_time()));
        
        // Update button text
        addShiftButton.setText("Save Changes");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn()) {
            Notification.show("Please Log-in", 2000, Notification.Position.MIDDLE);
            event.rerouteTo("");
            return;
        }
        
        // Load shift from URL parameter
        java.util.List<String> params = event.getLocation().getQueryParameters().getParameters().get("shiftId");
        if (params != null && !params.isEmpty()) {
            String shiftIdStr = params.get(0);
            if (shiftIdStr != null && !shiftIdStr.isEmpty()) {
                try {
                    Long shiftId = Long.parseLong(shiftIdStr);
                    loadShiftById(shiftId);
                } catch (NumberFormatException e) {
                    Notification.show("Invalid shift ID", 3000, Notification.Position.MIDDLE);
                }
            }
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        // TODO Auto-generated method stub
        
    }

    private void save_button_click_listener()
    {
        if(validateFields())
        {
            try {
                Date shiftDate = new Date(
                    shiftDatePicker.getValue().getDayOfMonth(), 
                    shiftDatePicker.getValue().getMonthValue(),
                    shiftDatePicker.getValue().getYear());
                
                // Check if the date is blocked by manager
                if (blockedDateService.isDateBlocked(shiftDate)) {
                    Notification.show("Cannot save shift: This date has been blocked by a manager.", 
                        4000, Notification.Position.MIDDLE);
                    return;
                }
                
                // Check if the selected worker has blocked this date
                User selectedWorker = workerComboBox.getValue();
                if (selectedWorker instanceof StudentWorker) {
                    StudentWorker studentWorker = (StudentWorker) selectedWorker;
                    if (studentBlockedDateService.isDateBlocked(studentWorker, shiftDate)) {
                        Notification.show(String.format("Cannot save shift: %s has marked themselves unavailable on this date.",
                            studentWorker.getUsername()),
                            4000, Notification.Position.MIDDLE);
                        return;
                    }
                }
                
                Time shiftTime = new Time(
                    parseTimeFromString(startTimeComboBox.getValue()),
                    parseTimeFromString(endTimeComboBox.getValue())
                );
                
                // Check if worker is double booked
                Long excludeShiftId = (currentShift != null) ? currentShift.getId() : null;
                if (shiftService.workerDoubleBooked(selectedWorker, shiftDate, shiftTime, excludeShiftId)) {
                    Notification.show("Selected worker is already scheduled for another shift at this date and time.", 
                        4000, Notification.Position.MIDDLE);
                    return;
                }
                
                // Check if the worker is a StudentWorker and if this shift would exceed max hours
                if (selectedWorker instanceof StudentWorker) {
                    StudentWorker studentWorker = (StudentWorker) selectedWorker;
                    if (shiftService.wouldExceedMaxHours(studentWorker, shiftDate, shiftTime, excludeShiftId)) {
                        double currentHours = shiftService.getWeeklyHours(studentWorker, shiftDate, excludeShiftId);
                        double shiftHours = shiftTime.getDurationInHours();
                        double totalHours = currentHours + shiftHours;
                        
                        Notification.show(String.format(
                            "Cannot save shift: %s is already scheduled for %.1f hours this week. " +
                            "This %.1f hour shift would total %.1f hours, exceeding their max of %d hours.",
                            studentWorker.getUsername(), currentHours, shiftHours, totalHours, studentWorker.getMax_hours()
                        ), 5000, Notification.Position.MIDDLE);
                        return;
                    }
                }
                
                // Check if workstation is occupied and handle admin/senior override
                if (shiftService.workstationOcupied(workstationComboBox.getValue(), shiftDate, shiftTime, excludeShiftId)) {
                    // Get the conflicting shift
                    Shift conflictingShift = shiftService.getConflictingShift(workstationComboBox.getValue(), shiftDate, shiftTime);
                    
                    // Make sure it's not the same shift being edited
                    if (conflictingShift != null && (excludeShiftId == null || !conflictingShift.getId().equals(excludeShiftId))) {
                        User currentWorker = workerComboBox.getValue();
                        User conflictingWorker = conflictingShift.getStudentWorker();
                        
                        // Check if logged in user is an admin
                        if (Auth.isAdmin()) {
                            // Admin override - show confirmation dialog
                            Long availableWorkstationId = shiftService.workstationAvailable(shiftDate, shiftTime);
                            
                            ConfirmDialog dialog = new ConfirmDialog();
                            dialog.setHeader("Admin Override - Workstation Conflict");
                            
                            if (availableWorkstationId != null) {
                                dialog.setText(String.format(
                                    "The selected workstation is currently assigned to %s. " +
                                    "As an admin, you can override this and reassign %s to an available workstation. " +
                                    "Do you want to proceed?",
                                    conflictingWorker.getUsername(),
                                    conflictingWorker.getUsername()
                                ));
                            } else {
                                dialog.setText(String.format(
                                    "WARNING: The selected workstation is currently assigned to %s and NO other workstations are available. " +
                                    "As an admin, you can override this, but %s's shift will be DELETED. " +
                                    "Do you want to proceed?",
                                    conflictingWorker.getUsername(),
                                    conflictingWorker.getUsername()
                                ));
                            }
                            
                            dialog.setCancelable(true);
                            dialog.setConfirmText("Override");
                            dialog.setCancelText("Cancel");
                            
                            dialog.addConfirmListener(event -> {
                                try {
                                    if (availableWorkstationId != null) {
                                        // Reassign conflicting worker to available workstation
                                        Workstation newWorkstation = workstationService.findById(availableWorkstationId).orElse(null);
                                        
                                        if (newWorkstation != null) {
                                            shiftService.updateShift(
                                                conflictingShift,
                                                conflictingShift.getDate(),
                                                conflictingShift.getStudentWorker(),
                                                newWorkstation,
                                                conflictingShift.getTime()
                                            );
                                            
                                            Notification.show(String.format("Admin override: %s reassigned to another workstation.", 
                                                conflictingWorker.getUsername()), 
                                                3000, Notification.Position.MIDDLE);
                                        }
                                    } else {
                                        // No available workstation - delete the conflicting shift
                                        shiftService.deleteShift(conflictingShift);
                                        
                                        Notification.show(String.format("Admin override: %s's shift was deleted (no available workstation for reassignment).", 
                                            conflictingWorker.getUsername()), 
                                            4000, Notification.Position.MIDDLE);
                                    }
                                    
                                    // Update current shift
                                    if (currentShift != null) {
                                        shiftService.updateShift(
                                            currentShift,
                                            shiftDate,
                                            currentWorker,
                                            workstationComboBox.getValue(),
                                            shiftTime
                                        );
                                    } else {
                                        shiftService.addShift(
                                            shiftDate,
                                            currentWorker,
                                            workstationComboBox.getValue(),
                                            shiftTime
                                        );
                                    }
                                    
                                    dirty = false;
                                    Notification.show("Shift updated successfully!", 
                                        3000, Notification.Position.BOTTOM_START);
                                    UI.getCurrent().navigate("main-menu");
                                } catch (Exception ex) {
                                    Notification.show("Error during admin override: " + ex.getMessage(), 
                                        4000, Notification.Position.MIDDLE);
                                }
                            });
                            
                            dialog.open();
                            return; // Exit early, dialog handles the rest
                        }
                        // Check if current worker is more senior (non-admin path)
                        else if (shiftService.isSenior(currentWorker, conflictingWorker)) {
                            // Find an available workstation for the conflicting shift
                            Long availableWorkstationId = shiftService.workstationAvailable(shiftDate, shiftTime);
                            
                            if (availableWorkstationId != null) {
                                // Show confirmation dialog for override
                                ConfirmDialog dialog = new ConfirmDialog();
                                dialog.setHeader("Senior Override");
                                dialog.setText(String.format(
                                    "You have higher seniority than %s. Would you like to take this workstation? " +
                                    "The other worker will be reassigned to an available workstation.",
                                    conflictingWorker.getUsername()
                                ));
                                
                                dialog.setCancelable(true);
                                dialog.setConfirmText("Override");
                                dialog.setCancelText("Cancel");
                                
                                dialog.addConfirmListener(event -> {
                                    try {
                                        // Find the available workstation
                                        Workstation newWorkstation = workstationService.findById(availableWorkstationId).orElse(null);
                                        
                                        if (newWorkstation != null) {
                                            // Update conflicting shift to new workstation
                                            shiftService.updateShift(
                                                conflictingShift,
                                                conflictingShift.getDate(),
                                                conflictingShift.getStudentWorker(),
                                                newWorkstation,
                                                conflictingShift.getTime()
                                            );
                                            
                                            // Update current shift
                                            if (currentShift != null) {
                                                shiftService.updateShift(
                                                    currentShift,
                                                    shiftDate,
                                                    currentWorker,
                                                    workstationComboBox.getValue(),
                                                    shiftTime
                                                );
                                            } else {
                                                shiftService.addShift(
                                                    shiftDate,
                                                    currentWorker,
                                                    workstationComboBox.getValue(),
                                                    shiftTime
                                                );
                                            }
                                            
                                            dirty = false;
                                            Notification.show("Shift updated successfully! Previous worker reassigned.", 
                                                3000, Notification.Position.BOTTOM_START);
                                            UI.getCurrent().navigate("main-menu");
                                        }
                                    } catch (Exception ex) {
                                        Notification.show("Error during override: " + ex.getMessage(), 
                                            4000, Notification.Position.MIDDLE);
                                    }
                                });
                                
                                dialog.open();
                                return; // Exit early, dialog handles the rest
                            } else {
                                Notification.show("Cannot override: No other workstation available for reassignment.", 
                                    4000, Notification.Position.MIDDLE);
                                return;
                            }
                        } else {
                            Notification.show("Workstation is occupied and you do not have seniority override privileges.", 
                                4000, Notification.Position.MIDDLE);
                            return;
                        }
                    }
                }
                
                if (currentShift != null) {
                    // Update existing shift
                    shiftService.updateShift(
                        currentShift,
                        shiftDate,
                        workerComboBox.getValue(),
                        workstationComboBox.getValue(),
                        shiftTime
                    );
                    dirty = false;
                    Notification.show("Shift updated successfully!", 3000, Notification.Position.BOTTOM_START);
                } else {
                    // Create new shift
                    shiftService.addShift(
                        shiftDate,
                        workerComboBox.getValue(),
                        workstationComboBox.getValue(),
                        shiftTime
                    );
                    dirty = false;
                    Notification.show("Shift created successfully!", 3000, Notification.Position.BOTTOM_START);
                }
                
                UI.getCurrent().navigate("main-menu");
                
            } catch (Exception e) {
                Notification.show("Error saving shift: " + e.getMessage(), 
                    4000, Notification.Position.MIDDLE);
            }
        }
    }

    private void cancel_button_click_listener()
    {
        dirty = false;
        UI.getCurrent().navigate("main-menu");
    }


    private boolean validateFields()
    {
        if (shiftDatePicker.getValue() == null) {
            Notification.show("Please select a date", 3000, Notification.Position.MIDDLE);
            return false;
        }
        if (workerComboBox.getValue() == null) {
            Notification.show("Please select a worker", 3000, Notification.Position.MIDDLE);
            return false;
        }
        if (workstationComboBox.getValue() == null) {
            Notification.show("Please select a workstation", 3000, Notification.Position.MIDDLE);
            return false;
        }
        if (startTimeComboBox.getValue() == null) {
            Notification.show("Please select a start time", 3000, Notification.Position.MIDDLE);
            return false;
        }
        if (endTimeComboBox.getValue() == null) {
            Notification.show("Please select an end time", 3000, Notification.Position.MIDDLE);
            return false;
        }
        return validateTimes();
    }
    
    private List<String> generateTimeOptions() {
        List<String> timeOptions = new ArrayList<>();
        // Generate times from 7:30 AM to 5:30 PM in 30-minute intervals
        int startHour = Time.OPENING_TIME / 100; // Extract hour from OPENING_TIME (800 -> 8)
        int startMinute = Time.OPENING_TIME % 100;
        int endHour = Time.CLOSING_TIME / 100;   // Extract hour from CLOSING_TIME (1700 -> 17)
        int endMinute = Time.CLOSING_TIME % 100;

        //Edited heavily in in an update
        for (int hour = startHour; hour <= endHour; hour++) {
            if (hour == startHour) {
                if (startMinute == 0) {
                    timeOptions.add(String.format("%02d:00", hour));
                }
                timeOptions.add(String.format("%02d:30", hour));
            }
            else if (hour == endHour) {
                if (endMinute == 30) {
                    timeOptions.add(String.format("%02d:00", hour));
                }
            }
            else {
                timeOptions.add(String.format("%02d:00", hour));
                timeOptions.add(String.format("%02d:30", hour));
            }
        }
        return timeOptions;
    }
    
    private List<String> generateTimeOptionsForWorkstation(int startTime, int endTime) {
        /*List<String> timeOptions = new ArrayList<>();
        
        // Generate time options in 30-minute intervals within workstation hours
        int currentTime = startTime;
        while (currentTime <= endTime) {
            int hours = currentTime / 100;
            int minutes = currentTime % 100;
            timeOptions.add(String.format("%02d:%02d", hours, minutes));
            
            // Add 30 minutes
            minutes += 30;
            if (minutes >= 60) {
                hours++;
                minutes = 0;
            }
            currentTime = hours * 100 + minutes;
            
            // Break if we've reached the end time
            if (currentTime > endTime) {
                break;
            }
        }*/

        List<String> timeOptions = new ArrayList<>();
        // Generate times from 7:30 AM to 5:30 PM in 30-minute intervals
        int startHour = startTime / 100; // Extract hour from OPENING_TIME (800 -> 8)
        int startMinute = startTime % 100;
        int endHour = endTime / 100;   // Extract hour from CLOSING_TIME (1700 -> 17)
        int endMinute = endTime % 100;

        //Edited heavily in in an update
        for (int hour = startHour; hour <= endHour; hour++) {
            if (hour == startHour) {
                if (startMinute == 0) {
                    timeOptions.add(String.format("%02d:00", hour));
                }
                timeOptions.add(String.format("%02d:30", hour));
            }
            else if (hour == endHour) {
                if (endMinute == 30) {
                    timeOptions.add(String.format("%02d:00", hour));
                }
            }
            else {
                timeOptions.add(String.format("%02d:00", hour));
                timeOptions.add(String.format("%02d:30", hour));
            }
        }
        
        return timeOptions;
    }
    
    private void updateTimeOptionsIfNeeded(Workstation oldWorkstation, Workstation newWorkstation) {
        // Capture current values BEFORE updating items
        String currentStartTime = startTimeComboBox.getValue();
        String currentEndTime = endTimeComboBox.getValue();
        
        if (newWorkstation != null && newWorkstation.getOperation_hours() != null) {
            Time operatingHours = newWorkstation.getOperation_hours();
            List<String> workstationTimeOptions = generateTimeOptionsForWorkstation(
                operatingHours.getStart_time(), 
                operatingHours.getEnd_time()
            );
            
            // Update combo box items with workstation-specific times
            startTimeComboBox.setItems(workstationTimeOptions);
            endTimeComboBox.setItems(workstationTimeOptions);
            
            // Only update the values if current times fall outside the new workstation's hours
            if (currentStartTime != null && currentEndTime != null) {
                int startTime = parseTimeFromString(currentStartTime);
                int endTime = parseTimeFromString(currentEndTime);
                int workstationStart = operatingHours.getStart_time();
                int workstationEnd = operatingHours.getEnd_time();
                
                // Restore or update start time based on workstation hours
                if (startTime >= workstationStart && startTime <= workstationEnd) {
                    startTimeComboBox.setValue(currentStartTime); // Restore valid time
                } else {
                    startTimeComboBox.setValue(formatTimeForDisplay(workstationStart)); // Use workstation start
                }
                
                // Restore or update end time based on workstation hours
                if (endTime >= workstationStart && endTime <= workstationEnd) {
                    endTimeComboBox.setValue(currentEndTime); // Restore valid time
                } else {
                    endTimeComboBox.setValue(formatTimeForDisplay(workstationEnd)); // Use workstation end
                }
            } else {
                // If no current times set, use workstation defaults
                startTimeComboBox.setValue(formatTimeForDisplay(operatingHours.getStart_time()));
                endTimeComboBox.setValue(formatTimeForDisplay(operatingHours.getEnd_time()));
            }
        } else {
            // Use default time options
            List<String> defaultTimeOptions = generateTimeOptions();
            startTimeComboBox.setItems(defaultTimeOptions);
            endTimeComboBox.setItems(defaultTimeOptions);
            
            // Restore current values if they exist, otherwise use defaults
            if (currentStartTime != null) {
                startTimeComboBox.setValue(currentStartTime);
            } else {
                startTimeComboBox.setValue(formatTimeForDisplay(Time.OPENING_TIME));
            }
            if (currentEndTime != null) {
                endTimeComboBox.setValue(currentEndTime);
            } else {
                endTimeComboBox.setValue(formatTimeForDisplay(Time.CLOSING_TIME));
            }
        }
    }
    
    private boolean validateTimes() {
        if (startTimeComboBox.getValue() == null || endTimeComboBox.getValue() == null) {
            return true; // Skip validation if either time is not selected
        }
        
        Workstation selectedWorkstation = workstationComboBox.getValue();
        if (selectedWorkstation == null) {
            return true; // Skip validation if no workstation selected
        }
        
        int startTime = parseTimeFromString(startTimeComboBox.getValue());
        int endTime = parseTimeFromString(endTimeComboBox.getValue());
        
        // Check if start time is before end time
        if (startTime >= endTime) {
            Notification.show("Start time must be before end time", 3000, Notification.Position.MIDDLE);
            return false;
        }
        
        // Check if times are within workstation operating hours
        if (selectedWorkstation.getOperation_hours() != null) {
            Time operatingHours = selectedWorkstation.getOperation_hours();
            int workstationStart = operatingHours.getStart_time();
            int workstationEnd = operatingHours.getEnd_time();
            
            if (startTime < workstationStart || endTime > workstationEnd) {
                Notification.show("Selected times must be within workstation operating hours: " + 
                    formatTimeForDisplay(workstationStart) + " - " + formatTimeForDisplay(workstationEnd), 
                    4000, Notification.Position.MIDDLE);
                return false;
            }
        }
        
        return true;
    }
    
    private String formatTimeForDisplay(int time) {
        int hours = time / 100;
        int minutes = time % 100;
        return String.format("%02d:%02d", hours, minutes);
    }
    
    private int parseTimeFromString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return Time.OPENING_TIME; // Default to opening time from Time class
        }
        
        try {
            String[] parts = timeStr.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid time format");
            }
            
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            
            // Validate time ranges
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                throw new IllegalArgumentException("Invalid time values");
            }
            
            return hours * 100 + minutes;
        } catch (IllegalArgumentException e) {
            // Show error notification for invalid manual input
            Notification.show("Invalid time format. Please use HH:MM format (e.g., 09:30)", 
                4000, Notification.Position.MIDDLE);
            return Time.OPENING_TIME; // Return default time
        }
    }
}
