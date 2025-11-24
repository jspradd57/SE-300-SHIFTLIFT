package se300.shiftlift;

import java.util.ArrayList;
import java.util.List;



public class Week {

    private Date week_start_date;
    private Date week_end_date;
    private List<Day> week;
    private List<Shift> shifts;

    /**
     * Constructs a Week with start and end dates.
     * 
     * @param start_date the week start date (typically Friday)
     * @param end_date the week end date (typically Thursday)
     * @throws IllegalArgumentException if dates are null or start is after end
     */
    public Week(Date start_date, Date end_date) {
        if (start_date == null || end_date == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (start_date.get_Date() > end_date.get_Date()) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        this.week_start_date = start_date;
        this.week_end_date = end_date;
        this.shifts = new ArrayList<>();
    }
    
    /**
     * Gets the week start date.
     * 
     * @return the start date
     */
    public Date getWeekStartDate() {
        return week_start_date;
    }
    
    /**
     * Gets the week end date.
     * 
     * @return the end date
     */
    public Date getWeekEndDate() {
        return week_end_date;
    }

    /**
     * Gets the list of days in this week.
     * 
     * @return the list of days
     */
    public List<Day> getWeek() {
        return week;
    }

    /**
     * Gets the list of shifts in this week.
     * 
     * @return the shifts list
     */
    public List<Shift> getShifts() {
        return shifts;
    }

    /**
     * Adds a shift to this week if it falls within the week's date range.
     * 
     * @param shift the shift to add
     */
    public void addShift(Shift shift) {
        if (shift != null && isShiftInWeek(shift)) {
            shifts.add(shift);
        }
    }

    /**
     * Checks if a shift falls within this week's date range.
     * 
     * @param shift the shift to check
     * @return true if shift is in this week, false otherwise
     */
    public boolean isShiftInWeek(Shift shift) {
        if (shift == null || shift.getDate() == null) {
            return false;
        }
        int shiftDate = shift.getDate().get_Date();
        int startDate = week_start_date.get_Date();
        int endDate = week_end_date.get_Date();
        return shiftDate >= startDate && shiftDate <= endDate;
    }

    /**
     * Returns a formatted string representation of the week range.
     * 
     * @return formatted date range string
     */
    public String getWeekRangeString() {
        return week_start_date.toString() + " - " + week_end_date.toString();
    }

    /**
     * Returns a string representation of this week.
     * 
     * @return formatted string with week details
     */
    @Override
    public String toString() {
        return "Week{" +
                "start=" + week_start_date +
                ", end=" + week_end_date +
                ", shifts=" + (shifts != null ? shifts.size() : 0) +
                '}';
    }
}
