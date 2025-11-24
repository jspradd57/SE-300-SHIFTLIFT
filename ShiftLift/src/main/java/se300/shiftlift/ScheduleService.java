package se300.shiftlift;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepositry shiftRepositry;

    /**
     * Constructs a ScheduleService with required dependencies.
     * 
     * @param scheduleRepository the repository for schedule persistence
     * @param shiftRepositry the repository for shift persistence
     */
    ScheduleService(ScheduleRepository scheduleRepository, ShiftRepositry shiftRepositry) {
        this.scheduleRepository = scheduleRepository;
        this.shiftRepositry = shiftRepositry;
    }

    /**
     * Creates a new schedule with the specified date range.
     * 
     * @param startDate the start date of the schedule
     * @param endDate the end date of the schedule
     * @return the created schedule
     */
    @Transactional
    public Schedule createSchedule(Date startDate, Date endDate) {
        Schedule schedule = new Schedule(startDate, endDate);
        return scheduleRepository.saveAndFlush(schedule);
    }

    /**
     * Returns a list of all schedules in the database.
     * 
     * @return list of all schedules
     */
    @Transactional(readOnly = true)
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    /**
     * Finds a schedule by its ID.
     * 
     * @param id the schedule ID
     * @return Optional containing schedule if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Schedule> getScheduleById(Long id) {
        return scheduleRepository.findById(id);
    }

    /**
     * Saves or updates a schedule in the database.
     * 
     * @param schedule the schedule to save
     * @return the saved schedule
     */
    @Transactional
    public Schedule save(Schedule schedule) {
        return scheduleRepository.saveAndFlush(schedule);
    }

    /**
     * Deletes a schedule from the database.
     * Cleans up join table references before deletion.
     * 
     * @param schedule the schedule to delete
     */
    @Transactional
    public void delete(Schedule schedule) {
        if (schedule == null || schedule.getId() == null) return;
        scheduleRepository.deleteScheduleShiftsJoinTable(schedule.getId());
        scheduleRepository.flush();
        scheduleRepository.delete(schedule);
        scheduleRepository.flush();
    }

    /**
     * Delete a schedule and all its associated shifts.
     * This method ensures that shifts are deleted first to avoid constraint violations.
     * @param schedule the schedule to delete
     * @param shiftService the shift service to use for deleting shifts
     * @return the number of shifts that were deleted
     */
    @Transactional
    public int deleteScheduleWithShifts(Schedule schedule, ShiftService shiftService) {
        if (schedule == null) return 0;
        
        Date startDate = schedule.getStartDate();
        Date endDate = schedule.getEndDate();
        int deletedShiftsCount = 0;
        
        if (startDate != null && endDate != null) {
            List<Shift> allShifts = shiftService.getAllShifts();
            int startDateInt = startDate.get_Date();
            int endDateInt = endDate.get_Date();
            
            List<Shift> shiftsToDelete = allShifts.stream()
                .filter(shift -> {
                    if (shift.getDate() == null) return false;
                    int shiftDateInt = shift.getDate().get_Date();
                    return shiftDateInt >= startDateInt && shiftDateInt <= endDateInt;
                })
                .toList();
            
            deletedShiftsCount = shiftsToDelete.size();
            
            if (schedule.getId() != null) {
                scheduleRepository.deleteScheduleShiftsJoinTable(schedule.getId());
                scheduleRepository.flush();
            }
            
            for (Shift shift : shiftsToDelete) {
                shiftService.deleteShift(shift);
            }
        }
        
        scheduleRepository.delete(schedule);
        scheduleRepository.flush();
        
        return deletedShiftsCount;
    }

    /**
     * Deletes a schedule by its ID.
     * Cleans up join table references before deletion.
     * 
     * @param id the schedule ID
     */
    @Transactional
    public void deleteById(Long id) {
        if (id == null) return;
        scheduleRepository.deleteScheduleShiftsJoinTable(id);
        scheduleRepository.flush();
        scheduleRepository.deleteById(id);
        scheduleRepository.flush();
    }

    /**
     * Returns the total count of schedules in the database.
     * 
     * @return count of schedules
     */
    @Transactional(readOnly = true)
    public long count() {
        return scheduleRepository.count();
    }

    /**
     * Get the latest unpublished (not approved) schedule.
     * Returns the most recently created schedule that has not been approved yet.
     */
    @Transactional(readOnly = true)
    public Optional<Schedule> getLatestUnpublishedSchedule() {
        List<Schedule> allSchedules = scheduleRepository.findAll();
        return allSchedules.stream()
            .filter(s -> s.getApproved() == null || !s.getApproved())
            .max((s1, s2) -> {
                if (s1.getId() == null) return -1;
                if (s2.getId() == null) return 1;
                return s1.getId().compareTo(s2.getId());
            });
    }

    /**
     * Check if an unpublished schedule already exists.
     */
    @Transactional(readOnly = true)
    public boolean hasUnpublishedSchedule() {
        List<Schedule> allSchedules = scheduleRepository.findAll();
        return allSchedules.stream()
            .anyMatch(s -> s.getApproved() == null || !s.getApproved());
    }

    /**
     * Get the latest published (approved) schedule.
     */
    @Transactional(readOnly = true)
    public Optional<Schedule> getLatestPublishedSchedule() {
        List<Schedule> allSchedules = scheduleRepository.findAll();
        return allSchedules.stream()
            .filter(s -> s.getApproved() != null && s.getApproved())
            .max((s1, s2) -> {
                if (s1.getId() == null) return -1;
                if (s2.getId() == null) return 1;
                return s1.getId().compareTo(s2.getId());
            });
    }

    /**
     * Check if a published schedule already exists.
     */
    @Transactional(readOnly = true)
    public boolean hasPublishedSchedule() {
        List<Schedule> allSchedules = scheduleRepository.findAll();
        return allSchedules.stream()
            .anyMatch(s -> s.getApproved() != null && s.getApproved());
    }

    /**
     * Load shifts for a schedule from the database.
     * This method finds all shifts that fall within the schedule's date range.
     */
    @Transactional
    public void loadShiftsForSchedule(Schedule schedule) {
        List<Shift> allShifts = shiftRepositry.findAll();
        
        int startDate = schedule.getStartDate().get_Date();
        int endDate = schedule.getEndDate().get_Date();
        
        schedule.getShifts().clear();
        
        for (Shift shift : allShifts) {
            int shiftDate = shift.getDate().get_Date();
            if (shiftDate >= startDate && shiftDate <= endDate) {
                schedule.getShifts().add(shift);
            }
        }
        
        scheduleRepository.saveAndFlush(schedule);
    }
}
