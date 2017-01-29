package drizzy;

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
				if (rc.getRoundNum() >= rc.getRoundLimit() - 200) {
					int toDonate = (int) ((rc.getTeamBullets() - 10) / rc.getVictoryPointCost());
					if (toDonate >= 0) {
						rc.donate(toDonate * rc.getVictoryPointCost());
					}
				}
				float teamBullets = rc.getTeamBullets();
				float victoryPointsAvailable = teamBullets / rc.getVictoryPointCost();
				boolean canWin = rc.getTeamVictoryPoints()
						+ victoryPointsAvailable > GameConstants.VICTORY_POINTS_TO_WIN;
				if ((currentRound >= rc.getRoundLimit() - 1 || canWin) && teamBullets >= rc.getVictoryPointCost()) {
					rc.donate(teamBullets);
				}
				try {
					robot.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
				int endRound = rc.getRoundNum();
				if (endRound > currentRound) {
					// System.out.println("This bot went above its bytecode
					// limits!");
				}
				// System.out.println("have this many bytecodes left: " +
				// Clock.getBytecodesLeft());
				Clock.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
