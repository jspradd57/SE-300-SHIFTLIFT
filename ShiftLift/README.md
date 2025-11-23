# ShiftLift - Student Worker Shift Management System

ShiftLift is a comprehensive web-based application for managing student worker schedules, shifts, and workstations. Built with Spring Boot and Vaadin, it provides an intuitive interface for both administrators and student workers to manage work schedules efficiently.

## Features

### For Administrators
- **User Management**: Create, edit, and delete student workers and manager accounts
- **Workstation Management**: Manage workstations and track availability
- **Schedule Management**: Create and publish weekly schedules with automatic conflict detection
- **Shift Override**: Override shift assignments with notifications and automatic reassignment
- **Seniority System**: Automatic seniority calculation based on hire date
- **PDF Export**: Download schedules as PDF documents
- **Visual Calendar**: Interactive weekly calendar with half-hour time slots and color-coded workstations

### For Student Workers
- **View Schedules**: See published weekly schedules with visual calendar display
- **Request Shifts**: Create shift requests (subject to admin approval)
- **Senior Worker Override**: Workers with higher seniority can claim shifts from junior workers
- **Max Hours Protection**: Automatic validation prevents exceeding maximum weekly hours (20 hours default)
- **Password Management**: Change password securely

### System Features
- **Conflict Detection**: Prevents double-booking of workers and workstations
- **Automatic Validation**: 
  - Maximum 1 published and 1 unpublished schedule at a time
  - Prevents exceeding student worker max hours
  - Workstation availability checking
- **Responsive Design**: Day labels and UI elements scale with screen resolution
- **Cascade Deletion**: Safe deletion of users, workstations, and schedules with automatic cleanup
- **Color-Coded Workstations**: Visual distinction between different workstations

## Technology Stack

- **Backend**: Spring Boot 3.3.5, Spring Data JPA
- **Frontend**: Vaadin 24.5.3 (Java-based web framework)
- **Database**: MySQL (configured for remote database)
- **Build Tool**: Maven
- **Java Version**: 17+

## Project Structure

```
src/
├── main/
│   ├── frontend/
│   │   ├── themes/default/
│   │   │   ├── styles.css
│   │   │   └── theme.json
│   │   └── generated/ (auto-generated Vaadin resources)
│   ├── java/se300/shiftlift/
│   │   ├── Application.java (main entry point)
│   │   ├── Auth.java (authentication service)
│   │   ├── *View.java (UI views)
│   │   ├── *Service.java (business logic)
│   │   ├── *Repository.java (data access)
│   │   └── Domain models (User, Shift, Schedule, Workstation, etc.)
│   └── resources/
│       └── application.properties (database configuration)
└── test/ (unit tests)
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL database access

### Database Configuration

Edit `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:mysql://your-database-host:3306/your-database-name
spring.datasource.username=your-username
spring.datasource.password=your-password
```

### Running in Development Mode

1. Import the project into your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Run the `Application.java` class

Or from command line:

```bash
./mvnw spring-boot:run
```

The application will start at `http://localhost:8080`

### Building for Production

```bash
./mvnw clean package -Pproduction
```

The production-ready JAR will be in `target/` directory.

### Docker Deployment

Build Docker image:
```bash
docker build -t shiftlift:latest .
```

Run container:
```bash
docker run -p 8080:8080 shiftlift:latest
```

## User Guide

### First Time Setup (Administrator)

1. **Login** with administrator credentials
2. **Create Workstations** (Manage Workstations → Add New Workstation)
3. **Add Student Workers** (Manage Workers → Add New Worker)
4. **Create a Schedule** (Manage Schedules → Create New Schedule)
5. **Add Shifts** to the schedule
6. **Publish Schedule** when ready

### Creating a New Schedule

1. Navigate to **Manage Schedules**
2. Click **Create New Schedule**
3. Enter start and end dates (must be Monday-Friday)
4. Save (schedule starts as "unpublished")
5. Add shifts through the main menu calendar
6. Publish when complete

**Note**: Only 1 published and 1 unpublished schedule allowed at a time.

### Adding Shifts

