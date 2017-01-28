package yeezus;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BoundaryHelper {
	public static int westBoundary, northBoundary, eastBoundary, southBoundary;
	private static RobotController rc;

	public static void getAndBroadcastBoundaries(RobotController rc) throws GameActionException {
		BoundaryHelper.rc = rc;
		if (westBoundary == 0) {
			int west = rc.readBroadcast(Channels.WEST_BOUNDARY);
			if (west != 0) {
				westBoundary = west;
			}
			int res = checkBoundary(Direction.WEST);
			if (res != 0) {
				rc.broadcast(Channels.WEST_BOUNDARY, res);
			}
		}
		if (northBoundary == 0) {
			int north = rc.readBroadcast(Channels.NORTH_BOUNDARY);
			if (north == 0) {
				northBoundary = north;
			}
			int res = checkBoundary(Direction.NORTH);
			if (res != 0) {
				rc.broadcast(Channels.NORTH_BOUNDARY, res);
			}
		}
		if (eastBoundary == 0) {
			int east = rc.readBroadcast(Channels.EAST_BOUNDARY);
			if (east == 0) {
				eastBoundary = east;
			}
			int res = checkBoundary(Direction.EAST);
			if (res != 0) {
				rc.broadcast(Channels.EAST_BOUNDARY, res);
			}
		}
		if (southBoundary == 0) {
			int south = rc.readBroadcast(Channels.SOUTH_BOUNDARY);
			if (south == 0) {
				southBoundary = south;
			}
			int res = checkBoundary(Direction.SOUTH);
			if (res != 0) {
				rc.broadcast(Channels.SOUTH_BOUNDARY, res);
			}
		}
	}

	private static int checkBoundary(Direction dir) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		MapLocation out = myLoc.add(dir, rc.getType().sensorRadius);
		Direction opposite = dir.opposite();
		if (rc.onTheMap(out)) {
			return 0;
		} else {
			MapLocation iter = out;
			while (!rc.onTheMap(iter)) {
				iter = iter.add(opposite);
			}
			if (dir.equals(Direction.NORTH) || dir.equals(Direction.SOUTH)) {
				return (int) iter.y;
			} else {
				return (int) iter.x;
			}
		}
	}

}
