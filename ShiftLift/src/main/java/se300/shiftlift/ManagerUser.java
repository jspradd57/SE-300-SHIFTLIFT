package se300.shiftlift;

import jakarta.persistence.Entity;

@Entity
public class ManagerUser extends User {

    /**
     * Default no-argument constructor required by JPA.
     */
    public ManagerUser() {
    }
    /**
     * Constructs a ManagerUser with email and password.
     * Sets seniority to 0 as managers do not participate in seniority.
     * 
     * @param email the manager's email address
     * @param password the manager's password
     */
    public ManagerUser(String email, String password) {
        super(email, password);
        setSeniorityNumber(0);
    }

    /**
     * Returns a string representation of this manager user.
     * 
     * @return formatted string with manager details
     */
    @Override
    public String toString() {
        return "ManagerUser{" + "username='" + getUsername() + '\'' + ", email='" + getEmail() + '\'' + '}';
    }
}
