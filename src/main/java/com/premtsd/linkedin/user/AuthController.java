package com.premtsd.linkedin.user;

import com.premtsd.linkedin.user.AuthDtos.LoginRequest;
import com.premtsd.linkedin.user.AuthDtos.SignupRequest;
import com.premtsd.linkedin.user.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public TokenResponse signup(@Valid @RequestBody SignupRequest req) {
        return new TokenResponse(userService.signup(req));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return new TokenResponse(userService.login(req));
    }
}
