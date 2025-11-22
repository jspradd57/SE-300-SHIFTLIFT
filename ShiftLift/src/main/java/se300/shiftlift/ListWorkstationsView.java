package se300.shiftlift;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import jakarta.annotation.security.RolesAllowed;


@PageTitle("List Workstations")
@Route("list-workstations")
@RolesAllowed("ADMIN")
public class ListWorkstationsView extends AppLayout implements BeforeEnterObserver {

    private final WorkstationService workstationService;
    private final ScheduleService scheduleService;
    private final VerticalLayout listLayout = new VerticalLayout();
    private final TextField searchField = new TextField();
    private final Button prevButton = new Button("Previous");
    private final Button nextButton = new Button("Next");
    private final Button editButton = new Button("Edit Workstation");
    private int currentPage = 0;
    private String currentQuery = "";
    private Button selectedItem = null;


    public ListWorkstationsView(WorkstationService workstatioService, ScheduleService scheduleService)
    {
        this.workstationService = workstatioService;
        this.scheduleService = scheduleService;
        
        boolean admin = Auth.isAdmin();
        
        // Create styled drawer menu
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            // Routes that will be in the hamburger for navigation
            RouterLink manageWorkersLink = new RouterLink("Manage Workers", ListUsersView.class);
            RouterLink manageWorkstationsLink = new RouterLink("Manage Workstations", ListWorkstationsView.class);
            RouterLink manageSchedulesLink = new RouterLink("Manage Schedules", ManageSchedulesView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink newShiftLink = new RouterLink("Create New Shift", NewShiftView.class);
            RouterLink mainMenuLink = new RouterLink("Main Menu", MainMenuView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            // Apply styling to each link
            styleRouterLink(manageWorkersLink);
            styleRouterLink(manageWorkstationsLink);
            styleRouterLink(manageSchedulesLink);
            styleRouterLink(newShiftLink);
            styleRouterLink(changePasswordLink);
            styleRouterLink(mainMenuLink);
            
            drawerLayout.add(mainMenuLink, manageWorkersLink, manageWorkstationsLink, manageSchedulesLink, newShiftLink, downloadPdfButton, changePasswordLink);
        }
        else{
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            RouterLink newShiftLink = new RouterLink("Request New Shift", NewShiftView.class);
            RouterLink mainMenuLink = new RouterLink("Main Menu", MainMenuView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(newShiftLink);
            styleRouterLink(changePasswordLink);
            styleRouterLink(mainMenuLink);
            drawerLayout.add(mainMenuLink, newShiftLink, downloadPdfButton, changePasswordLink);
        }
        
        addToDrawer(drawerLayout);
        
        // Set drawer closed by default
        setDrawerOpened(false);

        // Creates a hamburger for navigation to other tabs
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
            .set("color", "#156fabff")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "4px");

        // Logout Button
        Button logoutButton = new Button("Logout");
        logoutButton.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutButton.addClickListener(e -> Auth.logoutToLogin());

        // Navbar layout (this is the header)
        var header = new HorizontalLayout(toggle, logoutButton);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
            .set("background-color", "white")
            .set("padding", "16px 20px");
        
        // Add title to navbar
        H2 navTitle = new H2("Workstations");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");
        
        // Update header to include title
        header.removeAll();
        header.add(toggle, navTitle, logoutButton);
        addToNavbar(header);
        
        //Search field
        searchField.setPlaceholder("Search workstations...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("400");
        searchField.getStyle().set("font-family", "Poppins, sans-serif");
        searchField.addValueChangeListener(e -> {
            this.currentQuery = e.getValue() == null ? "" : e.getValue();
            this.currentPage = 0;
            loadWorkstations(currentQuery, currentPage);
        });

        //Navigation buttons
        Button newWorkstationButton = new Button("Create New Workstation");
        Button returnButton = new Button("Return");

        //Set button styles
        prevButton.getStyle().set("font-family", "Poppins, sans-serif");
        nextButton.getStyle().set("font-family", "Poppins, sans-serif");
        editButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif")
            .set("opacity", "0.5");
        editButton.setEnabled(false);
        newWorkstationButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif"); 
        returnButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "grey");
        
        //Navigation button handlers
        newWorkstationButton.addClickListener(e -> UI.getCurrent().navigate("new-workstation"));
        returnButton.addClickListener(e -> UI.getCurrent().navigate(MainMenuView.class));

        //User url parameter to pass workstation name to edit workstaion view
        editButton.addClickListener(e ->{
            if(selectedItem != null) {
                String workstationName = selectedItem.getElement().getProperty("_workstation");
                java.util.Map<String, java.util.List<String>> parameters = Collections.singletonMap("name", 
                    java.util.Collections.singletonList(workstationName));
                QueryParameters qp = new QueryParameters(parameters);
                UI.getCurrent().navigate("edit-workstation", qp);
            }
        });

        //Add Search layout at the top with page navigation buttons
        HorizontalLayout searchLayout = new HorizontalLayout(searchField);
        searchLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        searchLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        searchLayout.setWidthFull();

        prevButton.addClickListener(e -> {
            if(currentPage > 0)
            {
                currentPage--;
                loadWorkstations(currentQuery, currentPage);
            }
        });

        nextButton.addClickListener(e -> {
            currentPage++;
            loadWorkstations(currentQuery, currentPage);
        });

        //Set list layout styling
        listLayout.setWidth("600px");
        listLayout.setPadding(false);
        listLayout.setSpacing(true);
        listLayout.setAlignItems(FlexComponent.Alignment.START);
        listLayout.getStyle()
            .set("gap", "6px")  // Minimal gap between rows
            .set("margin-top", "16px");

        //Create page navigation layout
        HorizontalLayout pageLayout = new HorizontalLayout(prevButton, nextButton);
        pageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        pageLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        pageLayout.setSpacing(true);

        //Create buttons layout
        HorizontalLayout buttonLayout = new HorizontalLayout(newWorkstationButton, editButton, returnButton);
        buttonLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);

        //Create Navigation layout for buttons set at bottom
        VerticalLayout bottomLayout = new VerticalLayout(pageLayout, buttonLayout);
        bottomLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        bottomLayout.setSpacing(true);
        bottomLayout.setPadding(true);
        bottomLayout.getStyle().set("margin-top", "24px");

        //Backgorund div for to allow for deselection of items
        Div backgroundDiv = new Div();
        backgroundDiv.setSizeFull();
        backgroundDiv.getStyle()
            .set("position", "fixed")
            .set("top", "0")
            .set("left", "0")
            .set ("z-index", "-1");
        backgroundDiv.addClickListener(e -> {
            if(selectedItem != null) {
               deselectWorkstation(selectedItem);
               selectedItem = null;
               editButton.setEnabled(false);
               editButton.getStyle().set("opacity", "0.5");
            }
        });

        //Create main content container
        VerticalLayout contentLayout = new VerticalLayout(searchLayout, listLayout, bottomLayout);
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);
        contentLayout.setPadding(true);
        contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        // Set content for AppLayout
        setContent(contentLayout);

