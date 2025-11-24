package se300.shiftlift;

import java.util.ArrayList;
import java.util.List;

public class Day {

    private Date date;
    private List<Shift> shifts;

    /**
     * Constructs a Day with the specified date.
     * Initializes an empty list of shifts.
     * 
     * @param date the date for this day
     */
    public Day(Date date) {
        this.date = date;
        this.shifts = new ArrayList<>();
    }

    /**
     * Gets the date for this day.
     * 
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Gets the list of shifts for this day.
     * 
     * @return the shifts list
     */
    public List<Shift> getShifts() {
        return shifts;
    }

    /**
     * Adds a shift to this day.
     * 
     * @param shift the shift to add
     */
    public void addShift(Shift shift) {
        shifts.add(shift);
    }

    /**
     * Removes a shift from this day.
     * 
     * @param shift the shift to remove
     */
    public void removeShift(Shift shift) {
        shifts.remove(shift);
    }

    /**
     * Finds and removes a shift for a specific worker and time.
     * 
     * @param worker the worker assigned to the shift
     * @param time the time of the shift
     */
    public void findShift(User worker, Time time)
    {
        for (Shift shift : shifts) {
            if (shift.getStudentWorker().equals(worker) &&
                shift.getTime().getStart_time() == time.getStart_time() &&
                shift.getTime().getEnd_time() == time.getEnd_time()) {
                shifts.remove(shift);
                return;
            }
        }
    }

    /**
     * Checks if a workstation is occupied during the specified time.
     * 
     * @param workstation the workstation to check
     * @param checkTime the time range to check
     * @return true if workstation is occupied, false otherwise
     */
    public boolean isWorkstationOccupied(Workstation workstation, Time checkTime) {
        if (workstation == null || checkTime == null) {
            return false;
        }
        
        for (Shift shift : shifts) {
            if (shift.getWorkstation().equals(workstation)) {
                if (timesOverlap(shift.getTime(), checkTime)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if a workstation is occupied during the specified time range.
     * 
     * @param workstation the workstation to check
     * @param startTime the start time
     * @param endTime the end time
     * @return true if workstation is occupied, false otherwise or if time range is invalid
     */
    public boolean isWorkstationOccupied(Workstation workstation, int startTime, int endTime) {
        if (workstation == null) {
            return false;
        }
        
        try {
            Time checkTime = new Time(startTime, endTime);
            return isWorkstationOccupied(workstation, checkTime);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Checks if two time periods overlap.
     * 
     * @param time1 the first time period
     * @param time2 the second time period
     * @return true if times overlap, false otherwise
     */
    private boolean timesOverlap(Time time1, Time time2) {
        int start1 = time1.getStart_time();
        int end1 = time1.getEnd_time();
        int start2 = time2.getStart_time();
        int end2 = time2.getEnd_time();
        
        return start1 < end2 && start2 < end1;
    }

    /**
     * Checks if a person is scheduled during the specified time.
     * 
     * @param worker the worker to check
     * @param checkTime the time range to check
     * @return true if worker is scheduled, false otherwise
     */
    public boolean isPersonScheduled(User worker, Time checkTime) {
        if (worker == null || checkTime == null) {
            return false;
        }
        
        for (Shift shift : shifts) {
            if (shift.getStudentWorker().equals(worker)) {
                if (timesOverlap(shift.getTime(), checkTime)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if a person is scheduled during the specified time range.
     * 
     * @param worker the worker to check
     * @param startTime the start time
     * @param endTime the end time
     * @return true if worker is scheduled, false otherwise or if time range is invalid
     */
    public boolean isPersonScheduled(User worker, int startTime, int endTime) {
        if (worker == null) {
            return false;
        }
        
        try {
            Time checkTime = new Time(startTime, endTime);
            return isPersonScheduled(worker, checkTime);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Gets all shifts for a person during the specified time.
     * 
     * @param worker the worker to check
     * @param checkTime the time range to check
     * @return list of overlapping shifts (empty if none found)
     */
    public List<Shift> getPersonShiftsDuringTime(User worker, Time checkTime) {
        List<Shift> overlappingShifts = new ArrayList<>();
        
        if (worker == null || checkTime == null) {
            return overlappingShifts;
        }
        
        for (Shift shift : shifts) {
            if (shift.getStudentWorker().equals(worker)) {
                if (timesOverlap(shift.getTime(), checkTime)) {
                    overlappingShifts.add(shift);
                }
            }
        }
        
        return overlappingShifts;
    }
    
}
