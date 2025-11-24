package se300.shiftlift;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Long id;

    @Embedded
    private Date assigned_date;
    
    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = true, 
                foreignKey = @ForeignKey(name = "fk_shift_worker"))
    private User assigned_Worker;
    
    @Embedded
    private Time assigned_time;
    
    @ManyToOne
    @JoinColumn(name = "workstation_id", nullable = true,
                foreignKey = @ForeignKey(name = "fk_shift_workstation"))
    private Workstation assigned_workstation;

    /**
     * Default no-argument constructor required by JPA.
     */
    public Shift() {
    }

    /**
     * Constructs a Shift with specified date, time, workstation, and worker.
     * 
     * @param date the shift date
     * @param time the shift time range
     * @param workstation the assigned workstation
     * @param worker the assigned worker
     */
    public Shift(Date date, Time time, Workstation workstation, User worker)
    {
        this.id = null;
        this.assigned_date = date;
        this.assigned_time = time;
        this.assigned_workstation = workstation;
        this.assigned_Worker = worker;
    }

    /**
     * Gets the shift's database ID.
     * 
     * @return the shift ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the shift's database ID.
     * 
     * @param id the shift ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the shift date.
     * 
     * @return the shift date
     */
    public Date getDate() {
        return assigned_date;
    }

    /**
     * Gets the shift time range.
     * 
     * @return the shift time
     */
    public Time getTime() {
        return assigned_time;
    }

    /**
     * Gets the assigned worker.
     * 
     * @return the assigned worker
     */
    public User getStudentWorker() {
        return assigned_Worker;
    }

    /**
     * Gets the assigned workstation.
     * 
     * @return the assigned workstation
     */
    public Workstation getWorkstation() {
        return assigned_workstation;
    }

    /**
     * Changes the assigned workstation.
     * 
     * @param newWorkstation the new workstation
     */
    public void changeWorkstation(Workstation newWorkstation) {
        this.assigned_workstation = newWorkstation;
    }

    /**
     * Changes the shift time.
     * 
     * @param newTime the new time range
     */
    public void changeTime(Time newTime) {
        this.assigned_time = newTime;
    }

    /**
     * Changes the shift date.
     * 
     * @param newDate the new date
     */
    public void changeDate(Date newDate) {
        this.assigned_date = newDate;
    }

    /**
     * Changes the assigned worker.
     * 
     * @param newWorker the new student worker
     */
    public void changeStudentWorker(StudentWorker newWorker) {
        this.assigned_Worker = newWorker;
    }

    /**
     * Returns a string representation of this shift.
     * 
     * @return formatted string with shift details
     */
    @Override
    public String toString() {
        return "Shift{" +
                "assigned_date=" + assigned_date +
                ", assigned_Worker=" + assigned_Worker +
                ", assigned_time=" + assigned_time +
                ", assigned_workstation=" + assigned_workstation +
                '}';
    }

}
