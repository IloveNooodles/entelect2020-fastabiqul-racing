package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.State;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

import java.security.SecureRandom;

public class Bot {
  private static final int maxSpeed = 9;
  private List<Command> directionList = new ArrayList<>();

  private final Random random;

  private final static Command ACCELERATE = new AccelerateCommand();
  private final static Command DEACELERATE = new DecelerateCommand();
  private final static Command DO_NOTHING = new DoNothingCommand();
  private final static Command LIZARD = new LizardCommand();
  private final static Command OIL = new OilCommand();
  private final static Command BOOST = new BoostCommand();
  private final static Command EMP = new EmpCommand();
  private final static Command FIX = new FixCommand();
  // private final static Command TWEET = new TweetCommand(0, 0);

  private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
  private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

  private Terrain listPowerup[] = {
      Terrain.LIZARD,
      Terrain.BOOST,
      Terrain.TWEET,
      Terrain.EMP,
      Terrain.OIL_POWER
  };

  private Terrain listBlockable[] = {
      Terrain.MUD,
      Terrain.WALL,
      Terrain.OIL_SPILL
  };

  HashMap<Terrain, Integer> points = new HashMap<Terrain, Integer>();

  private void setup() {
    for (Terrain t : listPowerup) {
      points.put(t, 4);
    }
    for (Terrain t : listBlockable) {
      if (t == Terrain.MUD) {
        points.put(t, -3);
      } else if (t == Terrain.WALL) {
        points.put(t, 0);
      } else {
        points.put(t, -4);
      }
    }
  }

  public Bot() {
    this.random = new SecureRandom();
    directionList.add(TURN_LEFT);
    directionList.add(TURN_RIGHT);
    setup();
  }

  public Command run(GameState gameState) {
    Car myCar = gameState.player;
    Car opponent = gameState.opponent;

    // Basic fix logic
    List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
    List<Object> nextBlocks = blocks.subList(0, 1);
    int emptyLand = countEmpty(myCar.position.lane, myCar.position.block, gameState);
    int blockerLand = countLaneBlockers(myCar.position.lane, myCar.position.block, gameState);
    // TODO
    // 1. Fixed car while the damage is >= 3
    if (myCar.damage >= 3) {
      return FIX;
    }
    // 2. use boost while you can and wall is bad
    if (myCar.boostCounter != 0 && !myCar.boosting && !blocks.contains(Terrain.WALL)) {
      return BOOST;
      // the consideration is we don't need to crash but it can be returned to max
      // speed
    }
    // 3. Accelerate First while avoiding crash and pickup powerups need to maximize
    if (myCar.speed <= maxSpeed && blockerLand == 0) {
      return ACCELERATE;
    }

    // 4. klo udah maxSpeed tinggal cek kiri kanan ambil best possiblity with the
    // point / powerup
    int bestChoice = turnDesicion(myCar.position.lane, myCar.position.block, gameState);
    if (bestChoice != 0) {
      return new ChangeLaneCommand(bestChoice);
    }
    // attacking command
    // 5. klo ternytata lanenya sama bisa mutusin pidnah atau engga bisa dicek sama
    // powerup nya musuh
    if (opponent.position.lane == myCar.position.lane) {
      int difference = Math.abs(opponent.position.block - myCar.position.block);
      if (opponent.position.block > myCar.position.block && hasPowerUp(PowerUps.EMP, myCar.powerups)
          && difference <= opponent.speed) {
        return EMP;
      } else if (opponent.position.block < myCar.position.block && hasPowerUp(PowerUps.OIL, myCar.powerups)
          && difference <= opponent.speed) {
        return OIL;
      }
    }
    // 6.
    // defensive command
    // tweet command
    // avoid cybertruck
    // lizard and boost should be the one yes if im still boosting tho and i could
    // use lizard
    //

    // Basic avoidance logic
    // if (blocks.contains(Terrain.MUD) || nextBlocks.contains(Terrain.WALL)) {
    // if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
    // return LIZARD;
    // }
    // if (nextBlocks.contains(Terrain.MUD) || nextBlocks.contains(Terrain.WALL)) {
    // int i = random.nextInt(directionList.size());
    // return directionList.get(i);
    // }
    // }
    return ACCELERATE;
  }

  // nentuin belok kanan atau kiri
  private int turnDesicion(int lane, int block, GameState gameState) {
    int curPoint = convertPoint(lane, block, gameState);
    int bestChoice = 0;
    if (lane == 1) {
      int rightPoint = convertPoint(lane + 1, block, gameState);
      if (curPoint == rightPoint) {
        bestChoice = compareLane(lane, lane + 1, block, gameState);
      }
    } else if (lane == 4) {
      int leftPoint = convertPoint(lane - 1, block, gameState);
      if (curPoint == leftPoint) {
        bestChoice = compareLane(lane, lane - 1, block, gameState);
      }
    } else {
      bestChoice = compareThreeLine(lane, block, gameState);
    }

    return bestChoice;
  }

