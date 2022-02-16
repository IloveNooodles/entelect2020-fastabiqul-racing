package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.*;

import java.util.*;

public class Bot {

  private int maxSpeed;

  private final static Command ACCELERATE = new AccelerateCommand();
  private final static Command DECELERATE = new DecelerateCommand();
  private final static Command LIZARD = new LizardCommand();
  private final static Command OIL = new OilCommand();
  private final static Command BOOST = new BoostCommand();
  private final static Command EMP = new EmpCommand();
  private final static Command FIX = new FixCommand();
  private final static Command SKIP = new DoNothingCommand();

  private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
  private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

  public Bot() {
    this.maxSpeed = 9;
  }

  public Command run(GameState gameState) {
    // YANG BELUM DICOVER
    // HUBUNGAN ANTARA MAX SPEED AMA DAMAGE -> BIAR BISA MAX SPEED TERUS DI DMG
    // TERTENTU SEBELUM AKHIRNYA FIX
    // BIKIN DETECTOR BLOCK NY JADI BISA LIAT JUMLAH BLOCKER, BUKAN CUMA SEKEDAR TAU
    // ADA ATAU GA
    // NENTUIN BEST CHOICE KALO SEMUA LANE PUNYA BLOCKER (BERHUBUNGAN SAMA POIN
    // ATAS)
    // NENTUIN LAST BLOCK BUAT NEXT ROUND (BLOCK SKRG + SPEED) ADA BLOCKER ATAU GA
    // -> BIAR USE LIZARD LEBIH MAKSIMAL0
    // UPGRADE POWERUP USAGE BIAR EFEKTIF
    Car myCar = gameState.player;
    Car opponent = gameState.opponent;
    int myCarLane = myCar.position.lane;
    int myCarBlock = myCar.position.block;
    int myCarSpeed = myCar.speed;
    int opponentLane = opponent.position.lane;
    int opponentBlock = opponent.position.block;
    int opponentSpeed = opponent.speed;
    Boolean boosting = myCar.boostCounter > 1;

    List<Object> blocksFront;
	if (myCar.boostCounter == 1) blocksFront = getBlocks(myCarLane, myCarBlock, 9, gameState);
	else blocksFront = getBlocks(myCarLane, myCarBlock, myCarSpeed, gameState);
	List<Object> blocksFrontFar = getBlocks(myCarLane, myCarBlock, 15, gameState);
    List<Object> blocksRight = getBlocks(myCarLane+1, myCarBlock, myCarSpeed, gameState);
	List<Object> blocksRightFar = getBlocks(myCarLane+1, myCarBlock, 15, gameState);
    List<Object> blocksLeft = getBlocks(myCarLane-1, myCarBlock, myCarSpeed, gameState);
	List<Object> blocksLeftFar = getBlocks(myCarLane-1, myCarBlock, 15, gameState);

	// speed ditambah biar sedikit melihat lebih jauh
	// carblock front ditambah biar block sekarang gaikut diitung
    int canRight = canMove(myCarLane+1, myCarBlock, myCarSpeed, opponent, gameState);
	int canRightFar = canMove(myCarLane+1, myCarBlock, 14, opponent, gameState);
    int canLeft = canMove(myCarLane-1, myCarBlock, myCarSpeed, opponent, gameState);
	int canLeftFar = canMove(myCarLane-1, myCarBlock, 14, opponent, gameState);
    int canForward = canMove(myCarLane, myCarBlock+1, myCarSpeed+1, opponent, gameState);
	int canForwardFar = canMove(myCarLane, myCarBlock+1, 15, opponent, gameState);
	Command bestLane = compareLane(canForward, canLeft, canRight, myCarLane);
    
    Boolean endGame = myCarBlock >= 1350;

    Boolean hasLizard = hasPowerUp(PowerUps.LIZARD, myCar.powerups);
	Boolean hasBoost = hasPowerUp(PowerUps.BOOST, myCar.powerups);
	Boolean hasEMP = hasPowerUp(PowerUps.EMP, myCar.powerups);
	Boolean hasOil = hasPowerUp(PowerUps.OIL, myCar.powerups);
	Boolean hasTweet = hasPowerUp(PowerUps.TWEET, myCar.powerups);
    int countLizard = countPowerUp(PowerUps.LIZARD, myCar.powerups);
	int countBoost = countPowerUp(PowerUps.BOOST, myCar.powerups);

    int listSpeed[] = { 9, 9, 8, 6, 3, 0 }; // from damage 0 to 5
    int listDamage[] = { 1, 1, 2, 2 }; // MUD OIL WALL CYBERTRUC
	Boolean canAccelerate = canMove(myCarLane, myCarBlock, myCarSpeed + 2, opponent, gameState) == 0 && (myCarSpeed < listSpeed[myCar.damage]);
	
	Terrain listBlocker[] = { Terrain.MUD, Terrain.WALL, Terrain.OIL_SPILL }; // MUD OIL WALL
	Boolean lastBlocked = 
	  blocksFront.get(blocksFront.size()-1) == Terrain.MUD
	  ||blocksFront.get(blocksFront.size()-1) == Terrain.WALL
	  ||blocksFront.get(blocksFront.size()-1) == Terrain.OIL_SPILL;
    
	Terrain listPowerUps[] = { Terrain.BOOST, Terrain.EMP, Terrain.LIZARD, Terrain.TWEET, Terrain.OIL_POWER };

    /* if cant move */
    if (myCar.damage >= 3){
	  if(myCarSpeed < listSpeed[myCar.damage] && canAccelerate) return ACCELERATE;
      return FIX;
    }
	if (myCarSpeed == 0) return ACCELERATE;
	/* endgame mode : prio speed but still turn right and left */
    if (endGame) {
	  /* EMP if losing in endgame */
      if ((hasEMP)
          && (opponentBlock > myCarBlock) && myCarSpeed >= listSpeed[myCar.damage] && (Math.abs(opponentLane - myCarLane) <= 1) && canForward <= 1) {
        return EMP;
      }
	  if (myCarBlock >= 1485 && hasBoost && myCar.damage == 0 && canForwardFar < 3) return BOOST;
      if ((hasBoost) && (!boosting) && (canForwardFar < 2)) {
        if (myCar.damage != 0) return FIX;
		return BOOST;
      }
	  if (myCar.damage >= 2){
	    if(myCarSpeed <= listSpeed[myCar.damage] && canAccelerate) return ACCELERATE;
        return FIX;
      } 
      if (myCarSpeed <= 3 && canAccelerate) return ACCELERATE;
	  if (containBoost(blocksFront) && canForward == 0){
		  if(myCar.damage > 0) return FIX;
		  return SKIP;
	  }
      if (containBoost(blocksRight) && canRight == 0) return TURN_RIGHT;
      if (containBoost(blocksLeft) && canLeft == 0) return TURN_LEFT;
	  
	  if (canAccelerate) return ACCELERATE;
	  /* use Powerup */
	  if (canForward < 2) {
        if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
            && (opponentBlock < myCarBlock)
            && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
          return OIL;
        }
        if ((hasTweet)
          && (myCarLane != opponentLane || opponentSpeed >= 8)) {
		  if (opponentLane == 1){
			Command TWEET = new TweetCommand(opponentLane + 1, opponentBlock + opponentSpeed + 2);
            return TWEET;
		  }
		  if (opponentLane == 4){
			Command TWEET = new TweetCommand(opponentLane - 1, opponentBlock + opponentSpeed + 2);
            return TWEET;
		  }
		  Command TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
		  return TWEET;
		}
      }
	  if (canForward > 1 && canForward == canForwardFar && hasLizard && !lastBlocked){
          return LIZARD;
	  }
	  return bestLane;
    }
	/* Boost while min speed */
	if (myCarSpeed <= 3 && hasBoost && myCar.damage == 0 && canForwardFar < 2) return BOOST;
    if (myCarSpeed <= 3 && canAccelerate) return ACCELERATE;
    /* Prio use emp if losing */
    if ((hasEMP)
        && ((opponentBlock > myCarBlock + 20) || (opponentSpeed > myCarSpeed))
        && (opponentBlock > myCarBlock) && (myCarSpeed >= 6)
        && (canForward < 2) && (Math.abs(opponentLane - myCarLane) <= 1)) {
      return EMP;
    }
    /* boost number 1 */
    if ((hasBoost)
        && (myCarSpeed <= 8 || countBoost > 2)
        && (canForwardFar < 2)) {
      if (myCar.damage != 0) {
        return FIX;
      }
      return BOOST;
    }
    /* Prio take boost */
    if (containBoost(blocksFront)) {
      if (canForward < 2) {
        if (canAccelerate) {
          return ACCELERATE;
        }
        if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
            && (opponentBlock < myCarBlock)
            && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
          return OIL;
        }
        if ((hasTweet)
          && (myCarLane != opponentLane || opponentSpeed >= 8)) {
		  if (opponentLane == 1){
			Command TWEET = new TweetCommand(opponentLane + 1, opponentBlock + opponentSpeed + 2);
            return TWEET;
		  }
		  if (opponentLane == 4){
			Command TWEET = new TweetCommand(opponentLane - 1, opponentBlock + opponentSpeed + 2);
            return TWEET;
		  }
          Command TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          return TWEET;
		}
		if(myCar.damage > 0) return FIX;
        return SKIP;
      }
	  if ((hasLizard) && (!lastBlocked)
        && (countLizard >= 3)) {
        return LIZARD;
	  }
	  if (canForward <= 2 && lastBlocked) return DECELERATE;
    }
	if (containBoost(blocksRight) && canRight < canLeft) {
      return TURN_RIGHT;
    }
    if (containBoost(blocksLeft) && canLeft < 2) {
      return TURN_LEFT;
    }
    /* Prio take emp */
    if (containEmp(blocksFront)) {
      if (canForward < 2) {
        if (canAccelerate) {
          return ACCELERATE;
        }
        if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
            && (opponentBlock < myCarBlock)
            && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
          return OIL;
        }
        if ((hasTweet)
          && (myCarLane != opponentLane || opponentSpeed >= 8)) {
		  if (opponentLane == 1){
			Command TWEET = new TweetCommand(opponentLane + 1, opponentBlock + opponentSpeed + 2);
            return TWEET;
		  }
		  if (opponentLane == 4){
			Command TWEET = new TweetCommand(opponentLane - 1, opponentBlock + opponentSpeed + 2);
            return TWEET;
		  }
          Command TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          return TWEET;
		}
		if(myCar.damage > 0) return FIX;
        return SKIP;
      }
	  if ((hasLizard) && (!lastBlocked)
        && (countLizard >= 3)) {
		return LIZARD;
	  }
	  if (canForward <= 2 && lastBlocked) return DECELERATE;
    }
	if (containEmp(blocksRight) && canRight < canLeft) {
      return TURN_RIGHT;
    }
    if (containEmp(blocksLeft) && canLeft < 2) {
      return TURN_LEFT;
    }
	/* Lane prio, harus di tengah */
    if (myCarLane == 1 && canRight <= canForward) {
      return TURN_RIGHT;
    }
    if (myCarLane == 4 && canLeft <= canForward) {
      return TURN_LEFT;
    }
	/* far Lane prio, harus di tengah */
    if (myCarLane == 1 && canRightFar <= canForwardFar) {
      return TURN_RIGHT;
    }
    if (myCarLane == 4 && canLeftFar <= canForwardFar) {
      return TURN_LEFT;
    }
	/* Prio speeed */
    if (canAccelerate) {
      return ACCELERATE;
    }
	/* while fullspeed and no-blocker*/
	/* use powerup */
	if (canForward < 2) {
      if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
          && (opponentBlock < myCarBlock)
          && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))){
        return OIL;
      }
      if ((hasTweet)
          && (myCarLane != opponentLane || opponentSpeed >= 8)) {
		if (opponentLane == 1){
		  Command TWEET = new TweetCommand(opponentLane + 1, opponentBlock + opponentSpeed + 2);
          return TWEET;
		}
		if (opponentLane == 4){
		  Command TWEET = new TweetCommand(opponentLane - 1, opponentBlock + opponentSpeed + 2);
          return TWEET;
		}
        Command TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
        return TWEET;
      }
    }
	/* Prio pickup powerup */
    if (containPowerUps(blocksFront)) {
	  if ((hasLizard) && (!lastBlocked)
        && (countLizard >= 3)) {
		return LIZARD;
	  }
    }
	if (containPowerUps(blocksRight) && canRight < canLeft) {
      return TURN_RIGHT;
    }
    if (containPowerUps(blocksLeft) && canLeft < 2) {
      return TURN_LEFT;
    }
	/* fix / do nothing */
	if (canForward < 2) {
	  if(myCar.damage > 0) return FIX;
      return SKIP;
	}
	if (canForward >= 2) return bestLane;
	/* if cant do anything */
	if (hasLizard && !lastBlocked) {
      return LIZARD;
    }
	if (myCarSpeed >= 6 && canMove(myCarLane, myCarBlock+1, myCarSpeed-2, opponent, gameState) < 2){
		return DECELERATE;
	}
	/* Prio take powerup on far side */
	if (containBoost(blocksRightFar) && canRightFar < canLeftFar) {
      return TURN_RIGHT;
    }
    if (containBoost(blocksLeftFar) && canLeftFar < 2) {
      return TURN_LEFT;
    }
	if (containEmp(blocksRightFar) && canRightFar < canLeftFar) {
      return TURN_RIGHT;
    }
    if (containEmp(blocksLeftFar) && canLeftFar < 2) {
      return TURN_LEFT;
    }
    if (containPowerUps(blocksRightFar) && canRightFar < canLeftFar) {
      return TURN_RIGHT;
    }
    if (containPowerUps(blocksLeftFar) && canLeftFar < 2) {
      return TURN_LEFT;
    }
    return bestLane;
  }

  private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
    for (PowerUps powerUp : available) {
      if (powerUp.equals(powerUpToCheck)) {
        return true;
      }
    }
    return false;
  }

  private int countPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
    int sum = 0;
    for (PowerUps powerUp : available) {
      if (powerUp.equals(powerUpToCheck)) {
        sum++;
      }
    }
    return sum;
  }

  private List<Object> getBlocks(int lane, int block, int speed, GameState gameState) {
    List<Object> blocks = new ArrayList<>();
    if (lane > 0 && lane <= 4) {
      List<Lane[]> map = gameState.lanes;
      Lane[] laneList = map.get(lane - 1);
      int startBlock = map.get(0)[0].position.block;
      int tmp = block - startBlock;
      for (int i = tmp; i <= tmp + speed; i++) {
        if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
          break;
        }
        blocks.add(laneList[i].terrain);
      }
    }
    return blocks;
  }

  private int canMove(int lane, int block, int speed, Car opponent, GameState gameState) {
    int myCarLane = lane;
    int myCarBlock = block;
    int myCarSpeed = speed;
    int opponentLane = opponent.position.lane;
    int opponentBlock = opponent.position.block;
    int opponentSpeed = opponent.speed;

    if (lane < 1 || lane > 4) {
      return 99;
    }
    int sum = 0;
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    Lane[] laneList = map.get(lane - 1);
    int tmp = myCarBlock - startBlock;
    int i = tmp;
    for (i = tmp; i <= tmp + myCarSpeed; i++) {
      if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
        break;
      }
	  if (laneList[i].occupiedByPlayerId == opponent.id) sum++;
      if ((laneList[i].terrain == Terrain.MUD)
          || (laneList[i].terrain == Terrain.OIL_SPILL)){
        sum += 2;
      }
	  if (laneList[i].terrain == Terrain.WALL) sum += 3;
	  if (laneList[i].isOccupiedByCyberTruck == true) sum += 5; //karena ngestuck > 2 wall dsb
	}
    return sum;
  }

  private Boolean containPowerUps(List<Object> blocks) {
    if ((blocks.contains(Terrain.BOOST))
        || (blocks.contains(Terrain.LIZARD))
        || (blocks.contains(Terrain.TWEET))
        || (blocks.contains(Terrain.EMP))
        || (blocks.contains(Terrain.OIL_POWER))) {
      return true;
    } else {
      return false;
    }
  }

  private Boolean containBoost(List<Object> blocks) {
    if (blocks.contains(Terrain.BOOST)) {
      return true;
    } else {
      return false;
    }
  }

  private Boolean containEmp(List<Object> blocks) {
    if (blocks.contains(Terrain.EMP)) {
      return true;
    } else {
      return false;
    }
  }
  private Command compareLane(int forward, int left, int right, int myCarLane){
	  if(forward <= left && forward <= right) return ACCELERATE;
	  if(left == right){
		if(myCarLane <= 2) return TURN_RIGHT;
		if(myCarLane > 2) return TURN_LEFT;
	  }
	  if(left < right) return TURN_LEFT;
	  else return TURN_RIGHT;
  }
  /*private Command Tweet(Car opponent, int left, int forward, int right){
	  int opponentLane = opponent.position.lane;
	  int block = opponent.position.block;
	  int speed = opponent.speed;
	  int tweetblock = block + speed + 2;
	  if(left != -1){
		  if(right != -1){
			if(forward <= left && forward <= right){
				Command TWEET = new TweetCommand(opponentLane, tweetblock);
				return TWEET;
			}
			if(left == right){
				if(opponentLane <= 2){
					Command TWEET = new TweetCommand(opponentLane + 1, tweetblock);
					return TWEET;
				}
				if(opponentLane > 2){
					Command TWEET = new TweetCommand(opponentLane - 1, tweetblock);
					return TWEET;
				}
			}
			if(left < right){
				Command TWEET = new TweetCommand(opponentLane - 1, tweetblock);
				return TWEET;
			}
			else{
				Command TWEET = new TweetCommand(opponentLane + 1, tweetblock);
				return TWEET;
			}
		  }
		  else{
			if(forward <= left){
				Command TWEET = new TweetCommand(opponentLane, tweetblock);
				return TWEET;
			}
			else{
				Command TWEET = new TweetCommand(opponentLane - 1, tweetblock);
				return TWEET;
			}
		  }
	  }
	  else{
		  if(forward <= right){
			  Command TWEET = new TweetCommand(opponentLane, tweetblock);
			  return TWEET;
		  }
		  else{
			  Command TWEET = new TweetCommand(opponentLane + 1, tweetblock);
			  return TWEET;
		  }
	  }
  }*/
}