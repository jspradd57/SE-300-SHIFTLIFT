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
    
    public WorkstationService(WorkstationRepository workstationRepository, ShiftService shiftService) {
        this.workstationRepository = workstationRepository;
        this.shiftService = shiftService;
    }
    
    
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

    //Returns list of all workstations in database
    @Transactional(readOnly = true)
    public List<Workstation> list(Pageable pageable) {
        return workstationRepository.findAll(pageable).toList();
    }

    //Returns a list of all workstations with a certain name
    @Transactional(readOnly = true)
    public List<Workstation> findByName(String name) {
        return workstationRepository.findByWorkstation(name);
    }
    
    //Returns Optional of workstation by name for EditWorkstationView
    @Transactional(readOnly = true)
    public java.util.Optional<Workstation> findByNameOptional(String name) {
        List<Workstation> workstations = workstationRepository.findByWorkstation(name);
        return workstations.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(workstations.get(0));
    }

    //Returns workstation from database matching name search
    @Transactional(readOnly = true)
    public Slice<Workstation> searchByName(String name, Pageable pageable) {
        return workstationRepository.findByWorkstationContainingIgnoreCase(name, pageable);
    }

    //Saves or updates a workstation in the database
    @Transactional
    public Workstation save(Workstation workstation)
    {
        return workstationRepository.saveAndFlush(workstation);
    }

    //Deletes a workstation from the database
    @Transactional
    public int delete(Workstation workstation) {
        if (workstation == null || workstation.getId() == null) return 0;
        
        // Count shifts before deleting
        List<Shift> allShifts = shiftService.getAllShifts();
        int deletedShiftsCount = (int) allShifts.stream()
            .filter(shift -> shift.getWorkstation() != null && 
                           shift.getWorkstation().getId() != null && 
                           shift.getWorkstation().getId().equals(workstation.getId()))
            .count();
        
        // Clear the persistence context first to avoid any cached entities
        entityManager.clear();
        
        // Use the batch delete query to delete all shifts for this workstation
        shiftService.deleteShiftsByWorkstationId(workstation.getId());
        
        // Delete the workstation
        workstationRepository.deleteById(workstation.getId());
        
        return deletedShiftsCount;
    }

    //Return number of workstations in database
    @Transactional(readOnly = true)
    public long count() {
        return workstationRepository.count();
    }

    //Find workstation by ID
    @Transactional(readOnly = true)
    public java.util.Optional<Workstation> findById(Long id) {
        return workstationRepository.findById(id);
    }

   
}