package se300.shiftlift;

import jakarta.persistence.Embeddable;

@Embeddable
public class Date {
    @SuppressWarnings("FieldMayBeFinal")
    private int day;
    @SuppressWarnings("FieldMayBeFinal")
    private int month;
    @SuppressWarnings("FieldMayBeFinal")
    private int year;
    private boolean open;

    /**
     * Default no-argument constructor required by JPA.
     */
    public Date() {
    }

    /**
     * Constructs a Date with the specified day, month, and year.
     * Sets the date as open by default.
     * 
     * @param day the day of the month
     * @param month the month (1-12)
     * @param year the year
     */
    public Date(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.open = true;
    }
    
    /**
     * Gets the day of the month.
     * 
     * @return the day
     */
    public int get_day() {
        return day;
    }
    
    /**
     * Gets the month.
     * 
     * @return the month (1-12)
     */
    public int get_month() {
        return month;
    }
    
    /**
     * Gets the year.
     * 
     * @return the year
     */
    public int get_year() {
        return year;
    }

    /**
     * Gets the date as an integer in YYYYMMDD format.
     * 
     * @return integer representation of the date
     */
    public int get_Date()
    {
        return day + month*100 + year*10000;
    }

    /**
     * Sets the open status of the date.
     * 
     * @param status true if the date is open for scheduling, false otherwise
     */
    public void set_open_status(boolean status)
    {
        this.open = status;
    }

    /**
     * Gets the open status of the date.
     * 
     * @return true if the date is open for scheduling, false otherwise
     */
    public boolean get_open_status()
    {
        return open;
    }

    /**
     * Returns a string representation of the date in MM/DD/YYYY format.
     * 
     * @return formatted date string
     */
    @Override
    public String toString() {
        return month + "/" + day + "/" + year;
    }
    
    /**
     * Converts this Date to a LocalDate for easier date calculations.
     * 
     * @return the corresponding LocalDate
     */
    public java.time.LocalDate toLocalDate() {
        return java.time.LocalDate.of(year, month, day);
    }
    
    /**
     * Gets the Friday that starts the work week containing this date.
     * Work week is defined as Friday-Thursday.
     * 
     * @return the LocalDate of the work week start (Friday)
     */
    public java.time.LocalDate getWorkWeekStart() {
        java.time.LocalDate date = toLocalDate();
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        if (dayOfWeek == java.time.DayOfWeek.FRIDAY) {
            return date;
        } else if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return date.with(java.time.temporal.TemporalAdjusters.previous(java.time.DayOfWeek.FRIDAY));
        } else {
            return date.with(java.time.temporal.TemporalAdjusters.previous(java.time.DayOfWeek.FRIDAY));
        }
    }
    
    /**
     * Checks if this date is in the same work week as another date.
     * Work week is Friday-Thursday.
     * 
     * @param other the other date to compare with
     * @return true if both dates are in the same work week, false otherwise
     */
    public boolean isSameWorkWeek(Date other) {
        return this.getWorkWeekStart().equals(other.getWorkWeekStart());
    }

}
