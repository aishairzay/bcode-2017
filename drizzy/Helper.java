package drizzy;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Helper {

	public static boolean isHostile(RobotType type) {
		return type == RobotType.LUMBERJACK || type == RobotType.SCOUT || type == RobotType.SOLDIER
				|| type == RobotType.TANK;
	}

	public static void broadcastLocation(int channel, RobotController rc, MapLocation loc) throws GameActionException {
		int x = (int) loc.x;
		int y = (int) loc.y;
		System.out.println("Broadcasted x: " + x);
		System.out.println("Broadcasted y: " + y);
		rc.broadcast(channel, x);
		rc.broadcast(channel + 1, y);
	}

	public static MapLocation getLocation(RobotController rc, int channel) throws GameActionException {
		int x = rc.readBroadcast(channel);
		if (x == 0) {
			return null;
		}
		int y = rc.readBroadcast(channel + 1);
		System.out.println("Got x: " + x);
		System.out.println("Got y: " + y);
		return new MapLocation((float) x, (float) y);
	}

}
