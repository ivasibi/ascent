package org.ascent.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "password")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {

    private String username;

    private String email;

    private String password;
}