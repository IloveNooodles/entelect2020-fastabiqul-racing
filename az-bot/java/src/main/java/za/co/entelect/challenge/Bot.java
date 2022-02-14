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
	
	public Bot(){
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
		
		List<Object> blocksFront = getBlocks(myCarLane, myCarBlock, myCarSpeed, gameState);
        List<Object> blocksRight = getBlocks(myCarLane+1, myCarBlock, myCarSpeed, gameState);
		List<Object> blocksLeft = getBlocks(myCarLane-1, myCarBlock, myCarSpeed, gameState);
		
		Boolean canRight = canMove(myCarLane + 1, myCarBlock, myCarSpeed-1, opponent, gameState);
		Boolean canLeft = canMove(myCarLane - 1, myCarBlock, myCarSpeed-1, opponent, gameState);
		Boolean canForward = canMove(myCarLane, myCarBlock, myCarSpeed, opponent, gameState);
		Boolean canAccelerate = canMove(myCarLane, myCarBlock, myCarSpeed + 2, opponent, gameState) && (myCar.damage <= 1) && (myCarSpeed != this.maxSpeed);
        
		if (myCar.damage >= 2) {
            return FIX;
        }
		if (myCarSpeed == 0){
			return ACCELERATE;
		}
		
		if ((hasPowerUp(PowerUps.BOOST, myCar.powerups))
			&&(canMove(myCarLane, myCarBlock, 15, opponent, gameState))
			&&(myCar.damage == 0)) {
            return BOOST;
        }
		
		if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
			return LIZARD;
		}
		
		if (containPowerUps(blocksFront) && canForward){
			if(canAccelerate){
				return ACCELERATE;
			}
			return SKIP;
		}
		if (containPowerUps(blocksRight) && canRight){
			return TURN_RIGHT;
		}
		if (containPowerUps(blocksLeft) && canLeft){
			return TURN_LEFT;
		}
		
		if (canAccelerate) {
            return ACCELERATE;
        }
		
		if ((hasPowerUp(PowerUps.EMP, myCar.powerups))
			&&(opponentBlock > myCarBlock)) {
            return EMP;
        }
		if ((hasPowerUp(PowerUps.OIL, myCar.powerups))
			&&(opponentBlock < myCarBlock)
			&&(opponentLane == myCarLane)){
            return OIL;
        }
		if ((hasPowerUp(PowerUps.TWEET, myCar.powerups))
			&&(myCarLane != opponentLane)){
			Command TWEET = new TweetCommand(opponentLane,opponentBlock+opponentSpeed);
			return TWEET;
		}
		
		if (canForward){
			return SKIP;
		}
		if (canRight){
			return TURN_RIGHT;
		}
		if (canLeft){
			return TURN_LEFT;
		}
		
		return DECELERATE;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }
    private List<Object> getBlocks(int lane, int block, int speed, GameState gameState) {  
        List<Object> blocks = new ArrayList<>();
		if (lane > 0 && lane <= 4){
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
	private Boolean canMove(int lane, int block, int speed, Car opponent, GameState gameState){
		int myCarLane = lane;
		int myCarBlock = block;
		int myCarSpeed = speed;
		int opponentLane = opponent.position.lane;
		int opponentBlock = opponent.position.block;
		int opponentSpeed = opponent.speed;
		
		if (lane < 1 || lane > 4){
			return false;
		}
		if ((myCarLane == opponentLane)
			&&(myCarBlock < opponentBlock)
			&&(myCarBlock + myCarSpeed >= opponentBlock + opponentSpeed)){
			return false;
		}
		Boolean out = true;
		List<Lane[]> map = gameState.lanes;
        int startBlock = map.get(0)[0].position.block;
        Lane[] laneList = map.get(lane - 1);
		int tmp = myCarBlock - startBlock;
		int i = tmp;
		while(laneList[i].terrain != Terrain.FINISH  && laneList[i] != null && i <= tmp + myCarSpeed && out){
			if ((laneList[i].terrain == Terrain.MUD)
				||(laneList[i].terrain == Terrain.OIL_SPILL)
				||(laneList[i].terrain == Terrain.WALL)
				||(laneList[i].isOccupiedByCyberTruck == true)){
				out = false;
			}
			else{
				i++;
			}
		}
		return out;
	}
	private Boolean containPowerUps(List<Object> blocks){
		if((blocks.contains(Terrain.BOOST))
			||(blocks.contains(Terrain.LIZARD))
			||(blocks.contains(Terrain.TWEET))
			||(blocks.contains(Terrain.EMP))
			||(blocks.contains(Terrain.OIL_POWER))){
			return true;
		}
		else {
			return false;
		}
	}
}