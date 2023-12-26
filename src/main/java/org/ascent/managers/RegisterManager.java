package org.ascent.managers;

import lombok.RequiredArgsConstructor;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.RegisterRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegisterManager {

    private final UserRepository userRepository;

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameAlreadyInUseException();
        }

        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyInUseException();
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        user.setPassword(bCryptPasswordEncoder.encode(registerRequest.getPassword()));

        user.setRole(Role.USER);
        user.setCreatedOn(Instant.now());
        userRepository.save(user);
    }
}