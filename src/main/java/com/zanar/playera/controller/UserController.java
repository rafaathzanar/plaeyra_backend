//package com.zanar.playera.controller;
//
//import com.zanar.playera.entity.User;
//import com.zanar.playera.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//
//@RestController
//@RequestMapping("/api/users")
//public class UserController {
//    @Autowired
//    private UserService userService;
//
//    @PostMapping("/register")
//    public ResponseEntity<?> registerUser(@RequestBody User user) {
//        if (userService.getUserByEmail(user.getEmail()).isPresent()) {
//            return ResponseEntity.badRequest().body("Email already exists!");
//        }
//        User newUser = userService.registerUser(user);
//        return ResponseEntity.ok(newUser);
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<?> getUserById(@PathVariable Long id) {
//        return userService.getUserById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//}
//
