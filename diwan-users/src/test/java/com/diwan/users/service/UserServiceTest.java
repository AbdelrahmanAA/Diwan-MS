package com.diwan.users.service;

import com.diwan.common.security.JwtService;
import com.diwan.users.dto.AuthResponse;
import com.diwan.users.dto.LoginRequest;
import com.diwan.users.dto.RegisterRequest;
import com.diwan.users.entity.User;
import com.diwan.users.outbox.OutboxService;
import com.diwan.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    private final UserRepository repo = mock(UserRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService(SECRET, 60_000);
    private final UserService service = new UserService(repo, jwt, encoder, outbox, redis);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    private User user(String email, String rawPassword, boolean active) {
        User u = new User();
        u.setFullName("Test User");
        u.setEmail(email);
        u.setPassword(encoder.encode(rawPassword));
        u.setActive(active);
        return u;
    }

    private RegisterRequest register(String email, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Test User");
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private LoginRequest login(String email, String password) {
        LoginRequest l = new LoginRequest();
        l.setEmail(email);
        l.setPassword(password);
        return l;
    }

    @Test
    void registerStoresAHashedPasswordAndReturnsAUsableToken() {
        when(repo.existsByEmail("a@b.c")).thenReturn(false);

        AuthResponse res = service.register(register("a@b.c", "Secret123!"));

        org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(repo).save(saved.capture());
        assertNotEquals("Secret123!", saved.getValue().getPassword());
        assertTrue(encoder.matches("Secret123!", saved.getValue().getPassword()));
        assertTrue(jwt.isValid(res.getToken()));
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(repo.existsByEmail("a@b.c")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.register(register("a@b.c", "x")));
        verify(repo, never()).save(any());
    }

    @Test
    void loginSucceedsWithTheRightPassword() {
        when(repo.findByEmail("a@b.c")).thenReturn(Optional.of(user("a@b.c", "Secret123!", true)));
        assertTrue(jwt.isValid(service.login(login("a@b.c", "Secret123!")).getToken()));
    }

    @Test
    void loginGivesTheSameErrorForUnknownEmailAndWrongPassword() {
        when(repo.findByEmail("a@b.c")).thenReturn(Optional.of(user("a@b.c", "Secret123!", true)));
        when(repo.findByEmail("nobody@b.c")).thenReturn(Optional.empty());

        String wrongPassword = assertThrows(RuntimeException.class, () -> service.login(login("a@b.c", "nope"))).getMessage();
        String unknownEmail = assertThrows(RuntimeException.class, () -> service.login(login("nobody@b.c", "x"))).getMessage();
        assertEquals(wrongPassword, unknownEmail); // does not reveal which emails exist
    }

    @Test
    void deactivatedAccountsCannotLogIn() {
        when(repo.findByEmail("a@b.c")).thenReturn(Optional.of(user("a@b.c", "Secret123!", false)));
        assertThrows(RuntimeException.class, () -> service.login(login("a@b.c", "Secret123!")));
    }

    @Test
    void logoutBlacklistsTheTokenIdUntilItWouldHaveExpired() {
        String token = jwt.generateToken(7L, "a@b.c");
        service.logout(token);
        verify(valueOps).set(eq("blacklist:" + jwt.getTokenId(token)), eq("1"), any(Duration.class));
    }

    @Test
    void logoutWithGarbageDoesNothing() {
        service.logout("not-a-jwt");
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void deactivateMarksTheUserInactiveAndRecordsAnEvent() {
        User u = user("a@b.c", "x", true);
        when(repo.findById(7L)).thenReturn(Optional.of(u));

        service.deactivateUser(7L);

        assertFalse(u.isActive());
        verify(repo).save(u);
        verify(outbox).userInvalidated(7L, "DEACTIVATED");
    }

    @Test
    void deactivatingAnUnknownUserRecordsNothing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        service.deactivateUser(99L);
        verify(outbox, never()).userInvalidated(anyLong(), anyString());
    }

    @Test
    void deleteRemovesTheUserAndRecordsAnEvent() {
        service.deleteUser(7L);
        verify(repo).deleteById(7L);
        verify(outbox).userInvalidated(7L, "DELETED");
    }
}
