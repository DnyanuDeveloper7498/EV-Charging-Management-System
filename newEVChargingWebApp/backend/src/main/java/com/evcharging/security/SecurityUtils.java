package com.evcharging.security;

import com.evcharging.model.Roles;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helpers for "who is calling?" and ownership checks. The user id ALWAYS comes from the JWT, never from the request. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw new AccessDeniedException("Authentication required.");
        }
        return user;
    }

    public static int currentUserId() {
        return currentUser().userId();
    }

    public static boolean isAdmin() {
        return Roles.ADMIN.equals(currentUser().role());
    }

    public static boolean isSelfOrAdmin(int userId) {
        AuthUser user = currentUser();
        return user.userId() == userId || Roles.ADMIN.equals(user.role());
    }

    public static void requireSelfOrAdmin(int userId) {
        if (!isSelfOrAdmin(userId)) {
            throw new AccessDeniedException("You can only access your own data.");
        }
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Administrator access required.");
        }
    }

//----------------finaceadmin-------------
    public static void requireAdminOrFinanceAdmin() {

        String role = currentUser().role();

        if (!Roles.ADMIN.equals(role)
                && !Roles.FINANCE_ADMIN.equals(role)) {

            throw new AccessDeniedException(
                    "Finance department access required."
            );
        }
    }


    //-----------user admin------------
    public static void requireAdminOrUserAdmin() {

        String role = currentUser().role();

        if (!Roles.ADMIN.equals(role)
                && !Roles.USER_ADMIN.equals(role)) {

            throw new AccessDeniedException(
                    "User management access required."
            );
        }
    }



    //---------------Admin (Super Admin) and Vehicle_admin-------

    public static  boolean isAdminOrVehicleAdmin()
    {
        String role=currentUser().role();

        return Roles.ADMIN.equals(role)||Roles.VEHICLE_ADMIN.equals(role);
    }


    public static  boolean isSelfOrAdminOrVehicleAdmin(int userId)
    {
        return currentUser().userId()==userId|| isAdminOrVehicleAdmin();
    }


    public static  void requireAdminOrVehicleAdmin()
    {
        if (!isAdminOrVehicleAdmin())
        {
            throw  new AccessDeniedException("Vehicle management access required.");
        }
    }


    //--------------- ADMIN (Super Admin) Or CHARGING_ADMIN --------------
    public static boolean isAdminOrChargingAdmin() {
        String role = currentUser().role();
        return Roles.ADMIN.equals(role) || Roles.CHARGING_ADMIN.equals(role);
    }

    public static void requireAdminOrChargingAdmin() {
        if (!isAdminOrChargingAdmin()) {
            throw new AccessDeniedException("Charging management access required.");
        }
    }



}
