package se300.shiftlift;

import java.util.List;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
@Route("")

public class LoginView extends VerticalLayout {

    @SuppressWarnings("unused")
    private final UserRepository userRepository;

    /**
     * Constructs the login view with username/password authentication.
     * Sets up UI elements including title, input fields, and login/forgot password buttons.
     * 
     * @param userRepository the repository for accessing user data
     */
    public LoginView(UserRepository userRepository) {
        this.userRepository = userRepository;
        H1 loginTitle = new H1("ShiftLift");
        loginTitle.getStyle()
            .set("color", "#156fabff")
            .set("font-family", "Poppins, sans-serif")
            .set("font-size", "100px")
            .set("margin-left", "center")
            .set("margin-top", "30px");
        add(loginTitle);
        setHorizontalComponentAlignment(Alignment.CENTER, loginTitle);

        var inputUser = new TextField("Username");
        var inputPassword = new PasswordField("Password");
        
        inputUser.setWidth("300px");
        inputPassword.setWidth("300px");
        
        var loginButton = new Button("Login");
        loginButton.getStyle()
            .set("background-color", "#156fabff")
            .set("color", "white");
        loginButton.setWidth("300px");
        
        inputUser.addKeyPressListener(Key.ENTER, e -> loginButton.click());
        inputPassword.addKeyPressListener(Key.ENTER, e -> loginButton.click());
            
        var changePassword = new Button("Forgot Password");
        changePassword.getStyle()
            .set("color", "gray");
        changePassword.setWidth("300px");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.add(inputUser, inputPassword, loginButton, changePassword);

        add(layout);

        layout.setWidthFull();

        loginButton.addClickListener(e -> {
            String username = inputUser.getValue();
            String password = inputPassword.getValue();

            if (username.isEmpty() || password.isEmpty()) {
                Notification.show("Please enter both username and password")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            List<User> users = userRepository.findByUsername(username);
            if (users.isEmpty()) {
                Notification.show("User not found")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {
                User user = users.get(0);
                if (PasswordUtil.matches(password, user.getPassword())) {
                    Auth.setCurrentUser(user);
                    getUI().ifPresent(ui -> ui.navigate(MainMenuView.class));
                } else {
                    Notification.show("Invalid password")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });

        changePassword.setText("Forgot Password");
        changePassword.addClickListener(e -> {
            if (!Auth.isLoggedIn()) {
                Notification.show("Please log in to change your password")
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }
            getUI().ifPresent(ui -> ui.navigate(ChangePasswordView.class));
        });
    }
}