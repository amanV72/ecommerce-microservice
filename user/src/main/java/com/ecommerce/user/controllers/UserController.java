package com.ecommerce.user.controllers;


import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Slf4j
public class UserController {


    private final UserService userService;
   // private static Logger logger= LoggerFactory.getLogger(UserController.class);


    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return new ResponseEntity<>(userService.fetchUsers(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
//        Optional<User> user= userService.fetchOneUser(id);
//        if(user==null) return ResponseEntity.notFound().build();
//        return ResponseEntity.ok(user);

        log.info("Request received for user: {}",id);

        log.trace("This is TRACE level - very detailed logs");
        log.debug("This is DEBUG level - used for development debugging");
        log.info("This is INFO level - General system information");
        log.warn("This is WARN level - Something might be wrong");
        log.error("This is ERROR level - Something failed");

        return userService.fetchOneUser(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());

    }

    @PostMapping
    public ResponseEntity<String> createUsers(@RequestBody UserRequest user) {
        userService.addUser(user);
        return ResponseEntity.ok("User added!");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, @RequestBody UserRequest user) {
        boolean isUpdated = userService.updateUser(id, user);
        return isUpdated ? ResponseEntity.ok("Updated") : ResponseEntity.notFound().build();
    }
}