**Method 1: From Main Menu Calendar**
- Click on any day column in the calendar
- Fill in worker, workstation, start time, and end time
- Click "Add Shift"

**Method 2: From Navigation Menu**
- Click "Create New Shift" (or "Request New Shift" for students)
- Select date, worker, workstation, and times
- Click "Add Shift"

### Seniority and Override System

**Seniority Calculation**:
- Automatically calculated based on hire date
- Lower number = higher seniority
- Senior workers can override junior workers' shifts

**Admin Override**:
- Admins receive a confirmation dialog when creating conflicting shifts
- Two scenarios:
  - **Available workstation**: Conflicting worker is reassigned automatically
  - **No available workstation**: Conflicting worker's shift is deleted (with warning)

**Senior Worker Override**:
- When a senior worker requests an occupied workstation
- Confirmation dialog appears
- Junior worker is reassigned to available workstation
- Requires at least one other workstation available

### Managing Users

**Add New Worker**:
1. Navigate to **Manage Workers**
2. Click **Add New Worker**
3. Enter username, email, password
4. Set hire date (affects seniority)
5. Set max hours (default: 20)
6. Select worker type (Student Worker or Manager)

**Edit Worker**:
1. Go to **Manage Workers**
2. Click **Edit** on desired worker
3. Modify fields (password, max hours, etc.)
4. Click **Update**

**Delete Worker**:
- Deletion automatically removes all associated shifts
- Seniority numbers are recalculated for remaining workers

### Managing Workstations

**Add Workstation**:
1. Navigate to **Manage Workstations**
2. Click **Add New Workstation**
3. Enter workstation name
4. Save

**Delete Workstation**:
- Automatically removes all shifts assigned to that workstation

### Viewing and Editing Shifts

**View Shifts**:
- Main menu displays current week's schedule
- Use arrow buttons to navigate between weeks
- Click on shift blocks to view details

**Edit Shift**:
- Click on a shift block in the calendar
- Edit worker, workstation, or time
- Save changes

**Delete Shift**:
- Click on shift block
- Click **Delete** button
- Confirm deletion

### Schedule Publishing

**Unpublished Schedule**:
- Visible in main menu
- Can be edited freely
- Shows as "Pending Schedule" in header

**Publishing**:
1. Navigate to **Manage Schedules**
2. Select unpublished schedule
3. Click **Publish**
4. Schedule becomes read-only to students

**Cleaning Up**:
- Old schedules can be deleted manually
- Automatic cleanup available for expired schedules

### PDF Export

1. Open main menu with desired schedule
2. Click **Download Schedule PDF** in navigation menu
3. PDF downloads with:
   - Week date range
   - All shifts organized by day
   - Worker and workstation assignments
   - Time slots

### Password Management

1. Click **Change Password** in navigation menu
2. Enter current password
3. Enter new password twice
4. Click **Change Password**

## Validation Rules

### Shift Creation
- ✅ Start time must be before end time
- ✅ Times must be within operating hours (8:00 AM - 5:00 PM)
- ✅ Worker cannot be double-booked
- ✅ Student workers cannot exceed max hours per week
- ✅ Workstation conflict detection with override options

### Schedule Management
- ✅ Maximum 1 published schedule at a time
- ✅ Maximum 1 unpublished schedule at a time
- ✅ Dates must be Monday through Friday
- ✅ End date must be after start date

### User Management
- ✅ Unique usernames and emails
- ✅ Hire date affects seniority ranking
- ✅ Max hours must be positive

## Troubleshooting

**Cannot create schedule**: Check if you already have 1 published + 1 unpublished schedule. Delete expired schedules first.

**Worker double-booked error**: The selected worker already has a shift at that time. Choose a different time or worker.

**Workstation occupied**: Another worker is assigned to this workstation. Use override if you have seniority/admin privileges.

**Max hours exceeded**: Student worker would exceed their weekly hour limit. Reduce shift hours or choose another worker.

**Cannot delete user/workstation**: Deletion cascade is automatic. If errors occur, check database connection.

## Support and Maintenance

For issues, bugs, or feature requests, contact the development team or create an issue in the project repository.

## License

See LICENSE.md for license information.
