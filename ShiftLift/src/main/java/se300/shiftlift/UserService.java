package se300.shiftlift;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ShiftService shiftService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Constructs a UserService with required dependencies.
     * 
     * @param userRepository the repository for user persistence
     * @param shiftService the service for managing shifts
     */
    UserService(UserRepository userRepository, ShiftService shiftService) {
        this.userRepository = userRepository;
        this.shiftService = shiftService;
        
    }

    /**
     * Creates a new Student Worker and saves to database.
     * Assigns seniority, generates unique initials, and hashes password.
     * 
     * @param email the student worker's email
     * @param password the student worker's password (will be hashed)
     * @param maxHours the maximum hours per week
     * @throws IllegalArgumentException if username already exists
     */
    @Transactional
    public void createStudentWorker(String email, String password, int maxHours) {
        StudentWorker studentWorker = new StudentWorker(email, password);
        studentWorker.setMax_hours(maxHours);
       
        if(!findByUsername(studentWorker.getUsername()).isEmpty()) {
            
            throw new IllegalArgumentException("Username already exists");
           
        }else{
            int maxSeniority = userRepository.findAll().stream()
                    .filter(u -> u instanceof StudentWorker)
                    .mapToInt(User::getSeniority)
                    .filter(s -> s >= 0)
                    .max()
                    .orElse(0);

            studentWorker.setSeniorityNumber(maxSeniority + 1);
            
            String uniqueInitials = generateUniqueInitials(studentWorker.getUsername());
            studentWorker.setInitials(uniqueInitials);

            studentWorker.setPassword(PasswordUtil.hash(studentWorker.getPassword()));
            userRepository.saveAndFlush(studentWorker);
        }
        
    }

    /**
     * Creates a new Manager (admin) user and saves to database.
     * Managers do not participate in seniority numbering.
     * 
     * @param email the manager's email
     * @param password the manager's password (will be hashed)
     * @throws IllegalArgumentException if username already exists
     */
    @Transactional
    public void createManagerUser(String email, String password) {
        ManagerUser manager = new ManagerUser(email, password);

        if(!findByUsername(manager.getUsername()).isEmpty()) {
            throw new IllegalArgumentException("Username already exists");
        } else {
            String uniqueInitials = generateUniqueInitials(manager.getUsername());
            manager.setInitials(uniqueInitials);

            manager.setPassword(PasswordUtil.hash(manager.getPassword()));
            userRepository.saveAndFlush(manager);
        }
    }

    /**
     * Returns a paginated list of all users in the database.
     * 
     * @param pageable the pagination information
     * @return list of users
     */
    @Transactional(readOnly = true)
    public List<User> list(Pageable pageable) {
        return userRepository.findAllBy(pageable).toList();
    }

    /**
     * Returns a list of all users with a specific username.
     * 
     * @param username the username to search for
     * @return list of matching users
     */
    @Transactional(readOnly = true)
    public List<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Searches for users by username containing the search term (case-insensitive).
     * 
     * @param username the search term
     * @param pageable the pagination information
     * @return slice of matching users
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Slice<User> searchByUsername(String username, org.springframework.data.domain.Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(username, pageable);
    }

    /**
     * Saves or updates a user in the database.
     * Ensures password is hashed if not already.
     * 
     * @param user the user to save
     * @return the saved user
     */
    @Transactional
    public User save(User user) {
        if (user.getPassword() != null && !PasswordUtil.isBcryptHash(user.getPassword())) {
            user.setPassword(PasswordUtil.hash(user.getPassword()));
        }
        return userRepository.saveAndFlush(user);
    }

    /**
     * Changes a user's password after validating the current password.
     * 
     * @param user the user whose password to change
     * @param currentPlain the current password in plain text
     * @param newPlain the new password in plain text
     * @throws IllegalArgumentException if user is null or current password is incorrect
     */
    @Transactional
    public void changePassword(User user, String currentPlain, String newPlain) {
        if (user == null) throw new IllegalArgumentException("No user");
        if (!PasswordUtil.matches(currentPlain, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(PasswordUtil.hash(newPlain));
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public int delete(User user) {
        if (user == null || user.getId() == null) return 0;
        
        List<Shift> allShifts = shiftService.getAllShifts();
        int deletedShiftsCount = (int) allShifts.stream()
            .filter(shift -> shift.getStudentWorker() != null && 
                           shift.getStudentWorker().getId() != null && 
                           shift.getStudentWorker().getId().equals(user.getId()))
            .count();
        
        boolean wasStudentWorker = user instanceof StudentWorker;
        
        entityManager.clear();
        
        shiftService.deleteShiftsByUserId(user.getId());
        
        userRepository.deleteById(user.getId());
        
        if (wasStudentWorker) {
            recalculateSeniority();
        }
        
        return deletedShiftsCount;
    }
    
    /**
     * Recalculates seniority numbers for all student workers.
     * Assigns sequential numbers (1, 2, 3...) in order of current seniority.
     */
    @Transactional
    public void recalculateSeniority() {
        List<User> allUsers = userRepository.findAll();

        List<StudentWorker> students = allUsers.stream()
                .filter(u -> u instanceof StudentWorker)
                .map(u -> (StudentWorker) u)
                .sorted((a, b) -> Integer.compare(a.getSeniority(), b.getSeniority()))
                .toList();

        int seniority = 1;
        for (StudentWorker sw : students) {
            sw.setSeniorityNumber(seniority);
            userRepository.save(sw);
            seniority++;
        }
    }

    /**
     * Returns the total count of users in the database.
     * 
     * @return count of users
     */
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    /**
     * Generates unique initials from a username.
     * Combines first and last initial, adds numeric suffix if duplicate exists.
     * 
     * @param username the username to generate initials from
     * @return unique two-letter initials (possibly with numeric suffix)
     */
    private String generateUniqueInitials(String username) {
        String baseInitials = (User.get_first_inital(username) + username.charAt(0)).toUpperCase();
        List<User> usersWithSameInitials = userRepository.findByInitials(baseInitials);
        
        if (usersWithSameInitials.isEmpty()) {
            return baseInitials;
        }
        
        int maxNumber = usersWithSameInitials.stream()
            .map(User::getInitials)
            .filter(i -> i.startsWith(baseInitials))
            .map(i -> i.substring(baseInitials.length()))
            .filter(suffix -> !suffix.isEmpty())
            .map(suffix -> {
                try {
                    return Integer.parseInt(suffix);
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max(Integer::compareTo)
            .orElse(0);
            
        return baseInitials + (maxNumber + 1);
    }
}
