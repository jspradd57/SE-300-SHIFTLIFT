package se300.shiftlift;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "user_id")
    @SuppressWarnings("unused")
    private Long id;
    
    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "initials")
    private String initials;

    @Column(name = "seniority")
    private int seniority;

    /**
     * Default no-argument constructor required by Hibernate.
     */
    protected User() {
    }

    /**
     * Constructs a User with email and password.
     * Extracts username from email and initializes with default values.
     * 
     * @param email the user's email address
     * @param password the user's password
     * @throws IllegalArgumentException if email or password is null or empty
     */
    public User(String email, String password)
    {
        if(email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Email, password, and name cannot be null or empty");
        }else{
            this.email = email;
            this.password = password;

            String [] emailParts = email.split("@");
            this.username = emailParts[0];
            this.initials = "";
            this.seniority = -1;
        }
    }

    /**
     * Extracts the first letter character from the username (searching backwards).
     * Used for generating unique initials.
     * 
     * @param username the username to extract initial from
     * @return the first letter found or empty string if none found
     */
    protected static String get_first_inital(String username)
    {
        if (username == null || username.isEmpty()) {
            return "";
        }

        for (int i = username.length() - 1; i >= 0; i--) {
            char c = username.charAt(i);
            if (Character.isLetter(c)) {
                return String.valueOf(c);
            }
        }

        return "";
    }

    /**
     * Gets the username.
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the user's database ID.
     * 
     * @return the user ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the user's unique initials.
     * 
     * @return the initials
     */
    public String getInitials() {
        return initials;
    }

    /**
     * Gets the user's email address.
     * 
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the user's password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the user's seniority number.
     * 
     * @return the seniority number
     */
    public int getSeniority() {
        return seniority;
    }

    /**
     * Sets the user's password.
     * 
     * @param password the new password
     * @throws IllegalArgumentException if password is null or empty
     */
    public void setPassword(String password) {
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("password cannot be null or empty");
        this.password = password;
    }

    /**
     * Sets the user's email and updates username.
     * Initials will be updated by service layer to ensure uniqueness.
     * 
     * @param email the new email address
     * @throws IllegalArgumentException if email is null or empty
     */
    public void setEmail(String email) {
        if (email == null || email.isEmpty()) throw new IllegalArgumentException("email cannot be null or empty");
        this.email = email;
        
        String[] emailParts = email.split("@");
        this.username = emailParts[0];
    }

    /**
     * Sets the user's initials.
     * Package-private to allow service layer to set unique initials.
     * 
     * @param initials the unique initials
     */
    void setInitials(String initials) {
        this.initials = initials;
    }

    /**
     * Sets the user's seniority number.
     * 
     * @param seniority the seniority number
     */
    public void setSeniorityNumber(int seniority) {
        this.seniority = seniority;
    }

    

}