  private int compareThreeLine(int lane, int block, GameState gameState) {
    int direction = compareLane(lane, lane - 1, block, gameState);
    direction = compareLane(lane + 1, direction, block, gameState);
    return direction;
  }

  private int compareLane(int currentLane, int anotherLane, int block, GameState gameState) {
    int curPoint = convertPoint(currentLane, block, gameState);
    int anotherPoint = convertPoint(anotherLane, block, gameState);
    int direction = currentLane > anotherLane ? -1 : 1;
    if (curPoint > anotherPoint) {
      return 0;
    } else if (curPoint == anotherPoint) {
      HashMap<Terrain, Integer> curPowerUp = generatePossiblePowerUp(currentLane, block, gameState);
      HashMap<Terrain, Integer> anotherPowerUp = generatePossiblePowerUp(currentLane, block, gameState);
      if (comparePowerUp(curPowerUp, anotherPowerUp)) {
        return 0;
      }
    }
    return direction;
  }

  private boolean comparePowerUp(HashMap<Terrain, Integer> curPowerUp, HashMap<Terrain, Integer> anotherPowerUp) {
    int curSum = 0;
    int anotherSum = 0;
    for (int val : curPowerUp.values()) {
      curSum += val;
    }
    for (int val : anotherPowerUp.values()) {
      anotherSum += val;
    }
    if (curSum > anotherSum) {
      return true;
    }
    if (curSum == anotherSum) {
      if (curPowerUp.get(Terrain.BOOST) >= anotherPowerUp.get(Terrain.BOOST)) {
        return true;
      }
      return false;
    }
    return false;
  }

  private HashMap<Terrain, Integer> generatePossiblePowerUp(int lane, int block, GameState gameState) {
    HashMap<Terrain, Integer> possiblePowerUp = new HashMap<Terrain, Integer>();
    for (Terrain t : listPowerup) {
      possiblePowerUp.put(t, 0);
    }
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    int curVal = 0;

    Lane[] laneList = map.get(lane - 1);
    for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
      if (possiblePowerUp.containsKey(laneList[i].terrain)) {
        curVal = possiblePowerUp.get(laneList[i].terrain);
        possiblePowerUp.put(laneList[i].terrain, curVal + 1);
      }
    }

    return possiblePowerUp;
  }

  // konvert point sekaligus memasukan banyaknya ke dalam hashmap ini auto update
  // setiap round
  private int convertPoint(int lane, int block, GameState gameState) {
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    int curPoint = 0;

    Lane[] laneList = map.get(lane - 1);
    for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
      if (points.containsKey(laneList[i].terrain)) {
        curPoint += points.get(laneList[i].terrain);
      }
    }
    return curPoint;
  }

  private int countEmpty(int lane, int block, GameState gameState) {
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    int emptyCounts = 0;

    Lane[] laneList = map.get(lane - 1);
    for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
      if (laneList[i].terrain == Terrain.EMPTY) {
        emptyCounts++;
      }
    }
    return emptyCounts;
  }

  // ngitung di satu lane ada berapa banyak blocker
  private int countLaneBlockers(int lane, int block, GameState gameState) {
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    int blockerCounts = 0;

    Lane[] laneList = map.get(lane - 1);
    for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
      for (Terrain t : listBlockable) {
        if (laneList[i].terrain == t) {
          blockerCounts++;
        }
      }
    }
    return blockerCounts;
  }

  // ngitung di satu lane ada berapa banyak powerup
  private int countLanePowerUps(int lane, int block, GameState gameState) {
    List<Lane[]> map = gameState.lanes;
    int startBlock = map.get(0)[0].position.block;
    int powerUpsCount = 0;

    Lane[] laneList = map.get(lane - 1);
    for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
      for (Terrain t : listPowerup) {
        if (laneList[i].terrain == t) {
          powerUpsCount++;
        }
      }
    }
    return powerUpsCount;

  }

  private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
    for (PowerUps powerUp : available) {
      if (powerUp.equals(powerUpToCheck)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns map of blocks and the objects in the for the current lanes, returns
   * the amount of blocks that can be traversed at max speed.
   **/
  private List<Object> getBlocksInFront(int lane, int block, GameState gameState) {
    List<Lane[]> map = gameState.lanes;
    List<Object> blocks = new ArrayList<>();
    int startBlock = map.get(0)[0].position.block;

    Lane[] laneList = map.get(lane - 1);
    for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
      if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
        break;
      }

      blocks.add(laneList[i].terrain);

    }
    return blocks;
  }

}
