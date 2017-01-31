package finale;

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
			if (Helper.isHostile(enemy.type)) {
				if (closestHostile == null) {
					closestHostile = enemy;
					break;
				}
			}
		}
		MapLocation dest = getClosestUnshookTree();
		System.out.println("Closest tree is : " + dest);
		MapLocation n = this.broadcastEnemy(enemies);
		if ((dest == null || closestHostile == null) && n != null) {
			dest = n;
		}
		if (dest != null) {
			if (rc.canMove(dest)) {
				rc.move(dest);
			} else {
				MapLocation toMove = this.makeSafeMove(dest, locs, closestHostile);
				System.out.println("To move is: " + toMove);
				if (toMove != null) {
					rc.move(toMove);
				}
			}
		}
		if (inDanger && !rc.hasMoved()) {
			MapLocation toMove = locs[rand.nextInt(locs.length)];
			rc.move(toMove);
		}

		if (!rc.hasMoved()) {
			System.out.println("Moving randomly");
			this.moveInUnexploredDirection(0);
		}
		attackEnemies(enemies, RobotType.GARDENER);
		shake(neutralTrees);
	}

	private float getDangerScore(MapLocation loc, RobotInfo closestHostile) {
		if (closestHostile == null) {
			return 0;
		}
		float score = 0;
		float dist = loc.distanceTo(closestHostile.location);
		if (dist <= 9) {
			score = 100 - dist;
		}
		return score;
	}

	private MapLocation makeSafeMove(MapLocation destination, MapLocation[] options, RobotInfo closestHostile)
			throws GameActionException {
		System.out.println("Options length:" + options.length);
		if (options.length == 0) {
			return null;
		}
		float bestScore = -1;
		MapLocation best = null;
		for (MapLocation next : options) {
			if (rc.canMove(next)) {
				System.out.println("Next is: " + next);
				if (best == null) {
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

	protected void moveInUnexploredDirection(int tries) throws GameActionException {
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
			makeMove(unexploredDir);
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
		for (RobotInfo enemy : enemies) {
			if ((type == null || enemy.type == type) && this.bulletPathClear(rc.getLocation(), enemy)) {
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
