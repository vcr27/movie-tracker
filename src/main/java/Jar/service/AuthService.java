package Jar.service;

import Jar.entity.User;
import Jar.exception.BadRequestException;
import Jar.repository.UserRepository;
import Jar.security.JwtService;
import Jar.dto.RegisterRequest;
import Jar.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;


    public String register (RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");

        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role("USER")
            .build();

        userRepository.save(user);
        return "User registered successfully";
    }

        public String login (LoginRequest request){
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

            if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
                throw new BadRequestException("Invalid credentials");
            }
            return jwtService.generateToken(user.getEmail());

        }

}
