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

    // Default constructor required by JPA
    public Shift() {
    }

    public Shift(Date date, Time time, Workstation workstation, User worker)
    {
        this.id = null; // Ensure ID is null for new entities
        this.assigned_date = date;
        this.assigned_time = time;
        this.assigned_workstation = workstation;
        this.assigned_Worker = worker;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return assigned_date;
    }

    public Time getTime() {
        return assigned_time;
    }

    public User getStudentWorker() {
        return assigned_Worker;
    }

    public Workstation getWorkstation() {
        return assigned_workstation;
    }

    public void changeWorkstation(Workstation newWorkstation) {
        this.assigned_workstation = newWorkstation;
    }

    public void changeTime(Time newTime) {
        this.assigned_time = newTime;
    }

    public void changeDate(Date newDate) {
        this.assigned_date = newDate;
    }

    public void changeStudentWorker(StudentWorker newWorker) {
        this.assigned_Worker = newWorker;
    }

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
