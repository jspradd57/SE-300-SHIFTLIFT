package se300.shiftlift;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "schedules")
public class Schedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "day", column = @Column(name = "start_day")),
        @AttributeOverride(name = "month", column = @Column(name = "start_month")),
        @AttributeOverride(name = "year", column = @Column(name = "start_year")),
        @AttributeOverride(name = "open", column = @Column(name = "start_open"))
    })
    private Date schedule_start_date;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "day", column = @Column(name = "end_day")),
        @AttributeOverride(name = "month", column = @Column(name = "end_month")),
        @AttributeOverride(name = "year", column = @Column(name = "end_year")),
        @AttributeOverride(name = "open", column = @Column(name = "end_open"))
    })
    private Date schedule_end_date;
    
    @Column(name = "is_approved")
    private Boolean is_approved;
    
    private transient List<Shift> shifts;
    
    private transient List<Week> weeks;

    /**
     * Default no-argument constructor required by JPA.
     * Initializes weeks list.
     */
    public Schedule() {
        this.weeks = new ArrayList<>();
    }

    /**
     * Constructs a Schedule with start and end dates.
     * Initializes the schedule as not approved.
     * 
     * @param start the schedule start date
     * @param end the schedule end date
     */
    public Schedule(Date start, Date end)
    {
        this.shifts = new ArrayList<>();
        this.schedule_start_date = start;
        this.schedule_end_date = end;
        this.is_approved = false;
        this.weeks = new ArrayList<>();
    }

    /**
     * Gets the schedule's database ID.
     * 
     * @return the schedule ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the schedule's database ID.
     * 
     * @param id the schedule ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the schedule start date.
     * 
     * @return the start date
     */
    public Date getStartDate() {
        return schedule_start_date;
    }

    /**
     * Gets the schedule end date.
     * 
     * @return the end date
     */
    public Date getEndDate() {
        return schedule_end_date;
    }

    /**
     * Sets the approval status of the schedule.
     * 
     * @param approved true if schedule is approved for publication, false otherwise
     */
    public void setApproved(Boolean approved)
    {
        this.is_approved = approved;
    }

    /**
     * Gets the approval status of the schedule.
     * 
     * @return true if schedule is approved, false otherwise
     */
    public Boolean getApproved()
    {
        return this.is_approved;
    }

    /**
     * Generates week subdivisions (Friday-Thursday) spanning from schedule start to end date.
     * Each week runs from Friday through the following Thursday.
     */
    public void generateWeeks() {
        weeks.clear();
        
        LocalDate startLocal = LocalDate.of(
            schedule_start_date.get_year(),
            schedule_start_date.get_month(),
            schedule_start_date.get_day()
        );
        
        LocalDate endLocal = LocalDate.of(
            schedule_end_date.get_year(),
            schedule_end_date.get_month(),
            schedule_end_date.get_day()
        );
        
        LocalDate currentFriday = startLocal.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        
        if (currentFriday.isBefore(startLocal)) {
            currentFriday = startLocal;
        }
        
        while (!currentFriday.isAfter(endLocal)) {
            LocalDate currentThursday = currentFriday.plusDays(6);
            
            if (currentThursday.isAfter(endLocal)) {
                currentThursday = endLocal;
            }
            
            Date weekStart = new Date(
                currentFriday.getDayOfMonth(),
                currentFriday.getMonthValue(),
                currentFriday.getYear()
            );
            
            Date weekEnd = new Date(
                currentThursday.getDayOfMonth(),
                currentThursday.getMonthValue(),
                currentThursday.getYear()
            );
            
            weeks.add(new Week(weekStart, weekEnd));
            
            currentFriday = currentFriday.plusWeeks(1);
        }
    }

    /**
     * Organizes loaded shifts into their respective weeks.
     * Clears existing shift assignments and redistributes all shifts.
     */
    public void organizeShiftsIntoWeeks() {
        if (weeks.isEmpty()) {
            generateWeeks();
        }
        
        for (Week week : weeks) {
            week.getShifts().clear();
        }
        
        for (Shift shift : shifts) {
            for (Week week : weeks) {
                if (week.isShiftInWeek(shift)) {
                    week.addShift(shift);
                    break;
                }
            }
        }
    }

    /**
     * Loads shifts from database that fall within the schedule's date range.
     * Clears existing shifts before loading and organizes them into weeks.
     * 
     * @param shiftService the service to retrieve shifts from database
     */
    public void loadShifts(ShiftService shiftService)
    {
        shifts.clear();
        
        List<Shift> allShifts = shiftService.getAllShifts();
        
        int startDate = schedule_start_date.get_Date();
        int endDate = schedule_end_date.get_Date();
        
        for (Shift shift : allShifts) {
            int shiftDate = shift.getDate().get_Date();
            if (shiftDate >= startDate && shiftDate <= endDate) {
                shifts.add(shift);
            }
        }
        
        organizeShiftsIntoWeeks();
    }

    /**
     * Returns the shifts list managed by JPA.
     * Initializes the list if it's null.
     * 
     * @return the list of shifts
     */
    public List<Shift> getShifts() {
        if (shifts == null) {
            shifts = new ArrayList<>();
        }
        return shifts;
    }

    /**
     * Returns a copy of the shifts list for read-only viewing.
     * Prevents external modification of the internal shifts list.
     * 
     * @return a new ArrayList containing all shifts
     */
    public List<Shift> getShiftsCopy() {
        return new ArrayList<>(getShifts());
    }

    /**
     * Returns the list of week subdivisions (Friday-Thursday).
     * Initializes the list if it's null.
     * 
     * @return the list of weeks
     */
    public List<Week> getWeeks() {
        if (weeks == null) {
            weeks = new ArrayList<>();
        }
        return weeks;
    }

    /**
     * Returns a specific week by index.
     * 
     * @param index the zero-based index of the week
     * @return the week at the specified index, or null if index is out of bounds
     */
    public Week getWeek(int index) {
        if (weeks == null || index < 0 || index >= weeks.size()) {
            return null;
        }
        return weeks.get(index);
    }
}
