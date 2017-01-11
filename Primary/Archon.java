package Primary;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public strictfp class Archon extends Bot{

	public Archon(RobotController rc) throws GameActionException {
		super(rc);
		rc.donate(GameConstants.BULLET_EXCHANGE_RATE);
		System.out.println("Here we are");;
	}
	
	public void run() throws GameActionException{
		System.out.println("Running archon now");
		build();
		//hireGardner();
		//move();
		//shake();
	}
	
	private void hireGardner() throws GameActionException {
		//rc.hireGardener(Direction.getNorth());
		for (Direction dir: directions) {
			if (rc.canHireGardener(dir)) {
				rc.hireGardener(dir);
			}
		}
	}
	
	private void build() throws GameActionException {
		if (rc.getRoundNum() <= 1) {
			hireGardner();
		} else if ((rc.getRoundNum() % Constants.GARDNER_PING_RATE) + 5 == rc.getRoundNum()){
			int gardnerCount = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, -1);
			if (gardnerCount < 3 || rc.getTeamBullets() > 500) {
				hireGardner();
			}
		}
	}
	
	private void move() {
		
	}

}
