package PrimarySoldier;

import java.util.Random;

import battlecode.common.*;

public class Movement {

	private RobotController rc;
	private MapLocation dest;

	private MapLocation myLoc;
	private boolean onWall;
	private Direction curDir;
	private int rotations;
	private boolean rotateLeft;
	private int recursionCount;
	private int shouldReset;

	public Movement(RobotController rc) {
		this.rc = rc;
	}

	public boolean isOnWall() {
		return onWall;
	}

	public boolean move() throws GameActionException {
		myLoc = rc.getLocation();
		return bug();
	}

	public void setDestination(MapLocation destination) {
		myLoc = rc.getLocation();
		System.out.println("Cur dir is: " + curDir);
		recursionCount = 0;
		if (destination == null) {
			return;
		}
		if (dest == null) {
			reset(destination);
			return;
		}
		if (dest.equals(destination)) {
			dest = destination;
			myLoc = rc.getLocation();
		}
		reset(destination);
	}

	public void reset(MapLocation destination) {
		this.dest = destination;
		onWall = false;
		curDir = myLoc.directionTo(dest);
		rotations = 0;
		rotateLeft = new Random(rc.getID() ^ rc.getRoundNum()).nextBoolean();
		shouldReset = 0;
	}

	private boolean bug() throws GameActionException {
		if (dest == null) {
			return false;
		}
		if (rc.hasMoved()) {
			return false;
		}
		if (shouldReset >= 4) {
			reset(dest);
		}
		if (!onWall) {
			curDir = myLoc.directionTo(dest);
			MapLocation next = myLoc.add(curDir, rc.getType().strideRadius);
			if (rc.canMove(next)) {
				rc.move(curDir);
				return true;
			} else {
				rotate();
				onWall = true;
				return bug();
			}
		}
		if (rotations % (360 / 30) == 0) {
			rotations = 0;
			onWall = false;
			return false;
		}

		if (onWall) {
			Direction prevTurn = rotateOtherWay();
			if (rc.senseRobotAtLocation(myLoc.add(prevTurn)) != null) {
				shouldReset++;
			}
			if (rc.canMove(prevTurn, rc.getType().strideRadius)) {
				rotateBack();
				return recursiveBug();
			} else if (!rc.onTheMap(myLoc.add(prevTurn, rc.getType().strideRadius))) {
				onWall = false;
				rotateLeft = !rotateLeft;
				rotations = 0;
				return false;
			} else {
				MapLocation next = myLoc.add(curDir);
				if (rc.canMove(next)) {
					rc.move(next);
				} else {
					if (rc.senseRobotAtLocation(next) != null) {
						shouldReset++;
					}
					rotate();
					return recursiveBug();
				}
			}
		}
		return false;
	}

	private boolean recursiveBug() throws GameActionException {
		recursionCount++;
		if (recursionCount >= 8) {
			return false;
		}
		return bug();
	}

	private void rotate() {
		if (rotateLeft) {
			curDir = curDir.rotateLeftDegrees(30);
		} else {
			curDir = curDir.rotateRightDegrees(30);
		}
	}

	private void rotateBack() {
		if (rotateLeft) {
			curDir = curDir.rotateRightDegrees(30);
		} else {
			curDir = curDir.rotateLeftDegrees(30);
		}
	}

	private Direction rotateOtherWay() {
		if (rotateLeft) {
			return curDir.rotateRightDegrees(30);
		} else {
			return curDir.rotateLeftDegrees(30);
		}
	}
}
