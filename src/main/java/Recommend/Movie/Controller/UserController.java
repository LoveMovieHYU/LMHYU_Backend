package Recommend.Movie.Controller;

import Recommend.Movie.DTO.UserDTO.UserFindResponseDTO;
import Recommend.Movie.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * param userId : userId
     * */
    @GetMapping(value = "/user/{userId}")
    public UserFindResponseDTO userMeApi(@PathVariable int userId) {
        return userService.readUser(userId);
    }

    /**
     * param userId : userId
     * */
    @DeleteMapping(value = "/user/{userId}")
    public ResponseEntity<Boolean> deleteUserApi(@PathVariable int userId) throws AccessDeniedException {
        userService.deleteUser(userId);
        return ResponseEntity.status(200).body(true);
    }


}