package com.gamelog.service;

import com.gamelog.dto.GameRatingSummary;
import com.gamelog.dto.ReviewRequest;
import com.gamelog.dto.ReviewResponse;
import com.gamelog.exception.ResourceNotFoundException;
import com.gamelog.models.Game;
import com.gamelog.models.Review;
import com.gamelog.models.User;
import com.gamelog.repository.GameRepository;
import com.gamelog.repository.ReviewRepository;
import com.gamelog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         GameRepository gameRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> findByGame(Long gameId, Pageable pageable) {
        if (!gameRepository.existsById(gameId)) {
            throw new ResourceNotFoundException("Juego no encontrado: " + gameId);
        }
        return reviewRepository.findByGameIdOrderByCreatedAtDesc(gameId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public GameRatingSummary getSummary(Long gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new ResourceNotFoundException("Juego no encontrado: " + gameId);
        }
        long total = reviewRepository.countByGameId(gameId);
        Double avg = reviewRepository.findAverageRatingByGameId(gameId);
        double rounded = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
        return new GameRatingSummary(gameId, rounded, total);
    }

    public ReviewResponse create(Long gameId, String userEmail, ReviewRequest req) {
        validateHalfStep(req.rating());

        User user = findUserByEmail(userEmail);
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Juego no encontrado: " + gameId));

        if (reviewRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya escribiste una reseña para este juego");
        }

        Review review = new Review();
        review.setUser(user);
        review.setGame(game);
        review.setRating(req.rating());
        review.setText(req.text().trim());

        return ReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    public ReviewResponse update(Long reviewId, String userEmail, ReviewRequest req) {
        validateHalfStep(req.rating());

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada: " + reviewId));

        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar tus propias reseñas");
        }

        review.setRating(req.rating());
        review.setText(req.text().trim());

        return ReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    public void delete(Long reviewId, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada: " + reviewId));

        User currentUser = findUserByEmail(userEmail);
        boolean isOwner = review.getUser().getEmail().equals(userEmail);
        boolean isAdmin = "ADMIN".equals(currentUser.getRol());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta reseña");
        }

        reviewRepository.delete(review);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    private void validateHalfStep(Double rating) {
        if (rating == null || Math.abs(rating * 2 - Math.round(rating * 2)) > 1e-9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La calificación debe ser en incrementos de 0.5 (ej. 3.5, 4.0, 4.5)");
        }
    }
}