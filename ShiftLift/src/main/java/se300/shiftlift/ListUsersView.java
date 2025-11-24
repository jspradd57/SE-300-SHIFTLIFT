package se300.shiftlift;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("List Users")
@Route("list-users")
@RolesAllowed("ADMIN")
public class ListUsersView extends AppLayout implements BeforeEnterObserver {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final VerticalLayout listLayout = new VerticalLayout();
    private final com.vaadin.flow.component.textfield.TextField searchField = new com.vaadin.flow.component.textfield.TextField();
    private final Button prevButton = new Button("Previous");
    private final Button nextButton = new Button("Next");
    private final Button editButton = new Button("Edit User");
    private int currentPage = 0;
    private String currentQuery = "";
    private Button selectedItem = null;

    /**
     * Constructs the List Users View for managing workers and students.
     * Initializes navigation drawer, search functionality, and pagination controls.
     * Displays user list with search, edit, and navigation capabilities.
     */
    public ListUsersView(UserService userService, ScheduleService scheduleService) {
        this.userService = userService;
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

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyle()
            .set("color", "#666666")
            .set("font-family", "Poppins, sans-serif")
            .set("margin-right", "20px");
        logoutBtn.addClickListener(e -> Auth.logoutToLogin());

        H2 navTitle = new H2("Users");
        navTitle.getStyle()
               .set("color", "#156fabff")
               .set("font-family", "Poppins, sans-serif")
               .set("margin", "0")
               .set("font-size", "24px");

        var header = new HorizontalLayout(toggle, navTitle, logoutBtn);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
            .set("background-color", "white")
            .set("padding", "16px 20px");
        addToNavbar(header);

        searchField.setPlaceholder("Search by username...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("400px");
        searchField.getStyle()
            .set("font-family", "Poppins, sans-serif");
        searchField.addValueChangeListener(e -> {
            currentQuery = e.getValue() == null ? "" : e.getValue();
            currentPage = 0;
            loadUsers(currentQuery, currentPage);
        });

        Button newUserButton = new Button("Create New User");
        Button returnButton = new Button("Return");

        prevButton.getStyle()
            .set("font-family", "Poppins, sans-serif");
        
        nextButton.getStyle()
            .set("font-family", "Poppins, sans-serif");
        
        editButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif");
        editButton.setEnabled(false);
        editButton.getStyle().set("opacity", "0.5");
        
        newUserButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white")
            .set("font-family", "Poppins, sans-serif");
            
        returnButton.getStyle()
            .set("font-family", "Poppins, sans-serif");

    newUserButton.addClickListener(e -> UI.getCurrent().navigate("NewWorker"));
    returnButton.addClickListener(e -> UI.getCurrent().navigate(MainMenuView.class));
        

        editButton.addClickListener(e -> {
            if (selectedItem != null) {
                String username = selectedItem.getElement().getProperty("_user");
                java.util.Map<String, java.util.List<String>> params = Collections.singletonMap("username", 
                    java.util.Collections.singletonList(username));
                QueryParameters qp = new QueryParameters(params);
                UI.getCurrent().navigate("EditUserView", qp);
            }
        });

        HorizontalLayout searchLayout = new HorizontalLayout(searchField);
        searchLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        searchLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        searchLayout.setWidthFull();

        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadUsers(currentQuery, currentPage);
            }
        });
        nextButton.addClickListener(e -> {
            currentPage++;
            loadUsers(currentQuery, currentPage);
        });

        listLayout.setWidth("600px");
        listLayout.setSpacing(true);
        listLayout.setPadding(false);
        listLayout.setAlignItems(FlexComponent.Alignment.START);
        listLayout.getStyle()
            .set("gap", "6px")
            .set("margin-top", "16px");
            
        HorizontalLayout paginationLayout = new HorizontalLayout(prevButton, nextButton);
        paginationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        paginationLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        paginationLayout.setSpacing(true);

        HorizontalLayout actionLayout = new HorizontalLayout(newUserButton, editButton, returnButton);
        actionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        actionLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        actionLayout.setSpacing(true);
        
        returnButton.getStyle()
            .set("font-family", "Poppins, sans-serif")
            .set("color", "#666666");

        VerticalLayout bottomLayout = new VerticalLayout(paginationLayout, actionLayout);
        bottomLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        bottomLayout.setSpacing(true);
        bottomLayout.setPadding(true);
        bottomLayout.getStyle().set("margin-top", "24px");
        
        Div background = new Div();
        background.setSizeFull();
        background.getStyle()
            .set("position", "fixed")
            .set("top", "0")
            .set("left", "0")
            .set("z-index", "-1");

        background.addClickListener(e -> {
            if (selectedItem != null) {
                deselectUser(selectedItem);
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

        loadUsers(currentQuery, currentPage);
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

    /**
     * Loads and displays users based on search query and pagination.
     * Fetches users from the service and renders them as interactive list items.
     */
    private void loadUsers(String query, int page) {
        listLayout.removeAll();
        org.springframework.data.domain.Slice<User> slice;
        if (query == null || query.isEmpty()) {
            slice = userService.searchByUsername("", org.springframework.data.domain.PageRequest.of(page, 20));
        } else {
            slice = userService.searchByUsername(query, org.springframework.data.domain.PageRequest.of(page, 20));
        }

        List<User> users = slice.toList();

        for (User u : users) {
            Avatar avatar = new Avatar(u.getUsername());
            avatar.setAbbreviation(u.getInitials());
            avatar.setHeight("50px");
            avatar.setWidth("50px");

            VerticalLayout userInfo = new VerticalLayout();
            userInfo.setSpacing(false);
            userInfo.setPadding(false);
            userInfo.setSizeFull();
            userInfo.setAlignItems(FlexComponent.Alignment.START);
            userInfo.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.CENTER);

            Span username = new Span(u.getUsername());
            username.getStyle()
                .set("font-weight", "600")
                .set("color", "#00070cff")
                .set("font-family", "Poppins, sans-serif")
                .set("font-size", "16px")
                .set("margin-bottom", "4px");

            Span email = new Span(u.getEmail());
            email.getStyle()
                .set("color", "#666666")
                .set("font-family", "Poppins, sans-serif")
                .set("font-size", "14px");

            userInfo.add(username, email);

            String labelText;
            String labelColor;
            if (u instanceof ManagerUser) {
                labelText = "Manager";
                labelColor = "#156fabff";
            } else {
                int s = u.getSeniority();
                labelText = s > 0 ? String.valueOf(s) : "";
                labelColor = "#000000";
            }
            Span labelSpan = new Span(labelText);
            labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", labelColor)
                .set("font-family", "Poppins, sans-serif")
                .set("font-size", "16px")
                .set("align-self", "center");

            HorizontalLayout row = new HorizontalLayout(avatar, userInfo, labelSpan);
            row.setWidth("560px");
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setHeight("80px");
            row.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
            row.expand(userInfo);
            row.getStyle()
                .set("padding", "10px 24px")
                .set("border-radius", "8px")
                .set("margin", "0")
                .set("height", "80px")
                .set("max-height", "80px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("box-sizing", "border-box");
            avatar.getStyle()
                .set("margin-right", "16px")
                .set("flex-shrink", "0")
                .set("align-self", "center");

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
                if (selectedItem == item) {
                    deselectUser(selectedItem);
                    selectedItem = null;
                    editButton.setEnabled(false);
                    editButton.getStyle().set("opacity", "0.5");
                } else {
                    if (selectedItem != null) {
                        deselectUser(selectedItem);
                    }
                    selectedItem = item;
                    selectUser(item);
                    editButton.setEnabled(true);
                    editButton.getStyle().set("opacity", "1");
                }
                e.getSource().getElement().executeJs("event.stopPropagation()");
            });

            item.getElement().setProperty("_user", u.getUsername());

            listLayout.add(item);
        }

        prevButton.setEnabled(slice.hasPrevious());
        nextButton.setEnabled(slice.hasNext());
    }

    /**
     * Applies selection styling to a user list item.
     * Sets ShiftLift blue background with yellow border and changes text to white.
     */
    private void selectUser(Button userButton) {
        userButton.getStyle()
            .set("background-color", "#156fabff")
            .set("border", "3px solid #ffc107");
        
        HorizontalLayout row = (HorizontalLayout) userButton.getChildren().findFirst().orElse(null);
        if (row != null) {
            row.getChildren().forEach(component -> {
                if (component instanceof Avatar) {
                    component.getStyle().set("color", "white");
                } else if (component instanceof VerticalLayout) {
                    VerticalLayout userInfo = (VerticalLayout) component;
                    userInfo.getChildren().forEach(textComponent -> {
                        if (textComponent instanceof Span) {
                            textComponent.getStyle().set("color", "white");
                        }
                    });
                } else if (component instanceof Span) {
                    component.getStyle().set("color", "white");
                }
            });
        }
    }

    /**
     * Removes selection styling from a user list item.
     * Resets background, border, and text colors to original state.
     */
    private void deselectUser(Button userButton) {
        userButton.getStyle()
            .set("background-color", "white")
            .set("border", "none");
        
        HorizontalLayout row = (HorizontalLayout) userButton.getChildren().findFirst().orElse(null);
        if (row != null) {
            row.getChildren().forEach(component -> {
                if (component instanceof Avatar) {
                    component.getStyle().remove("color");
                } else if (component instanceof VerticalLayout) {
                    VerticalLayout userInfo = (VerticalLayout) component;
                    userInfo.getChildren().forEach(textComponent -> {
                        if (textComponent instanceof Span) {
                            Span span = (Span) textComponent;
                            String text = span.getText();
                            if (text.contains("@")) {
                                span.getStyle().set("color", "#666666");
                            } else {
                                span.getStyle().set("color", "#00070cff");
                            }
                        }
                    });
                } else if (component instanceof Span) {
                    Span labelSpan = (Span) component;
                    String text = labelSpan.getText();
                    if ("Manager".equals(text)) {
                        labelSpan.getStyle().set("color", "#156fabff");
                    } else {
                        labelSpan.getStyle().set("color", "#000000");
                    }
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
                    Notification.show("No published schedule available", 3000, Notification.Position.MIDDLE)
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
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        return downloadButton;
    }
}
