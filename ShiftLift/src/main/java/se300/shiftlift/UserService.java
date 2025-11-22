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
    
    UserService(UserRepository userRepository, ShiftService shiftService) {
        this.userRepository = userRepository;
        this.shiftService = shiftService;
        
    }

    //Creates a new Student Worker object and immediately adds it to a row in the users database
    @Transactional
    public void createStudentWorker(String email, String password, int maxHours) {
        StudentWorker studentWorker = new StudentWorker(email, password);
        studentWorker.setMax_hours(maxHours);
        // student workers get seniority assigned below; no role column required (use instanceof / discriminator)
       
        if(!findByUsername(studentWorker.getUsername()).isEmpty()) {
            
            throw new IllegalArgumentException("Username already exists");
           
        }else{
            // Determine the current maximum seniority among existing student workers
            int maxSeniority = userRepository.findAll().stream()
                    .filter(u -> u instanceof StudentWorker)
                    .mapToInt(User::getSeniority)
                    .filter(s -> s >= 0) // ignore uninitialized values
                    .max()
                    .orElse(0);

            // New user gets lowest seniority number (max + 1)
            studentWorker.setSeniorityNumber(maxSeniority + 1);
            
            // Generate and set unique initials
            String uniqueInitials = generateUniqueInitials(studentWorker.getUsername());
            studentWorker.setInitials(uniqueInitials);

            // Hash password before saving
            studentWorker.setPassword(PasswordUtil.hash(studentWorker.getPassword()));
            userRepository.saveAndFlush(studentWorker);
        }
        
    }

    //Creates a new Manager (admin) user and saves to database
    @Transactional
    public void createManagerUser(String email, String password) {
        ManagerUser manager = new ManagerUser(email, password);

        if(!findByUsername(manager.getUsername()).isEmpty()) {
            throw new IllegalArgumentException("Username already exists");
        } else {
            // Managers do not participate in seniority numbering
            
            // Generate and set unique initials
            String uniqueInitials = generateUniqueInitials(manager.getUsername());
            manager.setInitials(uniqueInitials);

            // Hash password before saving
            manager.setPassword(PasswordUtil.hash(manager.getPassword()));
            userRepository.saveAndFlush(manager);
        }
    }

    

        

    //Returns a list of all users in the users database
    @Transactional(readOnly = true)
    public List<User> list(Pageable pageable) {
        return userRepository.findAllBy(pageable).toList();
    }

    //Returns a list of all students with a certain username
    //Does this have to return a list? I'll test
    @Transactional(readOnly = true)
    public List<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Slice<User> searchByUsername(String username, org.springframework.data.domain.Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(username, pageable);
    }

    @Transactional
    public User save(User user) {
        // Ensure password is hashed if not already
        if (user.getPassword() != null && !PasswordUtil.isBcryptHash(user.getPassword())) {
            user.setPassword(PasswordUtil.hash(user.getPassword()));
        }
        return userRepository.saveAndFlush(user);
    }

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
        
        // Count shifts before deleting
        List<Shift> allShifts = shiftService.getAllShifts();
        int deletedShiftsCount = (int) allShifts.stream()
            .filter(shift -> shift.getStudentWorker() != null && 
                           shift.getStudentWorker().getId() != null && 
                           shift.getStudentWorker().getId().equals(user.getId()))
            .count();
        
        boolean wasStudentWorker = user instanceof StudentWorker;
        
        // Clear the persistence context first to avoid any cached entities
        entityManager.clear();
        
        // Use the batch delete query to delete all shifts for this user
        // This uses JPQL and doesn't load entities into memory
        shiftService.deleteShiftsByUserId(user.getId());
        
        // Delete the user
        userRepository.deleteById(user.getId());
        
        // Recalculate seniority in a separate method/transaction if needed
        if (wasStudentWorker) {
            recalculateSeniority();
        }
        
        return deletedShiftsCount;
    }
    
    @Transactional
    public void recalculateSeniority() {
        // Fetch all users from database
        List<User> allUsers = userRepository.findAll();

        // Extract StudentWorker instances and sort them by current seniority (ascending)
        List<StudentWorker> students = allUsers.stream()
                .filter(u -> u instanceof StudentWorker)
                .map(u -> (StudentWorker) u)
                .sorted((a, b) -> Integer.compare(a.getSeniority(), b.getSeniority()))
                .toList();

        // Reassign seniority sequentially starting at 1
        int seniority = 1;
        for (StudentWorker sw : students) {
            sw.setSeniorityNumber(seniority);
            userRepository.save(sw);
            seniority++;
        }
    }

    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    // Generate unique initials for a new user
    private String generateUniqueInitials(String username) {
        String baseInitials = (User.get_first_inital(username) + username.charAt(0)).toUpperCase();
        List<User> usersWithSameInitials = userRepository.findByInitials(baseInitials);
        
        if (usersWithSameInitials.isEmpty()) {
            return baseInitials;
        }
        
        // Find the highest number suffix used
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
