package Fourth;

import battlecode.common.*;

public strictfp class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
		try {
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

				float victoryPointsAvailable = teamBullets / 10;
				float victoryPointsNeeded = GameConstants.VICTORY_POINTS_TO_WIN - victoryPointsAvailable;
				boolean canWin = rc.getTeamVictoryPoints() > victoryPointsNeeded;
				if (currentRound >= rc.getRoundLimit() - 1 || canWin) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
