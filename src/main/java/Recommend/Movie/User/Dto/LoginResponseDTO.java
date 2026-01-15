package Recommend.Movie.User.Dto;

public record LoginResponseDTO(int userId, boolean isNewUser, String accessToken, String refreshToken){ }
