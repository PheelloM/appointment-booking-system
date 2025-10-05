package com.appointment.config;

import com.appointment.entity.Branch;
import com.appointment.entity.TimeSlot;
import com.appointment.entity.User;
import com.appointment.repository.BranchRepository;
import com.appointment.repository.TimeSlotRepository;
import com.appointment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);  // Traditional logging
    private final BranchRepository branchRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Traditional constructor instead of @RequiredArgsConstructor
    public DataLoader(BranchRepository branchRepository,
                      TimeSlotRepository timeSlotRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.branchRepository = branchRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Loading initial data...");

        // Load users first
        loadUserData();

        // Only create data if no branches exist
        if (branchRepository.count() == 0) {
            loadBranchData();
            log.info("Initial data loaded successfully!");
        } else {
            log.info("Data already exists, skipping data loading.");
        }
    }

    // ... rest of the methods remain the same
    private void loadUserData() {
        // Only create users if none exist
        if (userRepository.count() == 0) {
            log.info("Creating default users...");

            // Admin user - store without ROLE_ prefix
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    Arrays.asList("ADMIN", "USER")
            );
            userRepository.save(admin);

            // Regular user
            User user = new User(
                    "user",
                    passwordEncoder.encode("user123"),
                    Arrays.asList("USER")
            );
            userRepository.save(user);

            log.info("Default users created successfully!");
        } else {
            log.info("Users already exist, skipping user creation.");
        }
    }

    private void loadBranchData() {
        // Create branches
        Branch branch1 = new Branch();
        branch1.setName("Downtown Branch");
        branch1.setAddress("123 Main St, Downtown");
        branch1.setPhone("+27-11-555-0101");
        branch1.setEmail("downtown@company.com");
        branch1.setOperatingHours("9:00 AM - 6:00 PM");
        branchRepository.save(branch1);

        Branch branch2 = new Branch();
        branch2.setName("Uptown Branch");
        branch2.setAddress("456 Oak Ave, Uptown");
        branch2.setPhone("+27-11-555-0102");
        branch2.setEmail("uptown@company.com");
        branch2.setOperatingHours("8:00 AM - 7:00 PM");
        branchRepository.save(branch2);

        Branch branch3 = new Branch();
        branch3.setName("Westside Branch");
        branch3.setAddress("789 Pine Rd, Westside");
        branch3.setPhone("+27-11-555-0103");
        branch3.setEmail("westside@company.com");
        branch3.setOperatingHours("10:00 AM - 8:00 PM");
        branchRepository.save(branch3);

        // Create time slots for tomorrow
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        createTimeSlot(branch1, tomorrow, LocalTime.of(9, 0), LocalTime.of(9, 30), 3);
        createTimeSlot(branch1, tomorrow, LocalTime.of(9, 30), LocalTime.of(10, 0), 3);
        createTimeSlot(branch1, tomorrow, LocalTime.of(10, 0), LocalTime.of(10, 30), 3);
        createTimeSlot(branch2, tomorrow, LocalTime.of(9, 0), LocalTime.of(9, 30), 2);
        createTimeSlot(branch2, tomorrow, LocalTime.of(10, 0), LocalTime.of(10, 30), 2);
    }

    private void createTimeSlot(Branch branch, LocalDate date, LocalTime start, LocalTime end, int capacity) {
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setBranch(branch);
        timeSlot.setSlotDate(date);
        timeSlot.setStartTime(start);
        timeSlot.setEndTime(end);
        timeSlot.setCapacity(capacity);
        timeSlot.setBookedCount(0);
        timeSlot.setAvailable(true);
        timeSlotRepository.save(timeSlot);
    }
}