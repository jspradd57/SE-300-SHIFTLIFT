package se300.shiftlift;

import jakarta.persistence.Embeddable;

@Embeddable
public class Time {

    public static final int OPENING_TIME = 730;
    public static final int CLOSING_TIME = 1730;

    private int start_time;
    private int end_time;

    /**
     * Constructs a Time with specified start and end times.
     * Validates that times are within operating hours and start is before end.
     * 
     * @param start_time the start time in 24-hour format (e.g., 800 for 8:00 AM)
     * @param end_time the end time in 24-hour format (e.g., 1700 for 5:00 PM)
     * @throws IllegalArgumentException if times are outside operating hours or invalid
     */
    public Time(int start_time, int end_time) {
        
        if(start_time < OPENING_TIME || end_time > CLOSING_TIME || start_time >= end_time) {
            throw new IllegalArgumentException("Shift times outside operating hours (0800-1700)");

        }else{
            this.start_time = start_time;
            this.end_time = end_time;
        }
    }

    /**
     * Default constructor sets time to full operating hours (8:00 AM - 5:00 PM).
     */
    public Time() {
        this.start_time = OPENING_TIME;
        this.end_time = CLOSING_TIME;
    }

    /**
     * Gets the start time.
     * 
     * @return the start time in 24-hour format
     */
    public int getStart_time() {
        return start_time;
    }

    /**
     * Gets the end time.
     * 
     * @return the end time in 24-hour format
     */
    public int getEnd_time() {
        return end_time;
    }

    /**
     * Sets the start time after validation.
     * 
     * @param start_time the start time in 24-hour format
     * @throws IllegalArgumentException if time is outside operating hours
     */
    public void set_start_time(int start_time) {
        if(start_time_is_valid(start_time)) {
            this.start_time = start_time;
        } else {
            throw new IllegalArgumentException("Start time outside operating hours (0730-1730)");
        }
    }

    /**
     * Sets the end time after validation.
     * 
     * @param end_time the end time in 24-hour format
     * @throws IllegalArgumentException if time is outside operating hours
     */
    public void set_end_time(int end_time) {
        if(end_time_is_valid(end_time)) {
            this.end_time = end_time;
        } else {
            throw new IllegalArgumentException("End time outside operating hours (0730-1730)");
        }
    }

    /**
     * Validates that a start time is within operating hours.
     * 
     * @param start_time the start time to validate
     * @return true if valid, false otherwise
     */
    private boolean start_time_is_valid(int start_time) {
        return start_time >= OPENING_TIME && start_time < CLOSING_TIME;
    }
    
    /**
     * Validates that an end time is within operating hours.
     * 
     * @param end_time the end time to validate
     * @return true if valid, false otherwise
     */
    private boolean end_time_is_valid(int end_time) {
        return end_time <= CLOSING_TIME && end_time > OPENING_TIME;
    }
    
    /**
     * Returns a string representation of the time range in 12-hour format.
     * 
     * @return formatted time range (e.g., "8:00 AM - 5:00 PM")
     */
    @Override
    public String toString() {
        return formatTime(start_time) + " - " + formatTime(end_time);
    }
    
    /**
     * Formats a time value to 12-hour format with AM/PM.
     * 
     * @param time the time value in 24-hour format
     * @return formatted time string
     */
    private String formatTime(int time) {
        int hours = time / 100;
        int minutes = time % 100;
        String period = hours < 12 ? "AM" : "PM";
        
        int displayHours = hours;
        if (hours == 0) {
            displayHours = 12;
        } else if (hours > 12) {
            displayHours = hours - 12;
        }
        
        return String.format("%d:%02d %s", displayHours, minutes, period);
    }
    
    /**
     * Calculates the duration of this shift in hours.
     * 
     * @return the duration in hours (as a double)
     */
    public double getDurationInHours() {
        int startHours = start_time / 100;
        int startMinutes = start_time % 100;
        int endHours = end_time / 100;
        int endMinutes = end_time % 100;
        
        int totalStartMinutes = startHours * 60 + startMinutes;
        int totalEndMinutes = endHours * 60 + endMinutes;
        
        return (totalEndMinutes - totalStartMinutes) / 60.0;
    }
    
}
