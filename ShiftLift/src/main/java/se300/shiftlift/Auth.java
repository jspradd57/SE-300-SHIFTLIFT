package se300.shiftlift;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * Utility class for managing user authentication and session.
 * Provides methods for login, logout, and user state checks.
 */
public final class Auth {
    private Auth() {}

    /**
     * Sets the current user in the session.
     * 
     * @param user the user to set as current
     */
    public static void setCurrentUser(User user) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(User.class, user);
        }
    }

    /**
     * Gets the current user from the session.
     * 
     * @return the current user, or null if not logged in
     */
    public static User getCurrentUser() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? session.getAttribute(User.class) : null;
    }

    /**
     * Checks if a user is currently logged in.
     * 
     * @return true if a user is logged in, false otherwise
     */
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    /**
     * Checks if the current user is an admin (ManagerUser).
     * 
     * @return true if current user is admin, false otherwise
     */
    public static boolean isAdmin() {
        return getCurrentUser() instanceof ManagerUser;
    }

    /**
     * Logs out the current user and navigates to the login page.
     * Clears the session and user attributes.
     */
    public static void logoutToLogin() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(User.class, null);
            session.close();
        }
        UI ui = UI.getCurrent();
        if (ui != null) ui.navigate("");
    }
}