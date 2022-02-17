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
  
  private static Car myCar;
  private static Car opponent;
  private static int myCarLane;
  private static int myCarBlock;
  private static int myCarSpeed;
  private static int opponentLane;
  private static int opponentBlock;
  private static int opponentSpeed;
  private static List<Lane[]> map;
  private static int startBlock;
  private static int currentBlock;
  private static Boolean boosting;
  private static Command TWEET;

  private static List<Object> blocksFront;
  private static List<Object> blocksFrontFar;
  private static List<Object> blocksRight;
  private static List<Object> blocksRightFar;
  private static List<Object> blocksLeft;
  private static List<Object> blocksLeftFar;

  private static int canRight;
  private static int canRightFar;
  private static int canLeft;
  private static int canLeftFar;
  private static int canForward;
  private static int canForwardFar;
  private static Command bestLane;

  private static Boolean endGame;

  private static Boolean hasLizard;
  private static Boolean hasBoost;
  private static Boolean hasEMP;
  private static Boolean hasOil;
  private static Boolean hasTweet;
  private static int countLizard;
  private static int countBoost;
  private static int countOil;
  private static int countTweet;

  private static int idxBlock;
  private static Boolean lastBlocked;

  private static final int listSpeed[] = { 9, 9, 8, 6, 3, 0 }; // from damage 0 to 5
  private static Boolean canAccelerate;  


  public Bot() {
    this.maxSpeed = 9;
  }

  public Command run(GameState gameState) {
	// Initialize
    myCar = gameState.player;
    opponent = gameState.opponent;
    myCarLane = myCar.position.lane;
    myCarBlock = myCar.position.block;
    myCarSpeed = myCar.speed;
    opponentLane = opponent.position.lane;
    opponentBlock = opponent.position.block;
    opponentSpeed = opponent.speed;
    map = gameState.lanes;
    startBlock = map.get(0)[0].position.block;
    currentBlock = myCarBlock - startBlock;
    boosting = myCar.boosting;
	
	// Lane decision initialize
    blocksFront = getBlocks(myCarLane, myCarBlock + 1, myCarSpeed, gameState);
    blocksFrontFar = getBlocks(myCarLane, myCarBlock + 1, 15, gameState);
    blocksRight = getBlocks(myCarLane + 1, myCarBlock, myCarSpeed, gameState);
    blocksRightFar = getBlocks(myCarLane + 1, myCarBlock, 15, gameState);
    blocksLeft = getBlocks(myCarLane - 1, myCarBlock, myCarSpeed, gameState);
    blocksLeftFar = getBlocks(myCarLane - 1, myCarBlock, 15, gameState);

    canRight = canMove(myCarLane + 1, myCarBlock, myCarSpeed, opponent, gameState);
    canRightFar = canMove(myCarLane + 1, myCarBlock, 15, opponent, gameState);
    canLeft = canMove(myCarLane - 1, myCarBlock, myCarSpeed, opponent, gameState);
    canLeftFar = canMove(myCarLane - 1, myCarBlock, 15, opponent, gameState);
    canForward = canMove(myCarLane, myCarBlock + 1, myCarSpeed, opponent, gameState);
    canForwardFar = canMove(myCarLane, myCarBlock + 1, 15, opponent, gameState);
    bestLane = compareLane(canForward, canLeft, canRight, myCarLane);
	
    canAccelerate = canMove(myCarLane, myCarBlock, myCarSpeed + 2, opponent, gameState) == 0
        && (myCarSpeed < listSpeed[myCar.damage]);
	
	// Endgame
    endGame = myCarBlock >= 1350;
	
	// PowerUp initialize
    hasLizard = hasPowerUp(PowerUps.LIZARD, myCar.powerups);
    hasBoost = hasPowerUp(PowerUps.BOOST, myCar.powerups);
    hasEMP = hasPowerUp(PowerUps.EMP, myCar.powerups);
    hasOil = hasPowerUp(PowerUps.OIL, myCar.powerups);
    hasTweet = hasPowerUp(PowerUps.TWEET, myCar.powerups);
    countLizard = countPowerUp(PowerUps.LIZARD, myCar.powerups);
    countBoost = countPowerUp(PowerUps.BOOST, myCar.powerups);
    countOil = countPowerUp(PowerUps.OIL, myCar.powerups);
    countTweet = countPowerUp(PowerUps.TWEET, myCar.powerups);

    // Index handling
    if (myCarBlock + myCarSpeed > 1500) {
      idxBlock = 1500 - startBlock;
    } else {
      idxBlock = currentBlock + myCarSpeed;
    }
    
	// Check whether nextBlock is blocked or not
    lastBlocked = false;
    if (blocksFront.size() > 0) {
      lastBlocked = blocksFront.get(blocksFront.size() - 1) == Terrain.MUD
          || blocksFront.get(blocksFront.size() - 1) == Terrain.WALL
          || blocksFront.get(blocksFront.size() - 1) == Terrain.OIL_SPILL
          || map.get(myCarLane - 1)[idxBlock - 1].isOccupiedByCyberTruck == true;
    }
	
	// MAIN
	// LastMove handle
	if (myCarBlock+myCarSpeed >= 1500){
	  if (hasBoost && myCar.damage == 0 && canForward <= 1){
		return BOOST;
	  }
	  Command moveForward = CAN_FORWARD();
	  if (moveForward != null){
	    return moveForward;
	  }
	  if (hasLizard && !lastBlocked){
		return LIZARD;
	  }
	  if (canLeft == 0){
		  return TURN_LEFT;
	  }
	  if (canRight == 0){
		  return TURN_RIGHT;
	  }
	}
    // Fix worst condition
    if (myCar.damage >= 3) {
      if (myCarSpeed < listSpeed[myCar.damage] && canAccelerate)
        return ACCELERATE;
      return FIX;
    }
    if (myCarSpeed == 0){
      return ACCELERATE;
	}

    // Endgame mode : prio speed but still turn right and left
    if (endGame) {
      return ENDGAME();
    }

    // Boost while min speed
    if (myCarSpeed <= 3 && hasBoost && myCar.damage == 0 && canForwardFar == 0)
      return BOOST;
    if (myCarSpeed <= 5 && canAccelerate)
      return ACCELERATE;

    // Prio use emp if losing
    if ((hasEMP)
        && ((opponentBlock > myCarBlock + 20) || (opponentSpeed > myCarSpeed))
        && (opponentBlock > myCarBlock) && (myCarSpeed >= 6)
        && (canForward == 0) && (Math.abs(opponentLane - myCarLane) <= 1)) {
      return EMP;
    }	

    // Boost always number 1
    if ((hasBoost && !boosting)
        && (canForwardFar == 0)) {
      if (myCar.damage != 0) {
        return FIX;
      }
      return BOOST;
    }

    // Prio take boost
    Command LOOKFOR_BOOST = BOOST_EMP(true);
    if (LOOKFOR_BOOST != null) {
      return LOOKFOR_BOOST;
    }

    // Prio take emp
    Command LOOKFOR_EMP = BOOST_EMP(false);
    if (LOOKFOR_EMP != null) {
      return LOOKFOR_EMP;
    }
	
    // Prio midlane
    Command midlane = PRIO_MID_LANE();
    if (midlane != null) {
      return midlane;
    }

    // While fullspeed and no-blocker then use powerup
    Command usePowerUp = USE_POWERUP();
    if (usePowerUp != null && canForward == 0) {
      return usePowerUp;
    }

    // Prio pickup powerup
    Command prioPowerUp = prioPowerUp();
    if (prioPowerUp != null){
      return prioPowerUp;
	}
	
	// Superior upgrade 1
	if ((hasBoost) && (myCarSpeed < 8) && (canForwardFar <= 1)){
      if (myCar.damage != 0) {
        return FIX;
      }
      return BOOST;
    }

    // Do nothing if can forward
    if (canForward == 0) {
      return SKIP;
    }

	// If cant forward but can turn
    if (canForward > 0 && (canRight == 0 || canLeft == 0)){
      return bestLane;
	}

    // If cant turn but has lizard
    if (hasLizard && !lastBlocked) {
      return LIZARD;
    }

	// Last try, prio take powerup on far side
    Command PrioPowerUpFarLand = prioPowerUpFarLand(gameState);
    if (PrioPowerUpFarLand != null) {
      return PrioPowerUpFarLand;
    }
	
	// Superior upgrade 2
	if ((hasBoost) && (myCar.damage == 0) && (!boosting) && (canForwardFar <= 1)){
      return BOOST;
    }
	
	// If cant do anything
    return bestLane;
  }

  /* Helping function */
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
      if (laneList[i] == null) {
        break;
      }
	  //CyberTruck bisa tiban finish
      if (laneList[i].isOccupiedByCyberTruck == true)
        sum += 4; // karena ngestuck > 2 wall dsb
	  if (laneList[i].terrain == Terrain.FINISH) {
        break;
      }
      if (laneList[i].terrain == Terrain.WALL)
        sum += 3;
      else if ((laneList[i].terrain == Terrain.MUD)
          || (laneList[i].terrain == Terrain.OIL_SPILL)) {
        sum += 1;
      }
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

  private Command compareLane(int forward, int left, int right, int myCarLane) {
    if (forward <= left && forward <= right)
      return ACCELERATE;
    if (left == right) {
      if (myCarLane <= 2)
        return TURN_RIGHT;
      if (myCarLane > 2)
        return TURN_LEFT;
    }
    if (left < right)
      return TURN_LEFT;
    else
      return TURN_RIGHT;
  }

  /* Command procedure */
  private Command USE_POWERUP() {
	Command TWEET;
    if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
        && (opponentBlock <= myCarBlock)
        && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
      return OIL;
    }
    if ((hasTweet) && !(myCarLane == opponentLane && myCarBlock <= opponentBlock + opponentSpeed + 3)
        && (myCarLane != opponentLane || opponentSpeed >= 8)) {
      if (opponentBlock >= 1490) {
        TWEET = new TweetCommand(opponentLane, 1500);
      } else {
        TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
      }
      return TWEET;
    }
	if (countOil > 3) {
      return OIL;
    }
    return null;
  }

  private Command CAN_FORWARD() {
	if (canForward == 0) {
	  if (canAccelerate) {
        return ACCELERATE;
      }
      Command usePowerUp = USE_POWERUP();
	  if (usePowerUp != null){
	    return usePowerUp;
	  }
      return SKIP;
    }
	return null;
  }

  private Boolean RIGHT(int canRight, int canForward, int canLeft, boolean hasLizard) {
    return (canRight == 0) || (canRight <= canForward && canRight <= canLeft && !hasLizard);
  }

  private Boolean LEFT(int canLeft, int canForward, int canRight, boolean hasLizard) {
    return (canLeft == 0) || (canLeft <= canForward && canLeft <= canRight && !hasLizard);
  }
  
  private Command prioPowerUp() {
    if (containPowerUps(blocksFront)) {
	  Command moveForward = CAN_FORWARD();
	  if (moveForward != null){
	    return moveForward;
	  }
    }
    if ((containPowerUps(blocksRight))
        && (RIGHT(canRight, canForward, canLeft, hasLizard))) {
      return TURN_RIGHT;
    }
    if ((containPowerUps(blocksLeft))
        && (LEFT(canLeft, canForward, canRight, hasLizard))) {
      return TURN_LEFT;
    }
    return null;
  }

  private Command BOOST_EMP(Boolean choice) {
    if (choice) {
      if (containBoost(blocksFront)) {
	    Command moveForward = CAN_FORWARD();
	    if (moveForward != null){
	      return moveForward;
	    }
      }
      if ((containBoost(blocksRight))
          && (RIGHT(canRight, canForward, canLeft, hasLizard))) {
        return TURN_RIGHT;
      }
      if ((containBoost(blocksLeft))
          && (LEFT(canLeft, canForward, canRight, hasLizard))) {
        return TURN_LEFT;
      }
    } else {
      if (containEmp(blocksFront)) {
		Command moveForward = CAN_FORWARD();
		if (moveForward != null){
	      return moveForward;
		}
      }
      if ((containEmp(blocksRight))
          && (RIGHT(canRight, canForward, canLeft, hasLizard))) {
        return TURN_RIGHT;
      }
      if ((containEmp(blocksLeft))
          && (LEFT(canLeft, canForward, canRight, hasLizard))) {
        return TURN_LEFT;
      }
    }
    return null;
  }

  private Command prioPowerUpFarLand(GameState gameState) {
    if (containBoost(blocksRightFar) && canRightFar <= canForwardFar && canRightFar <= canLeftFar) {
      return TURN_RIGHT;
    }
    if (containBoost(blocksLeftFar) && canLeftFar <= canForwardFar && canLeftFar <= canRightFar) {
      return TURN_LEFT;
    }
    if (containEmp(blocksRightFar) && canRightFar <= canForwardFar && canRightFar <= canLeftFar) {
      return TURN_RIGHT;
    }
    if (containEmp(blocksLeftFar) && canLeftFar <= canForwardFar && canLeftFar <= canRightFar) {
      return TURN_LEFT;
    }
    if (containPowerUps(blocksRightFar) && canRightFar <= canForwardFar && canRightFar <= canLeftFar) {
      return TURN_RIGHT;
    }
    if (containPowerUps(blocksLeftFar) && canLeftFar <= canForwardFar && canLeftFar <= canRightFar) {
      return TURN_LEFT;
    }
    if (myCarSpeed >= 6 && canMove(myCarLane, myCarBlock + 1, myCarSpeed - 2, opponent, gameState) < 2) {
      return DECELERATE;
    }
    return null;
  }

  private Command PRIO_MID_LANE() {
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
    return null;
  }

  private Command ENDGAME() {
    Command searchBoost;
	Command moveForward;
    /* EMP if losing in endgame */
    if ((hasEMP) && !(myCarLane == opponentLane && myCarBlock + myCarSpeed > opponentBlock)
        && (opponentBlock > myCarBlock) && myCarSpeed >= listSpeed[myCar.damage + 1]
        && (Math.abs(opponentLane - myCarLane) <= 1) && canForward == 0) {
      return EMP;
    }
	/* Boosting condition */
    if (boosting && hasLizard && !lastBlocked)
      return LIZARD;
    if (myCarBlock >= 1485 && hasBoost && myCar.damage == 0 && canForwardFar < 2)
      return BOOST;
    if ((hasBoost) && (!boosting) && (canForwardFar == 0)) {
      if (myCar.damage != 0)
        return FIX;
      return BOOST;
    }
	// Superior upgrade 3
	if ((hasBoost) && (myCarSpeed < 8) && (canForwardFar <= 1)) {
      if (myCar.damage != 0)
        return FIX;
      return BOOST;
    }
	/* Minimum speed */
    if (myCarSpeed <= 3 && canAccelerate) {
      return ACCELERATE;
	}
	/* Prio take boost */
    searchBoost = BOOST_EMP(true);
	if (searchBoost != null) {
	  return searchBoost;
	}
	/* Prio speeding */
    if (canAccelerate) {
      return ACCELERATE;
	}
    /* Moving forward */
    moveForward = CAN_FORWARD();
	if (moveForward != null) {
	  return moveForward;
	}
	/* Lizard if cant forward */
    if (canForward > 0 && canForward == canForwardFar && hasLizard && !lastBlocked) {
      return LIZARD;
    }
	/* If lizard not effective */
    return bestLane;
  }

}