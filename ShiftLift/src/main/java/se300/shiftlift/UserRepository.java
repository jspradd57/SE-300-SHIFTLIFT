package se300.shiftlift;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository interface for User entity persistence.
 * Extends JpaRepository for built-in CRUD operations.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    /**
     * Returns a slice of all users with pagination.
     * 
     * @param pageable the pagination information
     * @return slice of users
     */
    Slice<User> findAllBy(Pageable pageable);
    
    /**
     * Finds all users with a specific username.
     * 
     * @param username the username to search for
     * @return list of matching users
     */
    List<User> findByUsername(String username);
    
    /**
     * Searches for users by username containing the search term (case-insensitive).
     * 
     * @param username the search term
     * @param pageable the pagination information
     * @return slice of matching users
     */
    Slice<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    
    /**
     * Finds users with matching initials.
     * 
     * @param initials the initials to search for
     * @return list of users with matching initials
     */
    List<User> findByInitials(String initials);
}
