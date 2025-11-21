package Recommend.Movie.Controller;

import Recommend.Movie.DTO.NicknameUpdateRequest;
import Recommend.Movie.DTO.UserDTO.UserFindResponseDTO;
import Recommend.Movie.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
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

    /*
     * param userId : userId
     * */
    @PutMapping(value = "/user/{userId}/nickname")
    public ResponseEntity<String> updateUserNickname(
            @PathVariable int userId,
            @Valid @RequestBody NicknameUpdateRequest request,
            Authentication authentication) {

        // 1. 본인 인증 검사
        int authenticatedUserId = Integer.parseInt(authentication.getName());
        if (authenticatedUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to change this user's nickname.");
        }



        // 2. 닉네임 변경 로직
        try {
            userService.updateNickname(userId, request.newNickname());
            return ResponseEntity.ok("Nickname updated successfully.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("taken")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            }
            if (e.getMessage().contains("same")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed.");
        }
    }
}