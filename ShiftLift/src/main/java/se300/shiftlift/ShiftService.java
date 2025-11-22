package se300.shiftlift;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService 
{
    private ShiftRepositry shiftRepositry;
    private final WorkstationRepository workstationRepository;

    ShiftService(ShiftRepositry shiftRepositry, WorkstationRepository workstationRepository) {
        this.shiftRepositry = shiftRepositry;
        this.workstationRepository = workstationRepository;
    }

    @Transactional
    public void addShift(Date date, User worker, Workstation workstation, Time time)
    {
        try {
            Shift shift = new Shift(date, time, workstation, worker);
            shiftRepositry.saveAndFlush(shift);
        } catch (Exception e) {
            System.out.println("Error adding shift: " + e.getMessage());
        }
 
    }

    //Returns a list of all shifts in the database
    @Transactional(readOnly = true)
    public List<Shift> getAllShifts() {
        return shiftRepositry.findAll();
    }

    @Transactional
    public void updateShift(Shift shift, Date date, User worker, Workstation workstation, Time time) {
        try {
            shift.changeDate(date);
            shift.changeStudentWorker((StudentWorker) worker);
            shift.changeWorkstation(workstation);
            shift.changeTime(time);
            shiftRepositry.saveAndFlush(shift);
        } catch (Exception e) {
            System.out.println("Error updating shift: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteShift(Shift shift) {
        if (shift != null && shift.getId() != null) {
            // First, clean up the schedules_shifts join table
            shiftRepositry.deleteScheduleShiftsJoinTableByShiftId(shift.getId());
            shiftRepositry.flush();
            // Then delete the shift
            shiftRepositry.delete(shift);
            shiftRepositry.flush();
        }
    }

    @Transactional
    public int deleteShiftsByUserId(Long userId) {
        if (userId == null) return 0;
        // First, clean up the schedules_shifts join table
        shiftRepositry.deleteScheduleShiftsJoinTableByUserId(userId);
        shiftRepositry.flush();
        // Then delete the shifts
        int deleted = shiftRepositry.deleteByWorkerIdNative(userId);
        shiftRepositry.flush();
        return deleted;
    }

    @Transactional
    public int deleteShiftsByWorkstationId(Long workstationId) {
        if (workstationId == null) return 0;
        // First, clean up the schedules_shifts join table
        shiftRepositry.deleteScheduleShiftsJoinTableByWorkstationId(workstationId);
        shiftRepositry.flush();
        // Then delete the shifts
        int deleted = shiftRepositry.deleteByWorkstationIdNative(workstationId);
        shiftRepositry.flush();
        return deleted;
    }

    public boolean workstationOcupied(Workstation workstation, Date date, Time time) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            // Compare workstation by ID (for entities) and date by value
            boolean sameWorkstation = shift.getWorkstation().getId() != null && 
                                     workstation.getId() != null &&
                                     shift.getWorkstation().getId().equals(workstation.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorkstation && sameDate && timesOverlap(shift.getTime(), time)) {
                return true; // Workstation is occupied (times overlap)
            }
        }
        return false; // Workstation is available
    }

    public boolean workstationOcupied(Workstation workstation, Date date, Time time, Long excludeShiftId) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            // Skip the shift being edited
            if (excludeShiftId != null && shift.getId() != null && shift.getId().equals(excludeShiftId)) {
                continue;
            }
            
            // Compare workstation by ID (for entities) and date by value
            boolean sameWorkstation = shift.getWorkstation().getId() != null && 
                                     workstation.getId() != null &&
                                     shift.getWorkstation().getId().equals(workstation.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorkstation && sameDate && timesOverlap(shift.getTime(), time)) {
                return true; // Workstation is occupied (times overlap)
            }
        }
        return false; // Workstation is available
    }

    public boolean workerDoubleBooked(User worker, Date date, Time time) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            // Compare worker by ID (for entities) and date by value
            boolean sameWorker = shift.getStudentWorker().getId() != null && 
                                worker.getId() != null &&
                                shift.getStudentWorker().getId().equals(worker.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorker && sameDate && timesOverlap(shift.getTime(), time)) {
                return true; // Worker is double booked (times overlap)
            }
        }
        return false; // Worker is available
    }

    public boolean workerDoubleBooked(User worker, Date date, Time time, Long excludeShiftId) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            // Skip the shift being edited
            if (excludeShiftId != null && shift.getId() != null && shift.getId().equals(excludeShiftId)) {
                continue;
            }
            
            // Compare worker by ID (for entities) and date by value
            boolean sameWorker = shift.getStudentWorker().getId() != null && 
                                worker.getId() != null &&
                                shift.getStudentWorker().getId().equals(worker.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorker && sameDate && timesOverlap(shift.getTime(), time)) {
                return true; // Worker is double booked (times overlap)
            }
        }
        return false; // Worker is available
    }
    

    private boolean timesOverlap(Time t1, Time t2) {
        return t1.getStart_time() < t2.getEnd_time() && t2.getStart_time() < t1.getEnd_time();
    }

    public Long workstationAvailable(Date date, Time time) {
        List<Workstation> workstations = workstationRepository.findAll();
        for (Workstation workstation : workstations) {
            Long id = workstation.getId();
            if (id != null && !workstationOcupied(workstation, date, time)) {
                return id;
            }
        }
        // No available workstation found
        return null;
    }

    public Shift getConflictingShift(Workstation workstation, Date date, Time time) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            boolean sameWorkstation = shift.getWorkstation().getId() != null && 
                                     workstation.getId() != null &&
                                     shift.getWorkstation().getId().equals(workstation.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorkstation && sameDate && timesOverlap(shift.getTime(), time)) {
                return shift;
            }
        }
        return null;
    }

    public boolean isSenior(User user1, User user2) {
        // Both are StudentWorkers: compare seniority numbers (lower = more senior)
        if (user1 instanceof StudentWorker sw1 && user2 instanceof StudentWorker sw2) {
            return sw1.getSeniority() < sw2.getSeniority();
        }
        
        // user1 is Manager, user2 is StudentWorker: Manager is senior
        if (user1 instanceof ManagerUser && user2 instanceof StudentWorker) {
            return true;
        }
        
        // user1 is StudentWorker, user2 is Manager: StudentWorker is not senior to Manager
        if (user1 instanceof StudentWorker && user2 instanceof ManagerUser) {
            return false;
        }
        
        // Both are Managers: neither is more senior (equal rank)
        return false;
    }
    
    /**
     * Calculate the total hours a StudentWorker is scheduled for the work week containing the given date
     * Work week is Friday-Thursday inclusive
     * @param worker the StudentWorker
     * @param date the date to determine which work week
     * @return total hours scheduled in that work week
     */
    public double getWeeklyHours(StudentWorker worker, Date date) {
        if (worker == null || date == null) {
            return 0;
        }
        
        List<Shift> allShifts = getAllShifts();
        double totalHours = 0;
        
        for (Shift shift : allShifts) {
            // Check if the shift belongs to this worker
            boolean sameWorker = shift.getStudentWorker() != null &&
                               worker.getId() != null &&
                               shift.getStudentWorker().getId().equals(worker.getId());
            
            // Check if the shift is in the same work week
            boolean sameWeek = shift.getDate().isSameWorkWeek(date);
            
            if (sameWorker && sameWeek) {
                totalHours += shift.getTime().getDurationInHours();
            }
        }
        
        return totalHours;
    }
    
    /**
     * Calculate the total hours a StudentWorker is scheduled for the work week containing the given date,
     * excluding a specific shift (useful when editing an existing shift)
     * @param worker the StudentWorker
     * @param date the date to determine which work week
     * @param excludeShiftId the shift ID to exclude from calculation
     * @return total hours scheduled in that work week (excluding the specified shift)
     */
    public double getWeeklyHours(StudentWorker worker, Date date, Long excludeShiftId) {
        if (worker == null || date == null) {
            return 0;
        }
        
        List<Shift> allShifts = getAllShifts();
        double totalHours = 0;
        
        for (Shift shift : allShifts) {
            // Skip the shift being excluded
            if (excludeShiftId != null && shift.getId() != null && shift.getId().equals(excludeShiftId)) {
                continue;
            }
            
            // Check if the shift belongs to this worker
            boolean sameWorker = shift.getStudentWorker() != null &&
                               worker.getId() != null &&
                               shift.getStudentWorker().getId().equals(worker.getId());
            
            // Check if the shift is in the same work week
            boolean sameWeek = shift.getDate().isSameWorkWeek(date);
            
            if (sameWorker && sameWeek) {
                totalHours += shift.getTime().getDurationInHours();
            }
        }
        
        return totalHours;
    }
    
    /**
     * Check if adding a shift would cause the StudentWorker to exceed their max hours for the week
     * @param worker the StudentWorker
     * @param date the date of the proposed shift
     * @param time the time of the proposed shift
     * @return true if adding this shift would exceed max hours, false otherwise
     */
    public boolean wouldExceedMaxHours(StudentWorker worker, Date date, Time time) {
        if (worker == null || date == null || time == null) {
            return false;
        }
        
        double currentWeeklyHours = getWeeklyHours(worker, date);
        double shiftDuration = time.getDurationInHours();
        double totalHours = currentWeeklyHours + shiftDuration;
        
        return totalHours > worker.getMax_hours();
    }
    
    /**
     * Check if editing a shift would cause the StudentWorker to exceed their max hours for the week
     * @param worker the StudentWorker
     * @param date the date of the shift
     * @param time the time of the shift
     * @param excludeShiftId the ID of the shift being edited (to exclude from current hours calculation)
     * @return true if this shift would exceed max hours, false otherwise
     */
    public boolean wouldExceedMaxHours(StudentWorker worker, Date date, Time time, Long excludeShiftId) {
        if (worker == null || date == null || time == null) {
            return false;
        }
        
        double currentWeeklyHours = getWeeklyHours(worker, date, excludeShiftId);
        double shiftDuration = time.getDurationInHours();
        double totalHours = currentWeeklyHours + shiftDuration;
        
        return totalHours > worker.getMax_hours();
    }
}
