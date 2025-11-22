package se300.shiftlift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {

    @Modifying
    @Query(value = "DELETE FROM schedules_shifts WHERE schedule_schedule_id = :scheduleId", nativeQuery = true)
    void deleteScheduleShiftsJoinTable(@Param("scheduleId") Long scheduleId);

}
