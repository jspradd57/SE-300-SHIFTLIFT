package se300.shiftlift;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService 
{
    private ShiftRepositry shiftRepositry;
    private final WorkstationRepository workstationRepository;

    /**
     * Constructs a ShiftService with required dependencies.
     * 
     * @param shiftRepositry the repository for shift persistence
     * @param workstationRepository the repository for workstation persistence
     */
    ShiftService(ShiftRepositry shiftRepositry, WorkstationRepository workstationRepository) {
        this.shiftRepositry = shiftRepositry;
        this.workstationRepository = workstationRepository;
    }

    /**
     * Adds a new shift to the database.
     * 
     * @param date the shift date
     * @param worker the student worker assigned to the shift
     * @param workstation the workstation for the shift
     * @param time the time range for the shift
     */
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

    /**
     * Returns a list of all shifts in the database.
     * 
     * @return list of all shifts
     */
    @Transactional(readOnly = true)
    public List<Shift> getAllShifts() {
        return shiftRepositry.findAll();
    }

    /**
     * Updates an existing shift with new values.
     * 
     * @param shift the shift to update
     * @param date the new date
     * @param worker the new student worker
     * @param workstation the new workstation
     * @param time the new time range
     */
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

    /**
     * Deletes a shift from the database.
     * Cleans up join table references before deletion.
     * 
     * @param shift the shift to delete
     */
    @Transactional
    public void deleteShift(Shift shift) {
        if (shift != null && shift.getId() != null) {
            shiftRepositry.deleteScheduleShiftsJoinTableByShiftId(shift.getId());
            shiftRepositry.flush();
            shiftRepositry.delete(shift);
            shiftRepositry.flush();
        }
    }

    /**
     * Deletes all shifts for a specific user.
     * Cleans up join table references before deletion.
     * 
     * @param userId the ID of the user
     * @return number of shifts deleted
     */
    @Transactional
    public int deleteShiftsByUserId(Long userId) {
        if (userId == null) return 0;
        shiftRepositry.deleteScheduleShiftsJoinTableByUserId(userId);
        shiftRepositry.flush();
        int deleted = shiftRepositry.deleteByWorkerIdNative(userId);
        shiftRepositry.flush();
        return deleted;
    }

    /**
     * Deletes all shifts for a specific workstation.
     * Cleans up join table references before deletion.
     * 
     * @param workstationId the ID of the workstation
     * @return number of shifts deleted
     */
    @Transactional
    public int deleteShiftsByWorkstationId(Long workstationId) {
        if (workstationId == null) return 0;
        shiftRepositry.deleteScheduleShiftsJoinTableByWorkstationId(workstationId);
        shiftRepositry.flush();
        int deleted = shiftRepositry.deleteByWorkstationIdNative(workstationId);
        shiftRepositry.flush();
        return deleted;
    }

    /**
     * Checks if a workstation is occupied at the specified date and time.
     * 
     * @param workstation the workstation to check
     * @param date the date to check
     * @param time the time range to check
     * @return true if workstation is occupied, false otherwise
     */
    public boolean workstationOcupied(Workstation workstation, Date date, Time time) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            boolean sameWorkstation = shift.getWorkstation().getId() != null && 
                                     workstation.getId() != null &&
                                     shift.getWorkstation().getId().equals(workstation.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorkstation && sameDate && timesOverlap(shift.getTime(), time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a workstation is occupied at the specified date and time,
     * excluding a specific shift from the check.
     * 
     * @param workstation the workstation to check
     * @param date the date to check
     * @param time the time range to check
     * @param excludeShiftId the shift ID to exclude from the check
     * @return true if workstation is occupied, false otherwise
     */
    public boolean workstationOcupied(Workstation workstation, Date date, Time time, Long excludeShiftId) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            if (excludeShiftId != null && shift.getId() != null && shift.getId().equals(excludeShiftId)) {
                continue;
            }
            
            boolean sameWorkstation = shift.getWorkstation().getId() != null && 
                                     workstation.getId() != null &&
                                     shift.getWorkstation().getId().equals(workstation.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorkstation && sameDate && timesOverlap(shift.getTime(), time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a worker is double booked at the specified date and time.
     * 
     * @param worker the worker to check
     * @param date the date to check
     * @param time the time range to check
     * @return true if worker is double booked, false otherwise
     */
    public boolean workerDoubleBooked(User worker, Date date, Time time) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            boolean sameWorker = shift.getStudentWorker().getId() != null && 
                                worker.getId() != null &&
                                shift.getStudentWorker().getId().equals(worker.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorker && sameDate && timesOverlap(shift.getTime(), time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a worker is double booked at the specified date and time,
     * excluding a specific shift from the check.
     * 
     * @param worker the worker to check
     * @param date the date to check
     * @param time the time range to check
     * @param excludeShiftId the shift ID to exclude from the check
     * @return true if worker is double booked, false otherwise
     */
    public boolean workerDoubleBooked(User worker, Date date, Time time, Long excludeShiftId) {
        List<Shift> allShifts = getAllShifts();
        for (Shift shift : allShifts) {
            if (excludeShiftId != null && shift.getId() != null && shift.getId().equals(excludeShiftId)) {
                continue;
            }
            
            boolean sameWorker = shift.getStudentWorker().getId() != null && 
                                worker.getId() != null &&
                                shift.getStudentWorker().getId().equals(worker.getId());
            
            boolean sameDate = shift.getDate().get_Date() == date.get_Date();
            
            if (sameWorker && sameDate && timesOverlap(shift.getTime(), time)) {
                return true;
            }
        }
        return false;
    }
    

    /**
     * Checks if two time ranges overlap.
     * 
     * @param t1 the first time range
     * @param t2 the second time range
     * @return true if times overlap, false otherwise
     */
    private boolean timesOverlap(Time t1, Time t2) {
        return t1.getStart_time() < t2.getEnd_time() && t2.getStart_time() < t1.getEnd_time();
    }

    /**
     * Finds an available workstation at the specified date and time.
     * 
     * @param date the date to check
     * @param time the time range to check
     * @return the ID of an available workstation, or null if none available
     */
    public Long workstationAvailable(Date date, Time time) {
        List<Workstation> workstations = workstationRepository.findAll();
        for (Workstation workstation : workstations) {
            Long id = workstation.getId();
            if (id != null && !workstationOcupied(workstation, date, time)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Finds a conflicting shift for the specified workstation, date, and time.
     * 
     * @param workstation the workstation to check
     * @param date the date to check
     * @param time the time range to check
     * @return the conflicting shift, or null if none found
     */
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

    /**
     * Determines if user1 is senior to user2.
     * For student workers, compares seniority numbers (lower is more senior).
     * Managers are senior to student workers.
     * 
     * @param user1 the first user
     * @param user2 the second user
     * @return true if user1 is senior to user2, false otherwise
     */
    public boolean isSenior(User user1, User user2) {
        if (user1 instanceof StudentWorker sw1 && user2 instanceof StudentWorker sw2) {
            return sw1.getSeniority() < sw2.getSeniority();
        }
        
        if (user1 instanceof ManagerUser && user2 instanceof StudentWorker) {
            return true;
        }
        
        if (user1 instanceof StudentWorker && user2 instanceof ManagerUser) {
            return false;
        }
        
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
            boolean sameWorker = shift.getStudentWorker() != null &&
                               worker.getId() != null &&
                               shift.getStudentWorker().getId().equals(worker.getId());
            
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
            if (excludeShiftId != null && shift.getId() != null && shift.getId().equals(excludeShiftId)) {
                continue;
            }
            
            boolean sameWorker = shift.getStudentWorker() != null &&
                               worker.getId() != null &&
                               shift.getStudentWorker().getId().equals(worker.getId());
            
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
