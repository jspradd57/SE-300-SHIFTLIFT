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

    /**
     * Constructs the workstation management view with search, pagination, and navigation.
     * Initializes the layout with drawer menu, header, and workstation list display.
     */
    public ListWorkstationsView(WorkstationService workstatioService, ScheduleService scheduleService)
    {
        this.workstationService = workstatioService;
        this.scheduleService = scheduleService;
        
        boolean admin = Auth.isAdmin();
        
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);
        
        if(admin){
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink viewPublishedScheduleLink = new RouterLink("View Published Schedule", PublishedScheduleView.class);
            RouterLink manageWorkersLink = new RouterLink("Manage Workers", ListUsersView.class);
            RouterLink manageWorkstationsLink = new RouterLink("Manage Workstations", ListWorkstationsView.class);
            RouterLink manageSchedulesLink = new RouterLink("Manage Schedules", ManageSchedulesView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(viewPublishedScheduleLink);
            styleRouterLink(manageWorkersLink);
            styleRouterLink(manageWorkstationsLink);
            styleRouterLink(manageSchedulesLink);
            styleRouterLink(changePasswordLink);
            
            drawerLayout.add(viewPendingScheduleLink, viewPublishedScheduleLink, manageWorkersLink, manageWorkstationsLink, manageSchedulesLink, downloadPdfButton, changePasswordLink);
        }
        else{
            RouterLink viewPendingScheduleLink = new RouterLink("View Pending Schedule", MainMenuView.class);
            RouterLink viewPublishedScheduleLink = new RouterLink("View Published Schedule", PublishedScheduleView.class);
            RouterLink changePasswordLink = new RouterLink("Change Password", ChangePasswordView.class);
            
            Button downloadPdfButton = createDownloadPdfButton();
            
            styleRouterLink(viewPendingScheduleLink);
            styleRouterLink(viewPublishedScheduleLink);
            styleRouterLink(changePasswordLink);
            
            drawerLayout.add(viewPendingScheduleLink, viewPublishedScheduleLink, downloadPdfButton, changePasswordLink);
        }
        
        addToDrawer(drawerLayout);
        
        setDrawerOpened(false);

        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
            .set("color", "#156fabff")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "4px");

        Button logoutButton = new Button("Logout");
        logoutButton.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutButton.addClickListener(e -> Auth.logoutToLogin());

        var header = new HorizontalLayout(toggle, logoutButton);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
            .set("background-color", "white")
            .set("padding", "16px 20px");
        
        H2 navTitle = new H2("Workstations");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");
        
        header.removeAll();
        header.add(toggle, navTitle, logoutButton);
        addToNavbar(header);
        
        searchField.setPlaceholder("Search workstations...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("400");
        searchField.getStyle().set("font-family", "Poppins, sans-serif");
        searchField.addValueChangeListener(e -> {
            this.currentQuery = e.getValue() == null ? "" : e.getValue();
            this.currentPage = 0;
            loadWorkstations(currentQuery, currentPage);
        });

        Button newWorkstationButton = new Button("Create New Workstation");
        Button returnButton = new Button("Return");

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
        
        newWorkstationButton.addClickListener(e -> UI.getCurrent().navigate("new-workstation"));
        returnButton.addClickListener(e -> UI.getCurrent().navigate(MainMenuView.class));

        editButton.addClickListener(e ->{
            if(selectedItem != null) {
                String workstationName = selectedItem.getElement().getProperty("_workstation");
                java.util.Map<String, java.util.List<String>> parameters = Collections.singletonMap("name", 
                    java.util.Collections.singletonList(workstationName));
                QueryParameters qp = new QueryParameters(parameters);
                UI.getCurrent().navigate("edit-workstation", qp);
            }
        });

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

        listLayout.setWidth("600px");
        listLayout.setPadding(false);
        listLayout.setSpacing(true);
        listLayout.setAlignItems(FlexComponent.Alignment.START);
        listLayout.getStyle()
            .set("gap", "6px")
            .set("margin-top", "16px");

        HorizontalLayout pageLayout = new HorizontalLayout(prevButton, nextButton);
        pageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        pageLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        pageLayout.setSpacing(true);

        HorizontalLayout buttonLayout = new HorizontalLayout(newWorkstationButton, editButton, returnButton);
        buttonLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);

        VerticalLayout bottomLayout = new VerticalLayout(pageLayout, buttonLayout);
        bottomLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        bottomLayout.setSpacing(true);
        bottomLayout.setPadding(true);
        bottomLayout.getStyle().set("margin-top", "24px");

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

        VerticalLayout contentLayout = new VerticalLayout(searchLayout, listLayout, bottomLayout);
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);
        contentLayout.setPadding(true);
        contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        setContent(contentLayout);

        loadWorkstations(currentQuery, currentPage);
    }
    
    /**
     * Applies ShiftLift styling to navigation drawer links.
     * Sets blue color and Poppins font with proper spacing.
     */
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
    
    /**
     * Loads and displays workstations based on search query and pagination.
     * Fetches workstations from the service and renders them as interactive list items.
     */
    private void loadWorkstations(String query, int page)
    {
        listLayout.removeAll();
        Slice<Workstation> slice;
        if(query == null || query.isEmpty())
        {
            slice = workstationService.searchByName("", PageRequest.of(page, 20));
        }else{
            slice = workstationService.searchByName(query, PageRequest.of(page, 20));
        }

        List<Workstation> workstations = slice.toList();
        
        for(Workstation ws : workstations)
        {
            VerticalLayout workstationInfo = new VerticalLayout();
            workstationInfo.setSpacing(false);
            workstationInfo.setPadding(false);
            workstationInfo.setSizeFull();
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

            HorizontalLayout row = new HorizontalLayout(workstationInfo);
            row.setWidth("560px");
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setHeight("80px");
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            row.expand(workstationInfo);
            row.getStyle()
                .set("padding", "10px 24px")
                .set("border-radius", "8px")
                .set("margin", "0")
                .set("height", "80px")
                .set("max-height", "80px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("box-sizing", "border-box");

            Button item = new Button(row);
            item.getStyle()
                .set("width", "100%")
                .set("height", "80px")
                .set("text-align", "left")
                .set("padding", "0")
                .set("background", "white")
                .set("cursor", "pointer")
                .set("border", "none")
                .set("margin", "0 auto")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("transition", "all 0.2s"); 
            
            item.addClickListener(e -> {
                if(selectedItem == item){
                    deselectWorkstation(selectedItem);
                    selectedItem = null;
                    editButton.setEnabled(false);
                    editButton.getStyle().set("opacity", "0.5");
                }else{
                    if(selectedItem != null) {
                        deselectWorkstation(selectedItem);
                    }
                    selectedItem = item;
                    selectWorkstation(item);
                    editButton.setEnabled(true);
                    editButton.getStyle().set("opacity", "1");
                }
                e.getSource().getElement().executeJs("event.stopPropagation()");
            });
            item.getElement().setProperty("_workstation", ws.getName());
            listLayout.add(item);
        }

        prevButton.setEnabled(slice.hasPrevious());
        nextButton.setEnabled(slice.hasNext());
    }

    /**
     * Applies selection styling to a workstation list item.
     * Sets ShiftLift blue background with yellow border and changes text to white.
     */
    private void selectWorkstation(Button workstationButton) {
        workstationButton.getStyle()
            .set("background-color", "#156fabff")
            .set("border", "3px solid #ffc107");
        
        HorizontalLayout row = (HorizontalLayout) workstationButton.getChildren().findFirst().orElse(null);
        if (row != null) {
            row.getChildren().forEach(component -> {
                if (component instanceof VerticalLayout) {
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

    /**
     * Removes selection styling from a workstation list item.
     * Resets background, border, and text colors to original state.
     */
    private void deselectWorkstation(Button workstationButton) {
        workstationButton.getStyle()
            .set("background-color", "white")
            .set("border", "none");
        
        HorizontalLayout row = (HorizontalLayout) workstationButton.getChildren().findFirst().orElse(null);
        if (row != null) {
            row.getChildren().forEach(component -> {
                if (component instanceof VerticalLayout) {
                    VerticalLayout workstationInfo = (VerticalLayout) component;
                    workstationInfo.getChildren().forEach(textComponent -> {
                        if (textComponent instanceof Span) {
                            Span span = (Span) textComponent;
                            String text = span.getText();
                            if (text.contains("Not Set") || text.contains(":")) {
                                span.getStyle().set("color", "#666666");
                            } else {
                                span.getStyle().set("color", "#00070cff");
                            }
                        }
                    });
                }
            });
        }
    }
    
    /**
     * Creates a button for downloading the published schedule as a PDF.
     * Fetches the latest published schedule and generates a PDF document.
     */
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
                
                String tempDir = System.getProperty("java.io.tmpdir");
                String pdfPath = tempDir + "/schedule-" + schedule.getId() + ".pdf";
                SchedulePdfGenerator.generateSchedulePdf(schedule, pdfPath);
                
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
