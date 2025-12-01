package se300.shiftlift;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing a date that has been blocked by a manager.
 * Blocked dates prevent users from creating or editing shifts on that day.
 */
@Entity
@Table(name = "blocked_dates")
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blocked_date_id")
    private Long id;

    @Column(name = "date_value", unique = true, nullable = false)
    private int dateValue; // Stored as YYYYMMDD integer

    @Column(name = "day")
    private int day;

    @Column(name = "month")
    private int month;

    @Column(name = "year")
    private int year;

    /**
     * Default no-argument constructor required by JPA.
     */
    public BlockedDate() {
    }

    /**
     * Constructs a BlockedDate from a Date object.
     * 
     * @param date the date to block
     */
    public BlockedDate(Date date) {
        this.day = date.get_day();
        this.month = date.get_month();
        this.year = date.get_year();
        this.dateValue = date.get_Date();
    }

    /**
     * Constructs a BlockedDate from day, month, and year.
     * 
     * @param day the day of the month
     * @param month the month (1-12)
     * @param year the year
     */
    public BlockedDate(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.dateValue = day + month * 100 + year * 10000;
    }

    /**
     * Gets the database ID.
     * 
     * @return the ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the database ID.
     * 
     * @param id the ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the date value as an integer in YYYYMMDD format.
     * 
     * @return the date value
     */
    public int getDateValue() {
        return dateValue;
    }

    /**
     * Gets the day of the month.
     * 
     * @return the day
     */
    public int getDay() {
        return day;
    }

    /**
     * Gets the month.
     * 
     * @return the month (1-12)
     */
    public int getMonth() {
        return month;
    }

    /**
     * Gets the year.
     * 
     * @return the year
     */
    public int getYear() {
        return year;
    }

    /**
     * Converts this BlockedDate to a Date object.
     * 
     * @return the corresponding Date object
     */
    public Date toDate() {
        return new Date(day, month, year);
    }

    @Override
    public String toString() {
        return String.format("%d/%d/%d", month, day, year);
    }
}
