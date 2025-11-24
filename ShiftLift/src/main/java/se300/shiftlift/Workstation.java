package se300.shiftlift;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;


@Entity
@Table(name = "workstations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DescriminatorColumn(name = "dtype")
public class Workstation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "workstation_id")
    private Long id;

    @Column(name = "workstation_name")
    private String workstation;

    @Embedded
    private Time operation_hours;

    /**
     * Default no-argument constructor required by JPA.
     */
    public Workstation() {

    }

    /**
     * Constructs a Workstation with a name.
     * 
     * @param workstation the workstation name
     * @throws IllegalArgumentException if workstation name is null or empty
     */
    public Workstation(String workstation) {
        if(workstation != null && !workstation.isEmpty()) {
            this.workstation = workstation;
        } else {
            throw new IllegalArgumentException("Workstation name cannot be null or empty");
        }
    }

    /**
     * Gets the workstation's database ID.
     * 
     * @return the workstation ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the workstation's database ID.
     * 
     * @param id the workstation ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the workstation name.
     * 
     * @return the workstation name
     */
    public String getName() {
        return workstation;
    }

    /**
     * Sets the workstation name.
     * 
     * @param workstation the workstation name
     * @throws IllegalArgumentException if workstation name is null or empty
     */
    public void setName(String workstation) {
        if (workstation != null && !workstation.isEmpty()) {
            this.workstation = workstation;
        } else {
            throw new IllegalArgumentException("Workstation name cannot be null or empty");
        }
    }

    /**
     * Gets the workstation's operating hours.
     * 
     * @return the operating hours
     */
    public Time getOperation_hours() {
        return operation_hours;
    }

    /**
     * Sets the workstation's opening time.
     * Initializes operation hours with default values if not already set.
     * 
     * @param opening_time the opening time
     */
    public void station_opening(int opening_time) {
        if (this.operation_hours == null) {
            this.operation_hours = new Time();
        }
        this.operation_hours.set_start_time(opening_time);
    }

    /**
     * Sets the workstation's closing time.
     * Initializes operation hours with default values if not already set.
     * 
     * @param closing_time the closing time
     */
    public void station_closing(int closing_time) {
        if (this.operation_hours == null) {
            this.operation_hours = new Time();
        }
        this.operation_hours.set_end_time(closing_time);
    }
    
    /**
     * Sets the workstation's operating hours.
     * 
     * @param operation_hours the operating hours
     */
    public void setOperation_hours(Time operation_hours) {
        this.operation_hours = operation_hours;
    }

    /**
     * Returns a string representation of this workstation.
     * 
     * @return formatted string with workstation name
     */
    @Override
    public String toString() {
    return "Workstation{name='" + workstation + "'}";
    }


}
