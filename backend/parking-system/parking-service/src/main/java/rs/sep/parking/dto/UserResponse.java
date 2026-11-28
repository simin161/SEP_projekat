package rs.sep.parking.dto;

import rs.sep.parking.model.AppUser;

import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
