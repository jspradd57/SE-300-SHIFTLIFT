package se300.shiftlift;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class WorkstationService {
    
    private final WorkstationRepository workstationRepository;
    private final ShiftService shiftService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Constructs a WorkstationService with required dependencies.
     * 
     * @param workstationRepository the repository for workstation persistence
     * @param shiftService the service for managing shifts
     */
    WorkstationService(WorkstationRepository workstationRepository, ShiftService shiftService) {
        this.workstationRepository = workstationRepository;
        this.shiftService = shiftService;
    }
    
    /**
     * Creates a new workstation and saves to database.
     * 
     * @param name the workstation name
     * @throws IllegalArgumentException if name already exists
     */
    @Transactional
    public void createWorstation(String name)
    {
        Workstation workstation = new Workstation(name);

        if(!findByName(workstation.getName()).isEmpty()){
            throw new IllegalArgumentException("Workstation already exists");
        }else{
            workstationRepository.saveAndFlush(workstation);
        }

    }

    /**
     * Returns a paginated list of all workstations in the database.
     * 
     * @param pageable the pagination information
     * @return list of workstations
     */
    @Transactional(readOnly = true)
    public List<Workstation> list(Pageable pageable) {
        return workstationRepository.findAll(pageable).toList();
    }

    /**
     * Finds all workstations with a specific name.
     * 
     * @param name the workstation name to search for
     * @return list of matching workstations
     */
    @Transactional(readOnly = true)
    public List<Workstation> findByName(String name) {
        return workstationRepository.findByWorkstation(name);
    }
    
    /**
     * Finds a workstation by name, returning an Optional.
     * 
     * @param name the workstation name to search for
     * @return Optional containing workstation if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Workstation> findByNameOptional(String name) {
        List<Workstation> workstations = workstationRepository.findByWorkstation(name);
        return workstations.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(workstations.get(0));
    }

    /**
     * Searches for workstations by name containing the search term (case-insensitive).
     * 
     * @param name the search term
     * @param pageable the pagination information
     * @return slice of matching workstations
     */
    @Transactional(readOnly = true)
    public Slice<Workstation> searchByName(String name, Pageable pageable) {
        return workstationRepository.findByWorkstationContainingIgnoreCase(name, pageable);
    }

    /**
     * Saves or updates a workstation in the database.
     * 
     * @param workstation the workstation to save
     * @return the saved workstation
     */
    @Transactional
    public Workstation save(Workstation workstation)
    {
        return workstationRepository.saveAndFlush(workstation);
    }

    /**
     * Deletes a workstation from the database.
     * Removes all shifts assigned to this workstation.
     * 
     * @param workstation the workstation to delete
     * @return the number of shifts deleted
     */
    @Transactional
    public int delete(Workstation workstation) {
        if (workstation == null || workstation.getId() == null) return 0;
        
        List<Shift> allShifts = shiftService.getAllShifts();
        int deletedShiftsCount = (int) allShifts.stream()
            .filter(shift -> shift.getWorkstation() != null && 
                           shift.getWorkstation().getId() != null && 
                           shift.getWorkstation().getId().equals(workstation.getId()))
            .count();
        
        entityManager.clear();
        
        shiftService.deleteShiftsByWorkstationId(workstation.getId());
        
        workstationRepository.deleteById(workstation.getId());
        
        return deletedShiftsCount;
    }

    /**
     * Returns the total count of workstations in the database.
     * 
     * @return count of workstations
     */
    @Transactional(readOnly = true)
    public long count() {
        return workstationRepository.count();
    }

    /**
     * Finds a workstation by its ID.
     * 
     * @param id the workstation ID
     * @return Optional containing workstation if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Workstation> findById(Long id) {
        return workstationRepository.findById(id);
    }

   
}