package com.koustav.tms.strategy;

import com.koustav.tms.entity.Bid;

/**
 * Strategy interface for calculating bid scores.
 * Different implementations can provide different scoring algorithms.
 */
public interface BidScoringStrategy {

    /**
     * Calculate a score for the given bid.
     * Higher scores indicate better bids.
     *
     * @param bid the bid to score
     * @return the calculated score
     */
    double calculateScore(Bid bid);
}
