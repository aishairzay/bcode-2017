package infra2;

import battlecode.common.RobotType;

public class Helper {

	public static boolean isHostile(RobotType type) {
		return type == RobotType.LUMBERJACK || type == RobotType.SCOUT || type == RobotType.SOLDIER
				|| type == RobotType.TANK;
	}

}
