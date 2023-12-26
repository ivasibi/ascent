package org.ascent.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.ascent.enums.Role;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password", length = 60)
    private String password;

    @Column(name = "disabled")
    private boolean disabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "created_on")
    private Instant createdOn;

    @Column(name = "last_login")
    private Instant lastLogin;
}