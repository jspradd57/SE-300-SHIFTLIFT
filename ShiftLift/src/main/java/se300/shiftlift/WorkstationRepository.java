package se300.shiftlift;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository interface for Workstation entity persistence.
 * Extends JpaRepository for built-in CRUD operations.
 */
public interface WorkstationRepository extends JpaRepository<Workstation, Long>, JpaSpecificationExecutor<Workstation> {
    
    /**
     * Returns a slice of all workstations with pagination.
     * 
     * @param pageable the pagination information
     * @return slice of workstations
     */
    Slice<Workstation> findAllBy(Pageable pageable);
    
    /**
     * Finds all workstations with a specific name.
     * 
     * @param workstation the workstation name to search for
     * @return list of matching workstations
     */
    List<Workstation> findByWorkstation(String workstation);
    
    /**
     * Searches for workstations by name containing the search term (case-insensitive).
     * 
     * @param workstation the search term
     * @param pageable the pagination information
     * @return slice of matching workstations
     */
    Slice<Workstation> findByWorkstationContainingIgnoreCase(String workstation, Pageable pageable);


}
