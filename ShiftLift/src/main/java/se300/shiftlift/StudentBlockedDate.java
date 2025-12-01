package se300.shiftlift;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entity representing a date that has been blocked by a specific student worker.
 * Student workers cannot be scheduled for shifts on their blocked dates.
 */
@Entity
@Table(name = "student_blocked_dates")
public class StudentBlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_blocked_date_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_student_blocked_date_student"))
    private StudentWorker student;

    @Column(name = "date_value", nullable = false)
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
    public StudentBlockedDate() {
    }

    /**
     * Constructs a StudentBlockedDate from a student and Date object.
     * 
     * @param student the student worker blocking the date
     * @param date the date to block
     */
    public StudentBlockedDate(StudentWorker student, Date date) {
        this.student = student;
        this.day = date.get_day();
        this.month = date.get_month();
        this.year = date.get_year();
        this.dateValue = date.get_Date();
    }

    /**
     * Constructs a StudentBlockedDate from a student and date components.
     * 
     * @param student the student worker blocking the date
     * @param day the day of the month
     * @param month the month (1-12)
     * @param year the year
     */
    public StudentBlockedDate(StudentWorker student, int day, int month, int year) {
        this.student = student;
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
     * Gets the student worker.
     * 
     * @return the student worker
     */
    public StudentWorker getStudent() {
        return student;
    }

    /**
     * Sets the student worker.
     * 
     * @param student the student worker
     */
    public void setStudent(StudentWorker student) {
        this.student = student;
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
     * Converts this StudentBlockedDate to a Date object.
     * 
     * @return the corresponding Date object
     */
    public Date toDate() {
        return new Date(day, month, year);
    }

    @Override
    public String toString() {
        return String.format("%s blocked %d/%d/%d", 
            student != null ? student.getUsername() : "Unknown", 
            month, day, year);
    }
}
