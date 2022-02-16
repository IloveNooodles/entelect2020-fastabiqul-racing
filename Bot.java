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
    Car myCar = gameState.player;
    Car opponent = gameState.opponent;
    int myCarLane = myCar.position.lane;
    int myCarBlock = myCar.position.block;
    int myCarSpeed = myCar.speed;
    int opponentLane = opponent.position.lane;
    int opponentBlock = opponent.position.block;
    int opponentSpeed = opponent.speed;
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    int currentBlock = myCarBlock - startBlock;
    Boolean boosting = myCar.boostCounter > 0;
    Command TWEET;

    List<Object> blocksFront = getBlocks(myCarLane, myCarBlock + 1, myCarSpeed, gameState);
    List<Object> blocksFrontFar = getBlocks(myCarLane, myCarBlock + 1, 15, gameState);
    List<Object> blocksRight = getBlocks(myCarLane + 1, myCarBlock, myCarSpeed, gameState);
    List<Object> blocksRightFar = getBlocks(myCarLane + 1, myCarBlock, 15, gameState);
    List<Object> blocksLeft = getBlocks(myCarLane - 1, myCarBlock, myCarSpeed, gameState);
    List<Object> blocksLeftFar = getBlocks(myCarLane - 1, myCarBlock, 15, gameState);

    int canRight = canMove(myCarLane + 1, myCarBlock, myCarSpeed, opponent, gameState);
    int canRightFar = canMove(myCarLane + 1, myCarBlock, 15, opponent, gameState);
    int canLeft = canMove(myCarLane - 1, myCarBlock, myCarSpeed, opponent, gameState);
    int canLeftFar = canMove(myCarLane - 1, myCarBlock, 15, opponent, gameState);
    int canForward = canMove(myCarLane, myCarBlock + 1, myCarSpeed, opponent, gameState);
    int canForwardFar = canMove(myCarLane, myCarBlock + 1, 15, opponent, gameState);
    Command bestLane = compareLane(canForward, canLeft, canRight, myCarLane);

    Boolean endGame = myCarBlock >= 1350;

    Boolean hasLizard = hasPowerUp(PowerUps.LIZARD, myCar.powerups);
    Boolean hasBoost = hasPowerUp(PowerUps.BOOST, myCar.powerups);
    Boolean hasEMP = hasPowerUp(PowerUps.EMP, myCar.powerups);
    Boolean hasOil = hasPowerUp(PowerUps.OIL, myCar.powerups);
    Boolean hasTweet = hasPowerUp(PowerUps.TWEET, myCar.powerups);
    int countLizard = countPowerUp(PowerUps.LIZARD, myCar.powerups);
    int countBoost = countPowerUp(PowerUps.BOOST, myCar.powerups);
    int countOil = countPowerUp(PowerUps.OIL, myCar.powerups);
    int countTweet = countPowerUp(PowerUps.TWEET, myCar.powerups);

    int listSpeed[] = { 9, 9, 8, 6, 3, 0 }; // from damage 0 to 5
    Boolean canAccelerate = canMove(myCarLane, myCarBlock, myCarSpeed + 2, opponent, gameState) == 0
        && (myCarSpeed < listSpeed[myCar.damage]);

    int idxBlock;
    if (myCarBlock + myCarSpeed > 1500) {
      idxBlock = 1500 - startBlock;
    } else {
      idxBlock = currentBlock + myCarSpeed;
    }
    Boolean lastBlocked = false;
    if (blocksFront.size() > 0) {
      lastBlocked = blocksFront.get(blocksFront.size() - 1) == Terrain.MUD
          || blocksFront.get(blocksFront.size() - 1) == Terrain.WALL
          || blocksFront.get(blocksFront.size() - 1) == Terrain.OIL_SPILL
          || map.get(myCarLane - 1)[idxBlock - 1].isOccupiedByCyberTruck == true;
    }
    /* if cant move */
    if (myCar.damage >= 3) {
      if (myCarSpeed < listSpeed[myCar.damage] && canAccelerate)
        return ACCELERATE;
      return FIX;
    }
    if (myCarSpeed == 0)
      return ACCELERATE;
    /* endgame mode : prio speed but still turn right and left */
    if (endGame) {
      /* EMP if losing in endgame */
      if ((hasEMP) && (myCarLane != opponentLane || myCarBlock + myCarSpeed <= opponentBlock)
          && (opponentBlock > myCarBlock) && myCarSpeed >= listSpeed[myCar.damage + 1]
          && (Math.abs(opponentLane - myCarLane) <= 1) && canForward == 0) {
        return EMP;
      }
      if (boosting && hasLizard && !lastBlocked)
        return LIZARD;
      if (myCarBlock >= 1485 && hasBoost && myCar.damage == 0 && canForwardFar < 2)
        return BOOST;
      if ((hasBoost) && (!boosting) && (canForwardFar == 0)) {
        if (myCar.damage != 0)
          return FIX;
        return BOOST;
      }
      if (myCarSpeed <= 3 && canAccelerate)
        return ACCELERATE;
      if (containBoost(blocksFront) && canForward == 0) {
        if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
            && (opponentBlock <= myCarBlock)
            && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
          return OIL;
        }
        if ((hasTweet)
            && (myCarLane != opponentLane || opponentSpeed >= 8)) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
        if (countOil > 3 && myCarBlock >= opponentBlock)
          return OIL;
        if (countTweet > 3 && myCarBlock < opponentBlock) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
        return SKIP;
      }
      if (containBoost(blocksRight) && canRight == 0)
        return TURN_RIGHT;
      if (containBoost(blocksLeft) && canLeft == 0)
        return TURN_LEFT;
      if (canAccelerate)
        return ACCELERATE;
      /* use Powerup */
      if (canForward == 0) {
        if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
            && (opponentBlock <= myCarBlock)
            && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
          return OIL;
        }
        if ((hasTweet)
            && (myCarLane != opponentLane || opponentSpeed >= 8)) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
      }
      if (canForward > 0 && canForward == canForwardFar && hasLizard && !lastBlocked) {
        return LIZARD;
      }
      if (canRight == 0)
        return TURN_RIGHT;
      if (canLeft == 0)
        return TURN_LEFT;
      return bestLane;
    }
    /* Boost while min speed */
    if (myCarSpeed <= 3 && hasBoost && myCar.damage == 0 && canForwardFar == 0)
      return BOOST;
    if (myCarSpeed <= 5 && canAccelerate)
      return ACCELERATE;
    /* Prio use emp if losing */
    if ((hasEMP)
        && ((opponentBlock > myCarBlock + 20) || (opponentSpeed > myCarSpeed))
        && (opponentBlock > myCarBlock) && (myCarSpeed >= 6)
        && (canForward == 0) && (Math.abs(opponentLane - myCarLane) <= 1)) {
      return EMP;
    }
    /* boost number 1 */
    if ((hasBoost && !boosting)
        && (canForwardFar == 0)) {
      if (myCar.damage != 0) {
        return FIX;
      }
      return BOOST;
    }
    /* Prio take boost */
    if (containBoost(blocksFront)) {
      if (canForward == 0) {
        if (canAccelerate) {
          return ACCELERATE;
        }
        if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
            && (opponentBlock <= myCarBlock)
            && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
          return OIL;
        }
        if ((hasTweet)
            && (myCarLane != opponentLane || opponentSpeed >= 8)) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
        if (countOil > 3)
          return OIL;
        if (countTweet > 3 && myCarLane != opponentLane) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
        return SKIP;
      }
      if ((hasLizard) && (!lastBlocked)
          && (countLizard > 1)) {
        return LIZARD;
      }
    }
    if ((containBoost(blocksRight))
        && ((canRight == 0) || (canRight <= canForward && canRight <= canLeft && !hasLizard))) {
      return TURN_RIGHT;
    }
    if ((containBoost(blocksLeft))
        && ((canLeft == 0) || (canLeft <= canForward && canLeft <= canRight && !hasLizard))) {
      return TURN_LEFT;
    }
    /* Prio take emp */
    if (containEmp(blocksFront)) {
      if (canForward == 0) {
        if (canAccelerate) {
          return ACCELERATE;
        }
        if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
            && (opponentBlock <= myCarBlock)
            && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
          return OIL;
        }
        if ((hasTweet)
            && (myCarLane != opponentLane || opponentSpeed >= 8)) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
        if (countOil > 3)
          return OIL;
        if (countTweet > 3 && myCarLane != opponentLane) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
        return SKIP;
      }
      if ((hasLizard) && (!lastBlocked)
          && (countLizard > 1)) {
        return LIZARD;
      }
    }
    if ((containEmp(blocksRight))
        && ((canRight == 0) || (canRight <= canForward && canRight <= canLeft && !hasLizard))) {
      return TURN_RIGHT;
    }
    if ((containEmp(blocksLeft))
        && ((canLeft == 0) || (canLeft <= canForward && canLeft <= canRight && !hasLizard))) {
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
    /* while fullspeed and no-blocker */
    /* use powerup */
    if (canForward == 0) {
      if ((hasOil) && (Math.abs(opponentLane - myCarLane) <= 1)
          && (opponentBlock <= myCarBlock)
          && ((opponentLane == myCarLane) || (canLeft != 0 && canRight != 0))) {
        return OIL;
      }
      if ((hasTweet)
          && (myCarLane != opponentLane || opponentSpeed >= 8)) {
        if (opponentBlock >= 1490) {
          TWEET = new TweetCommand(opponentLane, 1500);
        } else {
          TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
        }
        return TWEET;
      }
      if (countOil > 3)
        return OIL;
      if (countTweet > 3 && myCarLane != opponentLane) {
        if (opponentBlock >= 1490) {
          TWEET = new TweetCommand(opponentLane, 1500);
        } else {
          TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
        }
        return TWEET;
      }
    }
    /* Prio pickup powerup */
    if (containPowerUps(blocksFront)) {
      if ((hasLizard) && (!lastBlocked)
          && (countLizard > 1)) {
        return LIZARD;
      }
      if (canForward == 0) {
        if (countOil > 3)
          return OIL;
        if (countTweet > 3 && myCarLane != opponentLane) {
          if (opponentBlock >= 1490) {
            TWEET = new TweetCommand(opponentLane, 1500);
          } else {
            TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
          }
          return TWEET;
        }
        return SKIP;
      }
    }
    if ((containPowerUps(blocksRight))
        && ((canRight == 0) || (canRight <= canForward && canRight <= canLeft && !hasLizard))) {
      return TURN_RIGHT;
    }
    if ((containPowerUps(blocksLeft))
        && ((canLeft == 0) || (canLeft <= canForward && canLeft <= canRight && !hasLizard))) {
      return TURN_LEFT;
    }
    /* fix / do nothing */
    if (canForward == 0) {
      if (countOil > 3)
        return OIL;
      if (countTweet > 3 && myCarLane != opponentLane) {
        if (opponentBlock >= 1490) {
          TWEET = new TweetCommand(opponentLane, 1500);
        } else {
          TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed + 3);
        }
        return TWEET;
      }
      return SKIP;
    }
    if (canForward > 0 && (canRight == 0 || canLeft == 0))
      return bestLane;
    /* if cant do anything */
    if (hasLizard && !lastBlocked) {
      return LIZARD;
    }
    /* Prio take powerup on far side */
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
      if (laneList[i].isOccupiedByCyberTruck == true)
        sum += 4; // karena ngestuck > 2 wall dsb
      else if (laneList[i].terrain == Terrain.WALL)
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
}