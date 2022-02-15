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
    // -> BIAR USE LIZARD LEBIH MAKSIMAL
    // UPGRADE POWERUP USAGE BIAR EFEKTIF
    Car myCar = gameState.player;
    Car opponent = gameState.opponent;
    int myCarLane = myCar.position.lane;
    int myCarBlock = myCar.position.block;
    int myCarSpeed = myCar.speed;
    int opponentLane = opponent.position.lane;
    int opponentBlock = opponent.position.block;
    int opponentSpeed = opponent.speed;
    Boolean boosting = myCar.boosting;

    List<Object> blocksFront = getBlocks(myCarLane, myCarBlock, myCarSpeed, gameState);
    List<Object> blocksRight = getBlocks(myCarLane + 1, myCarBlock, myCarSpeed, gameState);
    List<Object> blocksLeft = getBlocks(myCarLane - 1, myCarBlock, myCarSpeed, gameState);

    Boolean canRight = canMove(myCarLane + 1, myCarBlock, myCarSpeed - 1, opponent, gameState);
    Boolean canLeft = canMove(myCarLane - 1, myCarBlock, myCarSpeed - 1, opponent, gameState);
    Boolean canForward = canMove(myCarLane, myCarBlock, myCarSpeed, opponent, gameState);
    Boolean canAccelerate = canMove(myCarLane, myCarBlock, myCarSpeed + 2, opponent, gameState) && (myCar.damage <= 1)
        && (myCarSpeed < this.maxSpeed);
    Boolean endGame = myCarBlock >= 1300;

    Boolean hasLizard = hasPowerUp(PowerUps.LIZARD, myCar.powerups);
    int countLizard = countPowerUp(PowerUps.LIZARD, myCar.powerups);

    int listSpeed[] = { 9, 9, 8, 6, 3, 0 }; // from damage 0 to 5
    int listDamage[] = { 1, 1, 2, 2 }; // MUD OIL WALL CYBERTRUCK
    Terrain listBlocker[] = { Terrain.MUD, Terrain.WALL, Terrain.OIL_SPILL }; // MUD OIL WALL
    Terrain listPowerUps[] = { Terrain.BOOST, Terrain.EMP, Terrain.LIZARD, Terrain.TWEET, Terrain.OIL_POWER };

    /* if cant move */
    if (myCar.damage >= 2) {
      return FIX;
    }
    if (myCarSpeed == 0) {
      return ACCELERATE;
    }
    /* endgame mode : prio speed but still turn right and left */
    if (endGame) {
      /* EMP if losing in endgame */
      if ((hasPowerUp(PowerUps.EMP, myCar.powerups))
          && (opponentBlock > myCarBlock) && myCarSpeed >= 6 && (Math.abs(opponentLane - myCarLane) <= 1)) {
        return EMP;
      }
      if ((hasPowerUp(PowerUps.BOOST, myCar.powerups))
          && (!boosting) && canForward) {
        if (myCar.damage != 0) {
          return FIX;
        }
        return BOOST;
      }
      if (canAccelerate) {
        if (myCarSpeed >= 8 && hasLizard)
          return LIZARD;
        return ACCELERATE;
      }

      if (containBoost(blocksRight) && canRight) {
        return TURN_RIGHT;
      }
      if (containBoost(blocksLeft) && canLeft) {
        return TURN_LEFT;
      }

      if (hasLizard) {
        return LIZARD;
      }
    }
    /* Prio use emp if losing */
    if ((hasPowerUp(PowerUps.EMP, myCar.powerups))
        && ((opponentBlock > myCarBlock + 50) || (opponentSpeed == 15))
        && (opponentBlock > myCarBlock)
        && (canForward) && (Math.abs(opponentLane - myCarLane) <= 1)) {
      return EMP;
    }
    /* Minimum speed */
    if (myCarSpeed <= 5 && canAccelerate) {
      return ACCELERATE;
    }

    /* boost number 1 */
    if ((hasPowerUp(PowerUps.BOOST, myCar.powerups))
        && (!boosting)
        && (canMove(myCarLane, myCarBlock, 15, opponent, gameState))) {
      if (myCar.damage != 0) {
        return FIX;
      }
      return BOOST;
    }

    /* Lane prio, harus di tengah */
    if (myCarLane == 1 && canRight) {
      return TURN_RIGHT;
    }
    if (myCarLane == 4 && canLeft) {
      return TURN_LEFT;
    }
    /* Prio speeed */
    if (canAccelerate) {
      return ACCELERATE;
    }
    /* Prio take boost */
    if (containBoost(blocksFront)) {
      System.out.println("1");
      if ((hasLizard)
          && (countLizard >= 5)) {
        return LIZARD;
      }
      if (canForward) {
        if (canAccelerate) {
          return ACCELERATE;
        }
        return SKIP;
      }
    }
    if (containBoost(blocksRight) && canRight) {
      return TURN_RIGHT;
    }
    if (containBoost(blocksLeft) && canLeft) {
      return TURN_LEFT;
    }
    /* Prio take emp */
    if (containEmp(blocksFront)) {
      System.out.println("2");
      if ((hasLizard)
          && (countLizard >= 5)) {
        return LIZARD;
      }
      if (canForward) {
        if (canAccelerate) {
          return ACCELERATE;
        }
        return SKIP;
      }
    }
    if (containEmp(blocksRight) && canRight) {
      return TURN_RIGHT;
    }
    if (containEmp(blocksLeft) && canLeft) {
      return TURN_LEFT;
    }
    /* Prio move forward as fast as possible */
    if (containPowerUps(blocksFront)) {
      if ((hasLizard)
          && (countLizard >= 5)) {
        return LIZARD;
      }
      if (canForward) {
        System.out.println("3");
        if (canAccelerate) {
          return ACCELERATE;
        }
        if ((hasPowerUp(PowerUps.EMP, myCar.powerups))
            && (opponentBlock > myCarBlock) && (Math.abs(opponentLane - myCarLane) <= 1)) {
          return EMP;
        }
        if ((hasPowerUp(PowerUps.OIL, myCar.powerups))
            && (opponentBlock < myCarBlock)
            && (!canLeft)
            && (!canRight)) {
          return OIL;
        }
        if ((hasPowerUp(PowerUps.TWEET, myCar.powerups))
            && (myCarLane != opponentLane)
            && (opponentBlock <= myCarBlock + 2 * myCarSpeed)) {
          Command TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed);
          return TWEET;
        }
        return SKIP;
      }
    }
    if (canForward) {
      if ((hasPowerUp(PowerUps.EMP, myCar.powerups))
          && (opponentBlock > myCarBlock) && (Math.abs(opponentLane - myCarLane) <= 1)) {
        return EMP;
      }
      if ((hasPowerUp(PowerUps.OIL, myCar.powerups))
          && (opponentBlock < myCarBlock)
          && (!canLeft)
          && (!canRight)) {
        return OIL;
      }
      // masi bisa difix
      if ((hasPowerUp(PowerUps.TWEET, myCar.powerups))
          && (myCarLane != opponentLane)
          && (opponentBlock <= myCarBlock + 2 * myCarSpeed)) {
        Command TWEET = new TweetCommand(opponentLane, opponentBlock + opponentSpeed);
        return TWEET;
      }
    }
    /* Prio take powerup on side */
    if (containPowerUps(blocksRight) && canRight) {
      return TURN_RIGHT;
    }
    if (containPowerUps(blocksLeft) && canLeft) {
      return TURN_LEFT;
    }
    /* Prio keep moving */
    if (canForward) {
      System.out.println("4");
      if ((hasPowerUp(PowerUps.OIL, myCar.powerups))
          && (opponentBlock < myCarBlock)) {
        return OIL;
      }
      if ((hasPowerUp(PowerUps.EMP, myCar.powerups))
          && (opponentBlock > myCarBlock) && (Math.abs(opponentLane - myCarLane) <= 1)) {
        return EMP;
      }
      return SKIP;
    }
    if (canRight) {
      return TURN_RIGHT;
    }
    if (canLeft) {
      return TURN_LEFT;
    }

    if (hasLizard) {
      return LIZARD;
    }
    /* if cant do anything */
    System.out.println("5");
    if (canAccelerate)
      return ACCELERATE;
    return SKIP;
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

  private Boolean canMove(int lane, int block, int speed, Car opponent, GameState gameState) {
    int myCarLane = lane;
    int myCarBlock = block;
    int myCarSpeed = speed;
    int opponentLane = opponent.position.lane;
    int opponentBlock = opponent.position.block;
    int opponentSpeed = opponent.speed;

    if (lane < 1 || lane > 4) {
      return false;
    }
    if ((myCarLane == opponentLane)
        && (myCarBlock < opponentBlock)
        && (myCarBlock + myCarSpeed >= opponentBlock + opponentSpeed)) {
      return false;
    }
    Boolean out = true;
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    Lane[] laneList = map.get(lane - 1);
    int tmp = myCarBlock - startBlock;
    int i = tmp;
    while (laneList[i].terrain != Terrain.FINISH && laneList[i] != null && i <= tmp + myCarSpeed && out) {
      if ((laneList[i].terrain == Terrain.MUD)
          || (laneList[i].terrain == Terrain.OIL_SPILL)
          || (laneList[i].terrain == Terrain.WALL)
          || (laneList[i].isOccupiedByCyberTruck == true)) {
        out = false;
      } else {
        i++;
      }
    }
    return out;
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
}