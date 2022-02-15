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
  private int cybertruckPos = 0;

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

  private int listSpeed[] = { 9, 9, 8, 6, 3, 0 };

  private HashMap<Terrain, Integer> damage = new HashMap<Terrain, Integer>();
  HashMap<Terrain, Integer> points = new HashMap<Terrain, Integer>();

  private void setup() {
    for (Terrain t : listPowerup) {
      points.put(t, 4);
    }
    for (Terrain t : listBlockable) {
      if (t == Terrain.MUD) {
        points.put(t, -3);
        damage.put(t, 1);
      } else if (t == Terrain.WALL) {
        points.put(t, -999);
        damage.put(t, 2);
      } else {
        points.put(t, -4);
        damage.put(t, 1);
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
    System.out.println(blocks);
    // TODO
    // 1. Fixed car while the damage is >= 3
    // Bisa bandingin dulu sebanyak banyak bloknya baru belok FIXME
    // bisa bandingin damege FIXME
    // use lizard masi bug gaada apa2
    // prioritas 1.fix klo 5, 2. speed 0,
    // bug: kalo udah 15 masi accelerate
    // udah maxspeed masi accelerate (fungsi validasi) FIXME
    // cybertruck bisa ngecek klo udah maju berapa
    // BOOST > EMP
    if (myCar.damage >= 3) {
      return FIX;
    }

    if (myCar.speed < 3) {
      return ACCELERATE;
    }
    // 2. use boost while you can and wall is bad
    if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.boosting == false) {
      if (myCar.damage != 0) {
        return FIX;
      }
      // the consideration is we don't need to crash but it can be returned to max
      // speed
      if (blockerLand == 0 && !blocks.contains(true))
        return BOOST;
    }

    if (blocks.contains(true)) {
      if (myCar.position.lane == 1) {
        return TURN_RIGHT;
      } else if (myCar.position.lane == 4) {
        return TURN_LEFT;
      }
      int i = random.nextInt(directionList.size());
      if (i == -1) {
        return TURN_LEFT;
      } else {
        return TURN_RIGHT;
      }
    }

    int bestChoice = turnDesicion(myCar.position.lane, myCar.position.block, gameState);
    if (bestChoice == 1) {
      return TURN_RIGHT;
    } else if (bestChoice == -1) {
      return TURN_LEFT;
    }

    // ini harus dicek lagi kaya convert damage sm speed skrg
    if (myCar.speed <= listSpeed[myCar.damage] && blockerLand == 0) {
      return ACCELERATE;
    }

    // 3. Accelerate First while avoiding crash and pickup powerups need to maximize

    if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && (blockerLand > 0 || blocks.contains(true))) {
      return LIZARD;
    }

    if (opponent.position.block > myCar.position.block && hasPowerUp(PowerUps.EMP, myCar.powerups)) {
      return EMP;
    } else if (hasPowerUp(PowerUps.TWEET, myCar.powerups) && blockerLand == 0) {
      try {
        cybertruckPos = opponent.position.block + opponent.speed + 3;
        return new TweetCommand(opponent.position.lane, cybertruckPos);
      } catch (Exception e) {
        return ACCELERATE;
      }
    } else if (opponent.position.block < myCar.position.block && hasPowerUp(PowerUps.OIL, myCar.powerups)) {
      return OIL;
    }
    if (myCar.speed <= listSpeed[myCar.damage]) {
      return ACCELERATE;
    }
    return DO_NOTHING;
    // 4. klo udah maxSpeed tinggal cek kiri kanan ambil best possiblity with the

    // attacking command
    // 5. klo ternytata lanenya sama bisa mutusin pidnah atau engga bisa dicek sama
    // powerup nya musuh
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
  }

  // nentuin belok kanan atau kiri
  private int turnDesicion(int lane, int block, GameState gameState) {
    int curPoint = convertPoint(lane, block, gameState);
    int bestChoice = 0;
    if (lane == 1) {
      int rightPoint = convertPoint(lane + 1, block, gameState);
      if (rightPoint > curPoint) {
        bestChoice = 1;
      }
      if (curPoint == rightPoint) {
        bestChoice = compareLane(lane, lane + 1, block, gameState);
      }
    } else if (lane == 4) {
      int leftPoint = convertPoint(lane - 1, block, gameState);
      if (leftPoint > curPoint) {
        bestChoice = -1;
      }
      if (curPoint == leftPoint) {
        bestChoice = compareLane(lane, lane - 1, block, gameState);
      }
    } else if (lane >= 2 && lane <= 3) {
      bestChoice = compareThreeLine(lane, block, gameState);
    }

    return bestChoice;
  }

  private int compareThreeLine(int lane, int block, GameState gameState) {
    // compare left and right
    int leftRight = compareLane(lane - 1, lane + 1, block, gameState);
    // compare the best and the current lane;
    leftRight = convertIntToLane(leftRight, lane);
    int bestLane = compareLane(lane, leftRight, block, gameState);
    return bestLane;
    // int left = compareLane(lane, lane - 1, block, gameState); // 0
    // int right = compareLane(lane, lane + 1, block, gameState); // 1
    // left = convertIntToLane(left, lane); // lane
    // right = convertIntToLane(right, lane); // lane+1
  }

  private int convertIntToLane(int val, int lane) {
    if (val == -1) {
      return lane - 1;
    } else if (val == 0) {
      return lane;
    } else {
      return lane + 1;
    }
  }

  private int compareLane(int currentLane, int anotherLane, int block, GameState gameState) {
    int curPoint = convertPoint(currentLane, block, gameState);
    int anotherPoint = convertPoint(anotherLane, block, gameState);
    int direction = currentLane > anotherLane ? -1 : 1;
    if (Math.abs(currentLane - anotherLane) >= 2) {
      if (curPoint > anotherPoint) {
        return -1;
      } else if (curPoint == anotherPoint) {
        HashMap<Terrain, Integer> curPowerUp = generatePossiblePowerUp(currentLane, block, gameState);
        HashMap<Terrain, Integer> anotherPowerUp = generatePossiblePowerUp(anotherLane, block, gameState);
        if (comparePowerUp(curPowerUp, anotherPowerUp)) {
          return -1;
        }
      }
      return 1;
    } else {
      if (curPoint > anotherPoint) {
        return 0;
      } else if (curPoint == anotherPoint) {
        HashMap<Terrain, Integer> curPowerUp = generatePossiblePowerUp(currentLane, block, gameState);
        HashMap<Terrain, Integer> anotherPowerUp = generatePossiblePowerUp(anotherLane, block, gameState);
        if (comparePowerUp(curPowerUp, anotherPowerUp)) {
          return 0;
        }
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
      if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
        break;
      }
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
      if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
        break;
      }
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
      if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
        break;
      }
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
      if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
        break;
      }
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
      if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
        break;
      }
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
      blocks.add(laneList[i].isOccupiedByCyberTruck);

    }
    return blocks;
  }

}
