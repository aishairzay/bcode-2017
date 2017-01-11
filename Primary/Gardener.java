package Primary;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public strictfp class Gardener extends Bot{

	public Gardener(RobotController rc) {
		super(rc);

	}

	@Override
	public void run() throws GameActionException {
		ping();
		build();
		move();
	}
	
	private void build() throws GameActionException {
		if(rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
			for (Direction dir: directions) {
				if (rc.canBuildRobot(RobotType.SCOUT, dir)) {
					rc.buildRobot(RobotType.SCOUT, dir);
					break;
				}
			}
		}
	}
	
	private void move() {
		
	}
	
	// every 40 rounds send a ping, so that a
	// friendly archon knows how many gardeners are current alive.
	private void ping() throws GameActionException {
		if (rc.getRoundNum() % Constants.GARDNER_PING_RATE != 0) {
			return;
		}
		int currentPing = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
		if (currentPing <= 0) {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, 1);
		} else {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, currentPing + 1);
		}
	}
}
