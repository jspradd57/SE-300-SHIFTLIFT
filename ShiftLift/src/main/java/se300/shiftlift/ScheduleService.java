package se300.shiftlift;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepositry shiftRepositry;

    ScheduleService(ScheduleRepository scheduleRepository, ShiftRepositry shiftRepositry) {
        this.scheduleRepository = scheduleRepository;
        this.shiftRepositry = shiftRepositry;
    }

    @Transactional
    public Schedule createSchedule(Date startDate, Date endDate) {
        Schedule schedule = new Schedule(startDate, endDate);
        return scheduleRepository.saveAndFlush(schedule);
    }

    @Transactional(readOnly = true)
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Schedule> getScheduleById(Long id) {
        return scheduleRepository.findById(id);
    }

    @Transactional
    public Schedule save(Schedule schedule) {
        return scheduleRepository.saveAndFlush(schedule);
    }

    @Transactional
    public void delete(Schedule schedule) {
        if (schedule == null || schedule.getId() == null) return;
        // Clean up the join table first
        scheduleRepository.deleteScheduleShiftsJoinTable(schedule.getId());
        scheduleRepository.flush();
        // Then delete the schedule
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
            
            // Find shifts within the schedule date range
            List<Shift> shiftsToDelete = allShifts.stream()
                .filter(shift -> {
                    if (shift.getDate() == null) return false;
                    int shiftDateInt = shift.getDate().get_Date();
                    return shiftDateInt >= startDateInt && shiftDateInt <= endDateInt;
                })
                .toList();
            
            deletedShiftsCount = shiftsToDelete.size();
            
            // First, clean up the join table entries if schedule ID exists
            if (schedule.getId() != null) {
                scheduleRepository.deleteScheduleShiftsJoinTable(schedule.getId());
                scheduleRepository.flush();
            }
            
            // Then delete each shift
            for (Shift shift : shiftsToDelete) {
                shiftService.deleteShift(shift);
            }
        }
        
        // Finally delete the schedule itself
        scheduleRepository.delete(schedule);
        scheduleRepository.flush();
        
        return deletedShiftsCount;
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) return;
        // Clean up the join table first
        scheduleRepository.deleteScheduleShiftsJoinTable(id);
        scheduleRepository.flush();
        // Then delete the schedule
        scheduleRepository.deleteById(id);
        scheduleRepository.flush();
    }

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
                // Compare by ID (assuming higher ID = more recent)
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
