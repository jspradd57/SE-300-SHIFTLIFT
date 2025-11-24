package se300.shiftlift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for Shift entity persistence.
 * Extends JpaRepository for built-in CRUD operations.
 * Provides custom query methods for bulk deletions and join table cleanup.
 */
public interface ShiftRepositry extends JpaRepository<Shift, Long>, JpaSpecificationExecutor<Shift> 
{
    /**
     * Deletes all shifts for a specific worker.
     * 
     * @param userId the worker ID
     * @return number of shifts deleted
     */
    @Modifying
    @Query("DELETE FROM Shift s WHERE s.assigned_Worker.id = :userId")
    int deleteByWorkerIdNative(@Param("userId") Long userId);
    
    /**
     * Cleans up join table entries for shifts belonging to a specific worker.
     * 
     * @param userId the worker ID
     */
    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE shifts_shift_id IN (SELECT shift_id FROM shifts WHERE worker_id = :userId)", nativeQuery = true)
    void deleteScheduleShiftsJoinTableByUserId(@Param("userId") Long userId);
    
    /**
     * Deletes all shifts for a specific workstation.
     * 
     * @param workstationId the workstation ID
     * @return number of shifts deleted
     */
    @Modifying
    @Query("DELETE FROM Shift s WHERE s.assigned_workstation.id = :workstationId")
    int deleteByWorkstationIdNative(@Param("workstationId") Long workstationId);
    
    /**
     * Cleans up join table entries for shifts at a specific workstation.
     * 
     * @param workstationId the workstation ID
     */
    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE shifts_shift_id IN (SELECT shift_id FROM shifts WHERE workstation_id = :workstationId)", nativeQuery = true)
    void deleteScheduleShiftsJoinTableByWorkstationId(@Param("workstationId") Long workstationId);
    
    /**
     * Cleans up join table entries for a specific shift.
     * 
     * @param shiftId the shift ID
     */
    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE shifts_shift_id = :shiftId", nativeQuery = true)
    void deleteScheduleShiftsJoinTableByShiftId(@Param("shiftId") Long shiftId);
}
