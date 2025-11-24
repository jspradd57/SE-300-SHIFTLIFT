package se300.shiftlift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for Schedule entity persistence.
 * Extends JpaRepository for built-in CRUD operations.
 * Provides custom query methods for join table cleanup.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {

    /**
     * Cleans up join table entries for a specific schedule.
     * 
     * @param scheduleId the schedule ID
     */
    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE schedule_schedule_id = :scheduleId", nativeQuery = true)
    void deleteScheduleShiftsJoinTable(@Param("scheduleId") Long scheduleId);

}
