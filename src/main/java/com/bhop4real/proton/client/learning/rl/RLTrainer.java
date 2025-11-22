package com.bhop4real.proton.client.learning.rl;

import java.util.Random;

/**
 * Q-learning trainer for click delay optimization
 */
public class RLTrainer
{
    private final Random random = new Random();
    private String currentState;
    private int lastAction;
    private long[] recentIntervals;
    private int intervalIndex;
    private static final int INTERVAL_HISTORY_SIZE = 3;
    
    public RLTrainer()
    {
        this.recentIntervals = new long[INTERVAL_HISTORY_SIZE];
        this.intervalIndex = 0;
        this.currentState = "s_0_0_0_t0";
    }
    
    /**
     * Select next action (click delay) using the model
     */
    public int selectAction(RLModel model, long sessionDuration)
    {
        String state = RLModel.createState(recentIntervals, sessionDuration);
        int action = model.selectAction(state, random);
        
        this.currentState = state;
        this.lastAction = action;
        
        return action;
    }
    
    /**
     * Update model with reward after action
     */
    public void updateModel(RLModel model, double reward, long sessionDuration)
    {
        String nextState = RLModel.createState(recentIntervals, sessionDuration);
        model.updateQValue(currentState, lastAction, reward, nextState);
        currentState = nextState;
    }
    
    /**
     * Record a click interval for state representation
     */
    public void recordInterval(long interval)
    {
        recentIntervals[intervalIndex] = interval;
        intervalIndex = (intervalIndex + 1) % INTERVAL_HISTORY_SIZE;
    }
    
    /**
     * Reset training state
     */
    public void reset()
    {
        this.recentIntervals = new long[INTERVAL_HISTORY_SIZE];
        this.intervalIndex = 0;
        this.currentState = "s_0_0_0_t0";
        this.lastAction = RLModel.getMinDelay();
    }
    
    public String getCurrentState()
    {
        return currentState;
    }
}