        loadWorkstations(currentQuery, currentPage);
    }
    
    private void styleRouterLink(RouterLink link) {
        link.getStyle()
            .set("color", "#156fabff")
            .set("font-family", "Poppins, sans-serif")
            .set("text-decoration", "none")
            .set("padding", "8px 0")
            .set("display", "block")
            .set("font-size", "16px");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!Auth.isLoggedIn() || !Auth.isAdmin()) {
            Notification.show("Access denied: Admins only", 2000, Notification.Position.MIDDLE);
            event.rerouteTo("main-menu");
        }
    }
    
    private void loadWorkstations(String query, int page)
    {
        listLayout.removeAll();
        Slice<Workstation> slice;
        //Fetch workstations from database based on search query
        if(query == null || query.isEmpty())
        {
            slice = workstationService.searchByName("", PageRequest.of(page, 20));
        }else{
            slice = workstationService.searchByName(query, PageRequest.of(page, 20));
        }

        List<Workstation> workstations = slice.toList();
        
        //For each workstation create a button item in the list
        for(Workstation ws : workstations)
        {
            VerticalLayout workstationInfo = new VerticalLayout();
            workstationInfo.setSpacing(false);
            workstationInfo.setPadding(false);
            workstationInfo.setSizeFull(); // Take full available space in the row
            workstationInfo.setAlignItems(FlexComponent.Alignment.START);
            workstationInfo.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

            Span nameSpan = new Span(ws.getName());
            nameSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#00070cff")
                .set("font-family", "Poppins, sans-serif")
                .set("font-size", "20px")
                .set("margin-bottom", "4px");
            
            Span hoursSpan = new Span((ws.getOperation_hours() != null ? ws.getOperation_hours().toString() : "Not Set"));
            hoursSpan.getStyle()
                .set("color", "#666666")
                .set("font-family", "Poppins, sans-serif")
                .set("font-size", "16px");

            workstationInfo.add(nameSpan, hoursSpan);

            // Create a horizontal row layout for proper styling
            HorizontalLayout row = new HorizontalLayout(workstationInfo);
            row.setWidth("560px"); // Slightly narrower than container
            row.setAlignItems(FlexComponent.Alignment.CENTER); // Vertically center all components
            row.setHeight("80px"); // Match ManageSchedulesView height
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            row.expand(workstationInfo);
            row.getStyle()
                .set("padding", "10px 24px")  // Padding to fit within button
                .set("border-radius", "8px")
                .set("margin", "0")  // Remove margin to prevent overflow
                .set("height", "80px") // Match ManageSchedulesView height
                .set("max-height", "80px") // Prevent expansion beyond button
                .set("display", "flex")
                .set("align-items", "center") // Ensure CSS flex centering
                .set("box-sizing", "border-box"); // Include padding in height calculation

            Button item = new Button(row);
            item.getStyle()
                .set("width", "100%")
                .set("height", "80px") // Match ManageSchedulesView height
                .set("text-align", "left")
                .set("padding", "0")
                .set("background", "white")
                .set("cursor", "pointer")
                .set("border", "none")
                .set("margin", "0 auto") // Center the narrower row in container
                .set("overflow", "hidden") // Prevent content from sticking out
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("transition", "all 0.2s"); 
            
            item.addClickListener(e -> {
                if(selectedItem == item){
                    //Deselect if already selected
                    deselectWorkstation(selectedItem);
                    selectedItem = null;
                    editButton.setEnabled(false);
                    editButton.getStyle().set("opacity", "0.5");
                }else{
                    //Select the workstation
                    if(selectedItem != null) {
                        deselectWorkstation(selectedItem);
                    }
                    selectedItem = item;
                    selectWorkstation(item);
                    editButton.setEnabled(true);
                    editButton.getStyle().set("opacity", "1");
                }
                e.getSource().getElement().executeJs("event.stopPropagation()"); //Force this action handler to run before background click
            });
            //Store workstaion name in element property for retrieval on edit
            item.getElement().setProperty("_workstation", ws.getName());
            listLayout.add(item);
        }

        //Enable/disable navigation buttons dynamically based on database slice info
        prevButton.setEnabled(slice.hasPrevious());
        nextButton.setEnabled(slice.hasNext());
    }

    private void selectWorkstation(Button workstationButton) {
        // Apply ShiftLift blue background and yellow border
        workstationButton.getStyle()
            .set("background-color", "#156fabff")
            .set("border", "3px solid #ffc107");
        
        // Change text colors to white
        HorizontalLayout row = (HorizontalLayout) workstationButton.getChildren().findFirst().orElse(null);
        if (row != null) {
            row.getChildren().forEach(component -> {
                if (component instanceof VerticalLayout) {
                    // Change workstation name and hours text to white
                    VerticalLayout workstationInfo = (VerticalLayout) component;
                    workstationInfo.getChildren().forEach(textComponent -> {
                        if (textComponent instanceof Span) {
                            textComponent.getStyle().set("color", "white");
                        }
                    });
                }
            });
        }
    }

    private void deselectWorkstation(Button workstationButton) {
        // Reset background and border
        workstationButton.getStyle()
            .set("background-color", "white")
            .set("border", "none");
        
        // Reset text colors to original
        HorizontalLayout row = (HorizontalLayout) workstationButton.getChildren().findFirst().orElse(null);
        if (row != null) {
            row.getChildren().forEach(component -> {
                if (component instanceof VerticalLayout) {
                    // Reset workstation name and hours text colors
                    VerticalLayout workstationInfo = (VerticalLayout) component;
                    workstationInfo.getChildren().forEach(textComponent -> {
                        if (textComponent instanceof Span) {
                            Span span = (Span) textComponent;
                            // Restore original colors based on content
                            String text = span.getText();
                            if (text.contains("Not Set") || text.contains(":")) {
                                // Hours - grey color
                                span.getStyle().set("color", "#666666");
                            } else {
                                // Workstation name - dark color
                                span.getStyle().set("color", "#00070cff");
                            }
                        }
                    });
                }
            });
        }
    }
    
    private Button createDownloadPdfButton() {
        Button downloadButton = new Button("Download PDF");
        downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        downloadButton.getStyle()
            .set("color", "#156fabff")
            .set("font-family", "Poppins, sans-serif")
            .set("padding", "8px 0")
            .set("font-size", "16px")
            .set("text-align", "left")
            .set("justify-content", "flex-start");
        
        downloadButton.addClickListener(e -> {
            try {
                // Find the latest published schedule
                List<Schedule> allSchedules = scheduleService.getAllSchedules();
                java.util.Optional<Schedule> latestPublished = allSchedules.stream()
                    .filter(s -> s.getApproved() != null && s.getApproved())
                    .max(java.util.Comparator.comparing(Schedule::getId));
                
                if (latestPublished.isEmpty()) {
                    Notification.show("No published schedule available to download", 
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                Schedule schedule = latestPublished.get();
                scheduleService.loadShiftsForSchedule(schedule);
                
                // Generate PDF to temporary file
                String tempDir = System.getProperty("java.io.tmpdir");
                String pdfPath = tempDir + "/schedule-" + schedule.getId() + ".pdf";
                SchedulePdfGenerator.generateSchedulePdf(schedule, pdfPath);
                
                // Trigger download
                java.io.File pdfFile = new java.io.File(pdfPath);
                com.vaadin.flow.server.StreamResource resource = 
                    new com.vaadin.flow.server.StreamResource("schedule.pdf", 
                        () -> {
                            try {
                                return new java.io.FileInputStream(pdfFile);
                            } catch (java.io.FileNotFoundException ex) {
                                return null;
                            }
                        });
                
                com.vaadin.flow.component.html.Anchor downloadLink = 
                    new com.vaadin.flow.component.html.Anchor(resource, "");
                downloadLink.getElement().setAttribute("download", true);
                downloadLink.setId("pdf-download-" + System.currentTimeMillis());
                
                getElement().appendChild(downloadLink.getElement());
                com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                    "document.getElementById($0).click()", downloadLink.getId().get()
                );
                
                Notification.show("PDF download started", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                Notification.show("Error generating PDF: " + ex.getMessage(), 
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        return downloadButton;
    }
}
