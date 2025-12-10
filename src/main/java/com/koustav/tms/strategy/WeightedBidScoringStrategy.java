package com.koustav.tms.strategy;

import org.springframework.stereotype.Component;

import com.koustav.tms.entity.Bid;

/**
 * Weighted scoring strategy that combines:
 * - Inverse of proposed rate (70% weight) - lower rates score higher
 * - Transporter rating normalized to 0-1 scale (30% weight)
 */
@Component
public class WeightedBidScoringStrategy implements BidScoringStrategy {

    private static final double RATE_WEIGHT = 0.7;
    private static final double RATING_WEIGHT = 0.3;
    private static final double MAX_RATING = 5.0;

    @Override
    public double calculateScore(Bid bid) {
        double rateScore = (1.0 / bid.getProposedRate()) * RATE_WEIGHT;
        double ratingScore = (bid.getTransporter().getRating() / MAX_RATING) * RATING_WEIGHT;
        return rateScore + ratingScore;
    }
}
