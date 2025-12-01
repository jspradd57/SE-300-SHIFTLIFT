package se300.shiftlift;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing blocked dates.
 * Provides methods to block/unblock dates and check if a date is blocked.
 */
@Service
public class BlockedDateService {
    
    private final BlockedDateRepository blockedDateRepository;
    
    /**
     * Constructs a BlockedDateService with required dependencies.
     * 
     * @param blockedDateRepository the repository for blocked date persistence
     */
    public BlockedDateService(BlockedDateRepository blockedDateRepository) {
        this.blockedDateRepository = blockedDateRepository;
    }
    
    /**
     * Blocks a specific date.
     * 
     * @param date the date to block
     * @return the created BlockedDate entity
     */
    @Transactional
    public BlockedDate blockDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        
        int dateValue = date.get_Date();
        
        // Check if already blocked
        if (blockedDateRepository.existsByDateValue(dateValue)) {
            return blockedDateRepository.findByDateValue(dateValue).orElse(null);
        }
        
        BlockedDate blockedDate = new BlockedDate(date);
        return blockedDateRepository.save(blockedDate);
    }
    
    /**
     * Unblocks a specific date.
     * 
     * @param date the date to unblock
     */
    @Transactional
    public void unblockDate(Date date) {
        if (date == null) {
            return;
        }
        
        int dateValue = date.get_Date();
        blockedDateRepository.deleteByDateValue(dateValue);
    }
    
    /**
     * Checks if a specific date is blocked.
     * 
     * @param date the date to check
     * @return true if the date is blocked, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isDateBlocked(Date date) {
        if (date == null) {
            return false;
        }
        
        return blockedDateRepository.existsByDateValue(date.get_Date());
    }
    
    /**
     * Gets all blocked dates.
     * 
     * @return list of all blocked dates
     */
    @Transactional(readOnly = true)
    public List<BlockedDate> getAllBlockedDates() {
        return blockedDateRepository.findAll();
    }
    
    /**
     * Gets a page of blocked dates.
     * 
     * @param pageable pagination information
     * @return page of blocked dates
     */
    @Transactional(readOnly = true)
    public Page<BlockedDate> list(Pageable pageable) {
        return blockedDateRepository.findAll(pageable);
    }
    
    /**
     * Gets a page of blocked dates matching a specification.
     * 
     * @param pageable pagination information
     * @param filter specification for filtering
     * @return page of blocked dates matching the filter
     */
    @Transactional(readOnly = true)
    public Page<BlockedDate> list(Pageable pageable, Specification<BlockedDate> filter) {
        return blockedDateRepository.findAll(filter, pageable);
    }
    
    /**
     * Counts the number of blocked dates.
     * 
     * @return the count of blocked dates
     */
    @Transactional(readOnly = true)
    public long count() {
        return blockedDateRepository.count();
    }
}
