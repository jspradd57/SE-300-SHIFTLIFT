package se300.shiftlift;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for StudentBlockedDate entities.
 * Provides database access methods for managing student blocked dates.
 */
public interface StudentBlockedDateRepository extends JpaRepository<StudentBlockedDate, Long>, JpaSpecificationExecutor<StudentBlockedDate> {
    
    /**
     * Finds all blocked dates for a specific student.
     * 
     * @param student the student worker
     * @return list of blocked dates for the student
     */
    List<StudentBlockedDate> findByStudent(StudentWorker student);
    
    /**
     * Finds all blocked dates for a specific student ID.
     * 
     * @param studentId the student worker ID
     * @return list of blocked dates for the student
     */
    @Query("SELECT sbd FROM StudentBlockedDate sbd WHERE sbd.student.id = :studentId")
    List<StudentBlockedDate> findByStudentId(@Param("studentId") Long studentId);
    
    /**
     * Checks if a student has blocked a specific date.
     * 
     * @param student the student worker
     * @param dateValue the date value as an integer
     * @return true if the student has blocked this date, false otherwise
     */
    boolean existsByStudentAndDateValue(StudentWorker student, int dateValue);
    
    /**
     * Checks if a student ID has blocked a specific date.
     * 
     * @param studentId the student worker ID
     * @param dateValue the date value as an integer
     * @return true if the student has blocked this date, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(sbd) > 0 THEN true ELSE false END FROM StudentBlockedDate sbd WHERE sbd.student.id = :studentId AND sbd.dateValue = :dateValue")
    boolean existsByStudentIdAndDateValue(@Param("studentId") Long studentId, @Param("dateValue") int dateValue);
    
    /**
     * Deletes a blocked date for a specific student and date value.
     * 
     * @param student the student worker
     * @param dateValue the date value as an integer
     */
    @Modifying
    void deleteByStudentAndDateValue(StudentWorker student, int dateValue);
    
    /**
     * Deletes all blocked dates for a specific student.
     * 
     * @param studentId the student worker ID
     */
    @Modifying
    @Query("DELETE FROM StudentBlockedDate sbd WHERE sbd.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
