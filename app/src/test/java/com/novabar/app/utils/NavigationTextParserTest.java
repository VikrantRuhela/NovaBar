package com.novabar.app.utils;

import com.novabar.app.domain.ManeuverType;
import org.junit.Assert;
import org.junit.Test;

public class NavigationTextParserTest {

    @Test
    public void testRightTurnManeuvers() {
        Assert.assertEquals(ManeuverType.RIGHT, NavigationTextParser.INSTANCE.parse("Turn right", "onto Main St"));
        Assert.assertEquals(ManeuverType.RIGHT, NavigationTextParser.INSTANCE.parse("Right turn", ""));
        Assert.assertEquals(ManeuverType.KEEP_RIGHT, NavigationTextParser.INSTANCE.parse("Keep right", "at fork"));
        Assert.assertEquals(ManeuverType.SLIGHT_RIGHT, NavigationTextParser.INSTANCE.parse("Slight right", "on Broadway"));
        Assert.assertEquals(ManeuverType.SHARP_RIGHT, NavigationTextParser.INSTANCE.parse("Sharp right", ""));
    }

    @Test
    public void testLeftTurnManeuvers() {
        Assert.assertEquals(ManeuverType.LEFT, NavigationTextParser.INSTANCE.parse("Turn left", "onto Main St"));
        Assert.assertEquals(ManeuverType.KEEP_LEFT, NavigationTextParser.INSTANCE.parse("Keep left", "at fork"));
        Assert.assertEquals(ManeuverType.SLIGHT_LEFT, NavigationTextParser.INSTANCE.parse("Slight left", "on Broadway"));
        Assert.assertEquals(ManeuverType.SHARP_LEFT, NavigationTextParser.INSTANCE.parse("Sharp left", ""));
    }

    @Test
    public void testForkAndRampManeuvers() {
        Assert.assertEquals(ManeuverType.FORK_LEFT, NavigationTextParser.INSTANCE.parse("Fork left", ""));
        Assert.assertEquals(ManeuverType.FORK_RIGHT, NavigationTextParser.INSTANCE.parse("Fork right", ""));
        Assert.assertEquals(ManeuverType.RAMP_LEFT, NavigationTextParser.INSTANCE.parse("Ramp left", "on exit"));
        Assert.assertEquals(ManeuverType.RAMP_RIGHT, NavigationTextParser.INSTANCE.parse("Take ramp right", ""));
    }

    @Test
    public void testStraightManeuvers() {
        Assert.assertEquals(ManeuverType.STRAIGHT, NavigationTextParser.INSTANCE.parse("Continue straight", "on I-95"));
    }

    @Test
    public void testDestinationManeuvers() {
        Assert.assertEquals(ManeuverType.DESTINATION, NavigationTextParser.INSTANCE.parse("You have arrived", "at your destination"));
        Assert.assertEquals(ManeuverType.DESTINATION, NavigationTextParser.INSTANCE.parse("Arrive at destination", ""));
    }

    @Test
    public void testUturnManeuvers() {
        Assert.assertEquals(ManeuverType.UTURN, NavigationTextParser.INSTANCE.parse("Make a U-turn", ""));
        Assert.assertEquals(ManeuverType.UTURN, NavigationTextParser.INSTANCE.parse("U turn ahead", ""));
    }

    @Test
    public void testRoundaboutManeuvers() {
        Assert.assertEquals(ManeuverType.ROUNDABOUT_EXIT, NavigationTextParser.INSTANCE.parse("At the roundabout, take the 2nd exit", ""));
        Assert.assertEquals(ManeuverType.ROUNDABOUT_ENTER, NavigationTextParser.INSTANCE.parse("Enter roundabout", ""));
    }

    @Test
    public void testWordBoundaryCollisions() {
        // Wright contains "right" but is not bounded as "right" - should be straight because of continue straight
        Assert.assertEquals(ManeuverType.STRAIGHT, NavigationTextParser.INSTANCE.parse("Continue straight", "onto Wright Ave"));
        
        // Cartwright contains "right" but is not bounded as "right" - should be straight
        Assert.assertEquals(ManeuverType.STRAIGHT, NavigationTextParser.INSTANCE.parse("Continue straight", "onto Cartwright Rd"));
        
        // Straight St contains "straight" but Turn Right is explicit and checked first
        Assert.assertEquals(ManeuverType.RIGHT, NavigationTextParser.INSTANCE.parse("Turn right", "onto Straight St"));
    }

    @Test
    public void testUnknownFallback() {
        Assert.assertEquals(ManeuverType.UNKNOWN, NavigationTextParser.INSTANCE.parse("200 m", "Main St"));
    }
}
