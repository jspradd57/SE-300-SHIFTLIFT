package se300.shiftlift;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing student blocked dates.
 * Provides methods for students to block/unblock dates when they're unavailable.
 */
@Service
public class StudentBlockedDateService {
    
    private final StudentBlockedDateRepository studentBlockedDateRepository;
    
    /**
     * Constructs a StudentBlockedDateService with required dependencies.
     * 
     * @param studentBlockedDateRepository the repository for student blocked date persistence
     */
    public StudentBlockedDateService(StudentBlockedDateRepository studentBlockedDateRepository) {
        this.studentBlockedDateRepository = studentBlockedDateRepository;
    }
    
    /**
     * Blocks a specific date for a student worker.
     * 
     * @param student the student worker
     * @param date the date to block
     * @return the created StudentBlockedDate entity
     */
    @Transactional
    public StudentBlockedDate blockDate(StudentWorker student, Date date) {
        if (student == null || date == null) {
            throw new IllegalArgumentException("Student and date cannot be null");
        }
        
        int dateValue = date.get_Date();
        
        // Check if already blocked
        if (studentBlockedDateRepository.existsByStudentAndDateValue(student, dateValue)) {
            // Return existing blocked date
            return studentBlockedDateRepository.findByStudent(student).stream()
                .filter(sbd -> sbd.getDateValue() == dateValue)
                .findFirst()
                .orElse(null);
        }
        
        StudentBlockedDate blockedDate = new StudentBlockedDate(student, date);
        return studentBlockedDateRepository.save(blockedDate);
    }
    
    /**
     * Unblocks a specific date for a student worker.
     * 
     * @param student the student worker
     * @param date the date to unblock
     */
    @Transactional
    public void unblockDate(StudentWorker student, Date date) {
        if (student == null || date == null) {
            return;
        }
        
        int dateValue = date.get_Date();
        studentBlockedDateRepository.deleteByStudentAndDateValue(student, dateValue);
    }
    
    /**
     * Checks if a student has blocked a specific date.
     * 
     * @param student the student worker
     * @param date the date to check
     * @return true if the student has blocked this date, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isDateBlocked(StudentWorker student, Date date) {
        if (student == null || date == null) {
            return false;
        }
        
        return studentBlockedDateRepository.existsByStudentAndDateValue(student, date.get_Date());
    }
    
    /**
     * Checks if a student (by ID) has blocked a specific date.
     * 
     * @param studentId the student worker ID
     * @param date the date to check
     * @return true if the student has blocked this date, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isDateBlockedByStudentId(Long studentId, Date date) {
        if (studentId == null || date == null) {
            return false;
        }
        
        return studentBlockedDateRepository.existsByStudentIdAndDateValue(studentId, date.get_Date());
    }
    
    /**
     * Gets all blocked dates for a specific student.
     * 
     * @param student the student worker
     * @return list of all blocked dates for the student
     */
    @Transactional(readOnly = true)
    public List<StudentBlockedDate> getBlockedDatesForStudent(StudentWorker student) {
        if (student == null) {
            return List.of();
        }
        return studentBlockedDateRepository.findByStudent(student);
    }
    
    /**
     * Gets all blocked dates for a specific student ID.
     * 
     * @param studentId the student worker ID
     * @return list of all blocked dates for the student
     */
    @Transactional(readOnly = true)
    public List<StudentBlockedDate> getBlockedDatesForStudentId(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return studentBlockedDateRepository.findByStudentId(studentId);
    }
    
    /**
     * Gets all student blocked dates.
     * 
     * @return list of all student blocked dates
     */
    @Transactional(readOnly = true)
    public List<StudentBlockedDate> getAllStudentBlockedDates() {
        return studentBlockedDateRepository.findAll();
    }
    
    /**
     * Gets a page of student blocked dates.
     * 
     * @param pageable pagination information
     * @return page of student blocked dates
     */
    @Transactional(readOnly = true)
    public Page<StudentBlockedDate> list(Pageable pageable) {
        return studentBlockedDateRepository.findAll(pageable);
    }
    
    /**
     * Gets a page of student blocked dates matching a specification.
     * 
     * @param pageable pagination information
     * @param filter specification for filtering
     * @return page of student blocked dates matching the filter
     */
    @Transactional(readOnly = true)
    public Page<StudentBlockedDate> list(Pageable pageable, Specification<StudentBlockedDate> filter) {
        return studentBlockedDateRepository.findAll(filter, pageable);
    }
    
    /**
     * Counts the number of student blocked dates.
     * 
     * @return the count of student blocked dates
     */
    @Transactional(readOnly = true)
    public long count() {
        return studentBlockedDateRepository.count();
    }
}
