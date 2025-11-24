package se300.shiftlift;

import jakarta.persistence.Entity;

@Entity
public class StudentWorker extends User{
    private int max_hours = 20;
    private int scheduled_hours;

    /**
     * Default no-argument constructor required by JPA.
     */
    public StudentWorker()
    {
        
    }

    /**
     * Constructs a StudentWorker with email and password.
     * 
     * @param email the student worker's email address
     * @param password the student worker's password
     */
    public StudentWorker(String email, String password) {
        super(email, password);
    }

    /**
     * Gets the currently scheduled hours for this student worker.
     * 
     * @return the scheduled hours
     */
    public int getScheduled_hours() {
        return scheduled_hours;
    }

    /**
     * Sets the scheduled hours for this student worker.
     * 
     * @param hours the scheduled hours (must be between 0 and max_hours)
     * @throws IllegalArgumentException if hours is out of range
     */
    public void setScheduled_hours(int hours) {
        if (hours < 0 || hours > max_hours) throw new IllegalArgumentException("scheduled hours out of range");
        this.scheduled_hours = hours;
    }

    /**
     * Gets the maximum hours per week this student worker can be scheduled.
     * 
     * @return the maximum hours
     */
    public int getMax_hours() {
        return max_hours;
    }

    /**
     * Sets the maximum hours per week this student worker can be scheduled.
     * 
     * @param max_hours the maximum hours
     */
    public void setMax_hours(int max_hours) {
        this.max_hours = max_hours;
    }

    /**
     * Returns a string representation of this student worker.
     * 
     * @return formatted string with student worker details
     */
    @Override
    public String toString() {
        return "StudentWorker{" + "username='" + getUsername()+ '\'' + "initials='" + getInitials() + '\'' + ", email='" + getEmail() + '\'' +  "Passowrd='" + getPassword() + '\'' + ", scheduled_hours=" + scheduled_hours + '}';
    }
    


}
