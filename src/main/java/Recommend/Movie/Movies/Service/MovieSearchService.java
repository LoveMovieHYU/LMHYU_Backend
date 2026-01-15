package Recommend.Movie.Movies.Service;

import Recommend.Movie.Movies.Dto.SearchMovieResponse;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Movies.Repository.MovieResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieSearchService {

    private final MovieResponseRepository movieResponseRepository;

    public List<SearchMovieResponse> searchByCategory(String category, String query) {
        // 검색어가 없으면 인기 작품 반환
        if (query == null || query.trim().isEmpty()) {
            return movieResponseRepository.findTop10ByOrderByVoteAverageDesc().stream()
                    .map(SearchMovieResponse::from).toList();
        }

        List<Movie> results;
        // 카테고리에 따른 분기 처리
        switch (category.toLowerCase()) {
            case "title":
                results = movieResponseRepository.findByTitleOnly(query);
                break;
            case "person":
                results = movieResponseRepository.findByPersonOnly(query);
                break;
            case "genre":
                results = movieResponseRepository.findByGenreOnly(query);
                break;
            default: // 카테고리 미지정 시 제목 기반 검색 (이전 로직 활용 가능)
                results = movieResponseRepository.findByTitleOnly(query);
                break;
        }
        return results.stream().map(SearchMovieResponse::from).toList();
    }
}