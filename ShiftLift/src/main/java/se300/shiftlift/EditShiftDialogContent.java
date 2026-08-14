package se300.shiftlift;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class EditShiftDialogContent extends VerticalLayout {
    
    private final DatePicker shiftDatePicker = new DatePicker("Shift Date");
    private final ComboBox<User> workerComboBox = new ComboBox<>("Select Worker");
    private final ComboBox<Workstation> workstationComboBox = new ComboBox<>("Select Workstation");
    private final ComboBox<String> startTimeComboBox = new ComboBox<>("Start Time");
    private final ComboBox<String> endTimeComboBox = new ComboBox<>("End Time");
    private final Button saveButton = new Button("Save Changes");
    private final Button deleteButton = new Button("Delete");
    private final Button cancelButton = new Button("Cancel");
    
    private final User currentUser;
    private Shift currentShift = null;
    private final UserService userService;
    private final WorkstationService workstationService;
    private final ShiftService shiftService;
    private final ScheduleService scheduleService;
    private final BlockedDateService blockedDateService;
    private final StudentBlockedDateService studentBlockedDateService;
    private final Runnable onSuccess;
    private final boolean useUnpublishedSchedule;
    
    /**
     * Constructs an edit shift dialog for modifying an existing shift.
     * Loads the specified shift and initializes the form with its data.
     * 
     * @param userService the service for managing users
     * @param workstationService the service for managing workstations
     * @param shiftService the service for managing shifts
     * @param scheduleService the service for managing schedules
     * @param blockedDateService the service for managing blocked dates
     * @param studentBlockedDateService the service for managing student blocked dates
     * @param shiftId the ID of the shift to edit
     * @param useUnpublishedSchedule if true, uses unpublished schedule dates; if false, uses published schedule dates
     * @param onSuccess callback to run when the shift is successfully saved or dialog is closed
     */
    public EditShiftDialogContent(
            UserService userService,
            WorkstationService workstationService,
            ShiftService shiftService,
            ScheduleService scheduleService,
            BlockedDateService blockedDateService,
            StudentBlockedDateService studentBlockedDateService,
            Long shiftId,
            boolean useUnpublishedSchedule,
            Runnable onSuccess) {
        
        this.userService = userService;
        this.workstationService = workstationService;
        this.shiftService = shiftService;
        this.scheduleService = scheduleService;
        this.blockedDateService = blockedDateService;
        this.studentBlockedDateService = studentBlockedDateService;
        this.currentUser = Auth.getCurrentUser();
        this.useUnpublishedSchedule = useUnpublishedSchedule;
        this.onSuccess = onSuccess;
        
        createContent();
        loadShiftById(shiftId);
    }
    
    /**
     * Creates and configures all UI elements for the edit shift form.
     * Sets up date picker, worker/workstation selectors, time inputs, and action buttons.
     */
    private void createContent() {
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.STRETCH);
        setMaxWidth("600px");
        
        H3 title = new H3("Edit Shift");
        title.getStyle()
            .set("color", "#156fabff")
            .set("font-family", "Poppins, sans-serif")
            .set("margin", "0 0 16px 0");
        
        shiftDatePicker.setWidthFull();
        setDatePickerConstraints();
        shiftDatePicker.getStyle().set("font-family", "Poppins, sans-serif");
        
        workerComboBox.setWidthFull();
        if (Auth.isAdmin()) {
            workerComboBox.setItems(userService.list(Pageable.unpaged()));
        } else {
            workerComboBox.setItems(Collections.singletonList(currentUser));
        }
        workerComboBox.setItemLabelGenerator(User::getUsername);
        workerComboBox.setValue(currentUser);
        workerComboBox.setAllowCustomValue(false);
        workerComboBox.getStyle().set("font-family", "Poppins, sans-serif");
        
        workstationComboBox.setWidthFull();
        workstationComboBox.setItems(workstationService.list(Pageable.unpaged()));
        workstationComboBox.setItemLabelGenerator(Workstation::getName);
        workstationComboBox.setAllowCustomValue(false);
        workstationComboBox.getStyle().set("font-family", "Poppins, sans-serif");
        workstationComboBox.addValueChangeListener(e -> updateTimeOptionsIfNeeded(e.getOldValue(), e.getValue()));
        
        startTimeComboBox.setWidthFull();
        startTimeComboBox.setItems(generateTimeOptions());
        startTimeComboBox.setAllowCustomValue(true);
        startTimeComboBox.setPlaceholder("Select or enter start time (HH:MM)");
        startTimeComboBox.getStyle().set("font-family", "Poppins, sans-serif");
        startTimeComboBox.addValueChangeListener(e -> validateTimes());
        startTimeComboBox.addCustomValueSetListener(e -> startTimeComboBox.setValue(e.getDetail()));
        
        endTimeComboBox.setWidthFull();
        endTimeComboBox.setItems(generateTimeOptions());
        endTimeComboBox.setAllowCustomValue(true);
        endTimeComboBox.setPlaceholder("Select or enter end time (HH:MM)");
        endTimeComboBox.getStyle().set("font-family", "Poppins, sans-serif");
        endTimeComboBox.addValueChangeListener(e -> validateTimes());
        endTimeComboBox.addCustomValueSetListener(e -> endTimeComboBox.setValue(e.getDetail()));
        
        deleteButton.setWidthFull();
        deleteButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "white")
            .set("background-color", "#9b0000ff");
        deleteButton.addClickListener(e -> handleDelete());
        
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("gap", "12px");
        
        saveButton.setWidth("calc(50% - 6px)");
        saveButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif");
        saveButton.addClickListener(e -> handleSave());
        
        cancelButton.setWidth("calc(50% - 6px)");
        cancelButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#666666");
        cancelButton.addClickListener(e -> onSuccess.run());
        
        buttonLayout.add(saveButton, cancelButton);
        
        add(title, shiftDatePicker, workerComboBox, workstationComboBox,
            startTimeComboBox, endTimeComboBox, deleteButton, buttonLayout);
    }
    
    /**
     * Loads shift data from the database by ID and populates the form.
     * 
     * @param shiftId the ID of the shift to load
     */
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
    
    /**
     * Populates the form fields with data from an existing shift.
     * 
     * @param shift the shift whose data should populate the form
     */
    private void populateFormWithShift(Shift shift) {
        Date shiftDate = shift.getDate();
        shiftDatePicker.setValue(LocalDate.of(
            shiftDate.get_year(),
            shiftDate.get_month(),
            shiftDate.get_day()
        ));
        
        workerComboBox.setValue(shift.getStudentWorker());
        workstationComboBox.setValue(shift.getWorkstation());
        
        Time shiftTime = shift.getTime();
        startTimeComboBox.setValue(formatTimeForDisplay(shiftTime.getStart_time()));
        endTimeComboBox.setValue(formatTimeForDisplay(shiftTime.getEnd_time()));
    }
    
    /**
     * Sets date picker constraints based on the latest published or unpublished schedule.
     * Restricts selectable dates to the schedule's start and end dates.
     */
    private void setDatePickerConstraints() {
        try {
            var scheduleOpt = useUnpublishedSchedule 
                ? scheduleService.getLatestUnpublishedSchedule()
                : scheduleService.getLatestPublishedSchedule();
            
            if (scheduleOpt.isEmpty()) {
                shiftDatePicker.setMin(LocalDate.now().plusYears(100));
                shiftDatePicker.setMax(LocalDate.now().plusYears(100));
                String scheduleType = useUnpublishedSchedule ? "unpublished" : "published";
                shiftDatePicker.setHelperText("No " + scheduleType + " schedule found. Please create a schedule first.");
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
                shiftDatePicker.setMin(LocalDate.now());
                shiftDatePicker.setMax(LocalDate.now().plusDays(30));
                shiftDatePicker.setHelperText("Schedule dates not properly set.");
            }
        } catch (Exception e) {
            shiftDatePicker.setMin(LocalDate.now());
            shiftDatePicker.setMax(LocalDate.now().plusDays(30));
            shiftDatePicker.setHelperText("Error loading schedule dates.");
        }
    }
    
    /**
     * Handles the delete button click to remove the current shift.
     * Shows confirmation and deletes the shift if user confirms.
     */
    private void handleDelete() {
        if (currentShift == null) {
            Notification.show("No shift to delete", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        try {
            shiftService.deleteShift(currentShift);
            Notification.show("Shift deleted successfully!", 3000, Notification.Position.BOTTOM_START);
            onSuccess.run();
        } catch (Exception e) {
            Notification.show("Error deleting shift: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }
    
    /**
     * Handles the save button click to update or create a shift.
     * Validates fields, checks for conflicts, and persists the shift.
     */
    private void handleSave() {
        if (!validateFields()) {
            return;
        }
        
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
            
            Long excludeShiftId = (currentShift != null) ? currentShift.getId() : null;
            if (shiftService.workerDoubleBooked(selectedWorker, shiftDate, shiftTime, excludeShiftId)) {
                Notification.show("Selected worker is already scheduled for another shift at this date and time.",
                    4000, Notification.Position.MIDDLE);
                return;
            }
            
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
            
            if (shiftService.workstationOcupied(workstationComboBox.getValue(), shiftDate, shiftTime, excludeShiftId)) {
                handleWorkstationConflict(shiftDate, shiftTime, selectedWorker, excludeShiftId);
                return;
            }
            
            if (currentShift != null) {
                shiftService.updateShift(currentShift, shiftDate, workerComboBox.getValue(),
                    workstationComboBox.getValue(), shiftTime);
                Notification.show("Shift updated successfully!", 3000, Notification.Position.BOTTOM_START);
            } else {
                shiftService.addShift(shiftDate, workerComboBox.getValue(),
                    workstationComboBox.getValue(), shiftTime);
                Notification.show("Shift created successfully!", 3000, Notification.Position.BOTTOM_START);
            }
            
            onSuccess.run();
            
        } catch (Exception e) {
            Notification.show("Error saving shift: " + e.getMessage(),
                4000, Notification.Position.MIDDLE);
        }
    }
    
    /**
     * Handles workstation conflict by showing appropriate override dialog.
     * Shows admin or senior override dialog based on user permissions.
     * 
     * @param shiftDate the date of the shift being created/edited
     * @param shiftTime the time of the shift being created/edited
     * @param currentWorker the worker for whom the shift is being created/edited
     * @param excludeShiftId ID of the shift being edited (to exclude from conflict checks)
     */
    private void handleWorkstationConflict(Date shiftDate, Time shiftTime, User currentWorker, Long excludeShiftId) {
        Shift conflictingShift = shiftService.getConflictingShift(workstationComboBox.getValue(), shiftDate, shiftTime);
        
        if (conflictingShift != null && (excludeShiftId == null || !conflictingShift.getId().equals(excludeShiftId))) {
            User conflictingWorker = conflictingShift.getStudentWorker();
            
            if (Auth.isAdmin()) {
                showAdminOverrideDialog(shiftDate, shiftTime, currentWorker, conflictingShift, conflictingWorker);
            } else if (shiftService.isSenior(currentWorker, conflictingWorker)) {
                showSeniorOverrideDialog(shiftDate, shiftTime, currentWorker, conflictingShift, conflictingWorker);
            } else {
                Notification.show("Workstation is occupied and you do not have seniority override privileges.",
                    4000, Notification.Position.MIDDLE);
            }
        }
    }
    
    /**
     * Shows admin override dialog for workstation conflicts.
     * Allows admin to reassign conflicting worker or delete their shift if no workstations available.
     * 
     * @param shiftDate the date of the shift being created/edited
     * @param shiftTime the time of the shift being created/edited
     * @param currentWorker the worker for whom the shift is being created/edited
     * @param conflictingShift the existing shift causing the conflict
     * @param conflictingWorker the worker with the conflicting shift
     */
    private void showAdminOverrideDialog(Date shiftDate, Time shiftTime, User currentWorker,
                                        Shift conflictingShift, User conflictingWorker) {
        Long availableWorkstationId = shiftService.workstationAvailable(shiftDate, shiftTime);
        
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Admin Override - Workstation Conflict");
        
        if (availableWorkstationId != null) {
            dialog.setText(String.format(
                "The selected workstation is currently assigned to %s. " +
                "As an admin, you can override this and reassign %s to an available workstation. " +
                "Do you want to proceed?",
                conflictingWorker.getUsername(), conflictingWorker.getUsername()
            ));
        } else {
            dialog.setText(String.format(
                "WARNING: The selected workstation is currently assigned to %s and NO other workstations are available. " +
                "As an admin, you can override this, but %s's shift will be DELETED. " +
                "Do you want to proceed?",
                conflictingWorker.getUsername(), conflictingWorker.getUsername()
            ));
        }
        
        dialog.setCancelable(true);
        dialog.setConfirmText("Override");
        dialog.setCancelText("Cancel");
        
        dialog.addConfirmListener(event -> {
            try {
                if (availableWorkstationId != null) {
                    Workstation newWorkstation = workstationService.findById(availableWorkstationId).orElse(null);
                    if (newWorkstation != null) {
                        shiftService.updateShift(conflictingShift, conflictingShift.getDate(),
                            conflictingShift.getStudentWorker(), newWorkstation, conflictingShift.getTime());
                        Notification.show(String.format("Admin override: %s reassigned to another workstation.",
                            conflictingWorker.getUsername()), 3000, Notification.Position.MIDDLE);
                    }
                } else {
                    shiftService.deleteShift(conflictingShift);
                    Notification.show(String.format("Admin override: %s's shift was deleted (no available workstation for reassignment).",
                        conflictingWorker.getUsername()), 4000, Notification.Position.MIDDLE);
                }
                
                if (currentShift != null) {
                    shiftService.updateShift(currentShift, shiftDate, currentWorker,
                        workstationComboBox.getValue(), shiftTime);
                } else {
                    shiftService.addShift(shiftDate, currentWorker, workstationComboBox.getValue(), shiftTime);
                }
                
                Notification.show("Shift updated successfully!", 3000, Notification.Position.BOTTOM_START);
                onSuccess.run();
            } catch (Exception ex) {
                Notification.show("Error during admin override: " + ex.getMessage(),
                    4000, Notification.Position.MIDDLE);
            }
        });
        
        dialog.open();
    }
    
    /**
     * Shows senior override dialog for workstation conflicts.
     * Allows senior workers to take workstation if another workstation is available for reassignment.
     * 
     * @param shiftDate the date of the shift being created/edited
     * @param shiftTime the time of the shift being created/edited
     * @param currentWorker the worker for whom the shift is being created/edited
     * @param conflictingShift the existing shift causing the conflict
     * @param conflictingWorker the worker with the conflicting shift
     */
    private void showSeniorOverrideDialog(Date shiftDate, Time shiftTime, User currentWorker,
                                         Shift conflictingShift, User conflictingWorker) {
        Long availableWorkstationId = shiftService.workstationAvailable(shiftDate, shiftTime);
        
        if (availableWorkstationId != null) {
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
                    Workstation newWorkstation = workstationService.findById(availableWorkstationId).orElse(null);
                    if (newWorkstation != null) {
                        shiftService.updateShift(conflictingShift, conflictingShift.getDate(),
                            conflictingShift.getStudentWorker(), newWorkstation, conflictingShift.getTime());
                        
                        if (currentShift != null) {
                            shiftService.updateShift(currentShift, shiftDate, currentWorker,
                                workstationComboBox.getValue(), shiftTime);
                        } else {
                            shiftService.addShift(shiftDate, currentWorker,
                                workstationComboBox.getValue(), shiftTime);
                        }
                        
                        Notification.show("Shift updated successfully! Previous worker reassigned.",
                            3000, Notification.Position.BOTTOM_START);
                        onSuccess.run();
                    }
                } catch (Exception ex) {
                    Notification.show("Error during override: " + ex.getMessage(),
                        4000, Notification.Position.MIDDLE);
                }
            });
            
            dialog.open();
        } else {
            Notification.show("Cannot override: No other workstation available for reassignment.",
                4000, Notification.Position.MIDDLE);
        }
    }
    
    /**
     * Validates that all required fields have values selected.
     * Shows notifications for missing fields and validates time consistency.
     * 
     * @return true if all fields are valid, false otherwise
     */
    private boolean validateFields() {
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
    
    /**
     * Validates that start and end times are logically consistent.
     * Checks that start is before end and both are within workstation operating hours.
     * 
     * @return true if times are valid, false otherwise
     */
    private boolean validateTimes() {
        if (startTimeComboBox.getValue() == null || endTimeComboBox.getValue() == null) {
            return true;
        }
        
        Workstation selectedWorkstation = workstationComboBox.getValue();
        if (selectedWorkstation == null) {
            return true;
        }
        
        int startTime = parseTimeFromString(startTimeComboBox.getValue());
        int endTime = parseTimeFromString(endTimeComboBox.getValue());
        
        if (startTime >= endTime) {
            Notification.show("Start time must be before end time", 3000, Notification.Position.MIDDLE);
            return false;
        }
        
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
    
    /**
     * Generates a list of time options for the time selection dropdowns.
     * Creates 30-minute intervals between opening and closing times.
     * 
     * @return list of time strings in HH:mm format
     */
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
    
    /**
     * Generates time options specific to a workstation's operating hours.
     * Creates 30-minute intervals between the workstation's start and end times.
     * 
     * @param startTime the workstation's opening time
     * @param endTime the workstation's closing time
     * @return list of time strings in HH:mm format
     */
    private List<String> generateTimeOptionsForWorkstation(int startTime, int endTime) {
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
    
    /**
     * Updates time dropdown options when workstation selection changes.
     * Adjusts available times to match the new workstation's operating hours.
     * 
     * @param oldWorkstation the previously selected workstation (unused but required by listener)
     * @param newWorkstation the newly selected workstation
     */
    private void updateTimeOptionsIfNeeded(Workstation oldWorkstation, Workstation newWorkstation) {
        String currentStartTime = startTimeComboBox.getValue();
        String currentEndTime = endTimeComboBox.getValue();
        
        if (newWorkstation != null && newWorkstation.getOperation_hours() != null) {
            Time operatingHours = newWorkstation.getOperation_hours();
            List<String> workstationTimeOptions = generateTimeOptionsForWorkstation(
                operatingHours.getStart_time(), operatingHours.getEnd_time()
            );
            
            startTimeComboBox.setItems(workstationTimeOptions);
            endTimeComboBox.setItems(workstationTimeOptions);
            
            if (currentStartTime != null && currentEndTime != null) {
                int startTime = parseTimeFromString(currentStartTime);
                int endTime = parseTimeFromString(currentEndTime);
                int workstationStart = operatingHours.getStart_time();
                int workstationEnd = operatingHours.getEnd_time();
                
                if (startTime >= workstationStart && startTime <= workstationEnd) {
                    startTimeComboBox.setValue(currentStartTime);
                } else {
                    startTimeComboBox.setValue(formatTimeForDisplay(workstationStart));
                }
                
                if (endTime >= workstationStart && endTime <= workstationEnd) {
                    endTimeComboBox.setValue(currentEndTime);
                } else {
                    endTimeComboBox.setValue(formatTimeForDisplay(workstationEnd));
                }
            } else {
                startTimeComboBox.setValue(formatTimeForDisplay(operatingHours.getStart_time()));
                endTimeComboBox.setValue(formatTimeForDisplay(operatingHours.getEnd_time()));
            }
        } else {
            List<String> defaultTimeOptions = generateTimeOptions();
            startTimeComboBox.setItems(defaultTimeOptions);
            endTimeComboBox.setItems(defaultTimeOptions);
            
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
        
        try {
            String[] parts = timeStr.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid time format");
            }
            
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                throw new IllegalArgumentException("Invalid time values");
            }
            
            return hours * 100 + minutes;
        } catch (IllegalArgumentException e) {
            Notification.show("Invalid time format. Please use HH:MM format (e.g., 09:30)",
                4000, Notification.Position.MIDDLE);
            return Time.OPENING_TIME;
        }
    }
}
