package unstuckpls;

import battlecode.common.*;

public strictfp class Scout extends Bot {

	boolean[] visitedArchonIndexes;
	MapLocation enemyLocation;

	public Scout(RobotController rc) {
		super(rc);
		visitedArchonIndexes = new boolean[allyArchons.length];
	}

	@Override
	public void run() throws GameActionException {
		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		BulletInfo[] bullets = rc.senseNearbyBullets(myType.bulletSightRadius);

		MapLocation[] locs = this.getSafestLocs(bullets, enemies, 7000);
		if (locs.length == 0) {
			locs = new MapLocation[directions.length];
			int i = 0;
			for (Direction dir : directions) {
				locs[i] = rc.getLocation().add(dir);
				i++;
			}
		}

		RobotInfo closestHostile = null;
		for (RobotInfo enemy : enemies) {
			if (Helper.isHostile(enemy.type) && enemy.type != RobotType.SCOUT) {
				if (closestHostile == null) {
					closestHostile = enemy;
					break;
				}
			}
		}
		MapLocation dest = getClosestUnshookTree();
		MapLocation n = this.broadcastEnemy(enemies);
		if ((dest == null || closestHostile == null) && n != null) {
			dest = n;
		}
		if (dest != null) {
			MapLocation toMove = this.makeSafeMove(dest, locs, closestHostile);
			if (toMove != null) {
				rc.move(toMove);
			}
		}
		if (inDanger && !rc.hasMoved()) {
			MapLocation toMove = locs[rand.nextInt(locs.length)];
			rc.move(toMove);
		}

		if (!rc.hasMoved()) {
			this.moveInUnexploredDirection(0, locs, closestHostile);
		}
		attackEnemies(enemies, RobotType.GARDENER);
		attackEnemies(enemies, RobotType.SCOUT);
		shake(neutralTrees);
	}

	private float getDangerScore(MapLocation loc, RobotInfo closestHostile) {
		if (closestHostile == null) {
			return 0;
		}
		float score = 0;
		float dist = loc.distanceTo(closestHostile.location);
		if (dist <= 10) {
			score = 100 - dist;
		}
		return score;
	}

	private MapLocation makeSafeMove(MapLocation destination, MapLocation[] options, RobotInfo closestHostile)
			throws GameActionException {
		if (options.length == 0) {
			return null;
		}
		float bestScore = -1;
		if (rc.canMove(destination) && getDangerScore(destination, closestHostile) <= 10) {
			return destination;
		}
		MapLocation best = null;
		for (MapLocation next : options) {
			if (rc.canMove(next)) {
				if (best == null || bestScore == -1) {
					bestScore = getDangerScore(next, closestHostile);
					best = next;
					continue;
				}
				float score = getDangerScore(next, closestHostile);
				if (score < bestScore) {
					bestScore = score;
					best = next;
				} else if (score == bestScore) {
					if (next.distanceTo(destination) < best.distanceTo(destination)) {
						bestScore = score;
						best = next;
					}
				}
			}
		}
		return best;
	}

	protected void moveInUnexploredDirection(int tries, MapLocation[] locs, RobotInfo closestHostile)
			throws GameActionException {
		if (tries == 8) {
			return;
		}
		if (unexploredDir == null) {
			unexploredDir = getRandomDirection();
			if (unexploredDir == Direction.getEast() || unexploredDir == Direction.getNorth()
					|| unexploredDir == Direction.getEast() || unexploredDir == Direction.getSouth()) {
				unexploredDir = getRandomDirection();
			}
		}
		if (rc.canMove(unexploredDir) || !rc.onTheMap(rc.getLocation().add(unexploredDir))) {
			this.makeMove(unexploredDir);
		} else {
			unexploredDir = this.getRandomDirection();
			moveInUnexploredDirection(tries + 1);
		}
	}

	protected MapLocation getClosestUnshookTree() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		for (TreeInfo tree : trees) {
			if (tree.team != Team.NEUTRAL || tree.containedBullets < 1) {
				continue;
			}
			return tree.location;
		}
		return null;
	}

	private void attackEnemies(RobotInfo[] enemies, RobotType type) throws GameActionException {
		RobotInfo closestEnemy = null;
		if (!rc.canFireSingleShot()) {
			return;
		}
		if (Clock.getBytecodesLeft() <= 2000) {
			return;
		}
		for (RobotInfo enemy : enemies) {
			if ((type == null || enemy.type == type) && this.bulletPathClear(rc.getLocation(), enemy)) {
				if (enemy.type != RobotType.GARDENER) {
					if (rc.getLocation().distanceTo(enemy.location) >= 3) {
						break;
					}
				}
				closestEnemy = enemy;
				break;
			}
		}
		if (closestEnemy != null) {
			Direction dir = rc.getLocation().directionTo(closestEnemy.location);
			rc.fireSingleShot(dir);
		}
	}

	void move(Direction dir) throws GameActionException {
		rc.move(dir);
	}

}
