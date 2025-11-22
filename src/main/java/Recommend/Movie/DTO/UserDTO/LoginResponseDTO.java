package Recommend.Movie.DTO.UserDTO;

public record LoginResponseDTO(int userId, boolean isNewUser, String accessToken, String refreshToken){ }
