package Primary;

import battlecode.common.*;

public strictfp class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
		Bot robot = null;
		if (rc.getType() == RobotType.ARCHON) {
			robot = new Archon(rc);
		} else if (rc.getType() == RobotType.GARDENER) {
			robot = new Gardener(rc);
		} else if (rc.getType() == RobotType.LUMBERJACK) {
			robot = new Lumberjack(rc);
		} else if (rc.getType() == RobotType.SCOUT) {
			robot = new Scout(rc);
		} else if (rc.getType() == RobotType.SOLDIER) {
			robot = new Soldier(rc);
		} else if (rc.getType() == RobotType.TANK) {
			robot = new Tank(rc);
		}
		while (true) {
			int currentRound = rc.getRoundNum();
			float teamBullets = rc.getTeamBullets();
			if (currentRound >= rc.getRoundLimit() - 1 || teamBullets >= (GameConstants.VICTORY_POINTS_TO_WIN * GameConstants.BULLET_EXCHANGE_RATE)) {
				rc.donate(teamBullets);
			}
			try {
				robot.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			int endRound = rc.getRoundNum();
			if (endRound > currentRound) {
				System.out.println("This bot went above its bytecode limits!");
			}
			Clock.yield();
		}
	}
}
