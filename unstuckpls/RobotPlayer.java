package unstuckpls;

import battlecode.common.*;

public strictfp class RobotPlayer {
	private static float lastRoundBullets;
	private static boolean farming;
	private static int roundsSameBullets;

	public static void run(RobotController rc) throws GameActionException {
		try {
			Bot robot = null;
			roundsSameBullets = 0;
			lastRoundBullets = 0;
			farming = false;
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
				float bullets = rc.getTeamBullets();
				if (bullets == lastRoundBullets) {
					roundsSameBullets++;
				} else {
					roundsSameBullets = 0;
				}
				lastRoundBullets = (int) bullets;

				if (roundsSameBullets >= 40) {
					farming = true;
				}

				int currentRound = rc.getRoundNum();
				if (farming || (rc.getRoundNum() >= rc.getRoundLimit() - 200
						&& (rc.getTeamBullets() / rc.getVictoryPointCost()) <= rc.getOpponentVictoryPoints())) {
					int toDonate = (int) ((rc.getTeamBullets() - 10) / rc.getVictoryPointCost());
					if (toDonate > 0) {
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

				if (rc.getRoundNum() >= 400 && teamBullets > 800) {
					teamBullets -= 800;
					int toDonate = (int) (teamBullets / rc.getVictoryPointCost());
					if (toDonate > 0) {
						float donation = toDonate * rc.getVictoryPointCost();
						rc.donate(donation);
					}
				}

				try {
					robot.run();
				} catch (Exception e) {
					e.printStackTrace();
				}

				Clock.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
