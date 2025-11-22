package se300.shiftlift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ShiftRepositry extends JpaRepository<Shift, Long>, JpaSpecificationExecutor<Shift> 
{
    @Modifying
    @Query("DELETE FROM Shift s WHERE s.assigned_Worker.id = :userId")
    int deleteByWorkerIdNative(@Param("userId") Long userId);
    
    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE shifts_shift_id IN (SELECT shift_id FROM shifts WHERE worker_id = :userId)", nativeQuery = true)
    void deleteScheduleShiftsJoinTableByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM Shift s WHERE s.assigned_workstation.id = :workstationId")
    int deleteByWorkstationIdNative(@Param("workstationId") Long workstationId);
    
    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE shifts_shift_id IN (SELECT shift_id FROM shifts WHERE workstation_id = :workstationId)", nativeQuery = true)
    void deleteScheduleShiftsJoinTableByWorkstationId(@Param("workstationId") Long workstationId);
    
    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE shifts_shift_id = :shiftId", nativeQuery = true)
    void deleteScheduleShiftsJoinTableByShiftId(@Param("shiftId") Long shiftId);
}
