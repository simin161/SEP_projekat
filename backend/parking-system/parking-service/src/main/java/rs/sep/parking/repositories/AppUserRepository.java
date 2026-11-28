package rs.sep.parking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.sep.parking.model.AppUser;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    boolean existsByEmailIgnoreCase(String email);
}
