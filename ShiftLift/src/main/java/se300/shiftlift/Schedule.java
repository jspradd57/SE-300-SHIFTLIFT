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
    
    // Non-persistent field to hold shifts loaded from database
    private transient List<Shift> shifts;
    
    // Non-persistent field to hold week subdivisions
    private transient List<Week> weeks;

    // Default constructor required by JPA
    public Schedule() {
        this.weeks = new ArrayList<>();
    }

    public Schedule(Date start, Date end)
    {
        this.shifts = new ArrayList<>();
        this.schedule_start_date = start;
        this.schedule_end_date = end;
        this.is_approved = false;
        this.weeks = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStartDate() {
        return schedule_start_date;
    }

    public Date getEndDate() {
        return schedule_end_date;
    }

    public void setApproved(Boolean approved)
    {
        this.is_approved = approved;
    }

    public Boolean getApproved()
    {
        return this.is_approved;
    }

    //Helper methods

    /**
     * Generates week subdivisions (Friday-Thursday) spanning from schedule start to end date.
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
        
        // Find the first Friday on or before the start date
        LocalDate currentFriday = startLocal.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        
        // If the first Friday is before the schedule start, use the start date instead
        if (currentFriday.isBefore(startLocal)) {
            currentFriday = startLocal;
        }
        
        while (!currentFriday.isAfter(endLocal)) {
            // Calculate Thursday of this week (6 days after Friday)
            LocalDate currentThursday = currentFriday.plusDays(6);
            
            // If Thursday goes beyond schedule end, use the end date instead
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
            
            // Move to next Friday
            currentFriday = currentFriday.plusWeeks(1);
        }
    }

    /**
     * Organizes loaded shifts into their respective weeks.
     */
    public void organizeShiftsIntoWeeks() {
        if (weeks.isEmpty()) {
            generateWeeks();
        }
        
        // Clear existing shifts in weeks
        for (Week week : weeks) {
            week.getShifts().clear();
        }
        
        // Distribute shifts into weeks
        for (Shift shift : shifts) {
            for (Week week : weeks) {
                if (week.isShiftInWeek(shift)) {
                    week.addShift(shift);
                    break; // Shift can only belong to one week
                }
            }
        }
    }

    public void loadShifts(ShiftService shiftService)
    {
        //Load shifts from database 
        //For all shifts in database that fall within schedule_start_date and schedule_end_date
        shifts.clear(); // Clear existing shifts before loading
        
        List<Shift> allShifts = shiftService.getAllShifts();
        
        int startDate = schedule_start_date.get_Date();
        int endDate = schedule_end_date.get_Date();
        
        for (Shift shift : allShifts) {
            int shiftDate = shift.getDate().get_Date();
            if (shiftDate >= startDate && shiftDate <= endDate) {
                shifts.add(shift);
            }
        }
        
        // Organize shifts into weeks after loading
        organizeShiftsIntoWeeks();
    }





    /**
     * Return the shifts list managed by JPA.
     */
    public List<Shift> getShifts() {
        if (shifts == null) {
            shifts = new ArrayList<>();
        }
        return shifts;
    }

    /**
     * Return a copy of the shifts list for read-only viewing.
     */
    public List<Shift> getShiftsCopy() {
        return new ArrayList<>(getShifts());
    }

    /**
     * Return the list of week subdivisions (Friday-Thursday).
     */
    public List<Week> getWeeks() {
        if (weeks == null) {
            weeks = new ArrayList<>();
        }
        return weeks;
    }

    /**
     * Return a specific week by index.
     */
    public Week getWeek(int index) {
        if (weeks == null || index < 0 || index >= weeks.size()) {
            return null;
        }
        return weeks.get(index);
    }
}
