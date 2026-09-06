package io.github.r3neer.lodestonetransit;

import io.github.r3neer.lodestonetransit.teleport.SafeLandingFinder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LandingScoreTest {
    @Test void fullPlatformBeatsEdgeAtSameDistance() {
        assertTrue(SafeLandingFinder.score(1, .5, 2, false) > SafeLandingFinder.score(.1, .5, 2, false));
    }
    @Test void openRoomBeatsExactFit() {
        assertTrue(SafeLandingFinder.score(1, 1, 2, false) > SafeLandingFinder.score(1, 0, 2, false));
    }
    @Test void safeFartherArrivalBeatsCloseLava() {
        assertTrue(SafeLandingFinder.score(.5, .5, 5, false) > SafeLandingFinder.score(1, 1, 0, true));
    }
    @Test void distanceContributionMatchesSpecification() {
        for (int distance=0;distance<=5;distance++) assertEquals(30-6*distance,SafeLandingFinder.score(0,0,distance,false),1e-10);
    }
}
