package se300.shiftlift;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository interface for BlockedDate entities.
 * Provides database access methods for managing blocked dates.
 */
public interface BlockedDateRepository extends JpaRepository<BlockedDate, Long>, JpaSpecificationExecutor<BlockedDate> {
    
    /**
     * Finds a blocked date by its date value (YYYYMMDD format).
     * 
     * @param dateValue the date value as an integer
     * @return Optional containing the blocked date if found
     */
    Optional<BlockedDate> findByDateValue(int dateValue);
    
    /**
     * Checks if a date value is blocked.
     * 
     * @param dateValue the date value as an integer
     * @return true if the date is blocked, false otherwise
     */
    boolean existsByDateValue(int dateValue);
    
    /**
     * Deletes a blocked date by its date value.
     * 
     * @param dateValue the date value as an integer
     */
    void deleteByDateValue(int dateValue);
}
