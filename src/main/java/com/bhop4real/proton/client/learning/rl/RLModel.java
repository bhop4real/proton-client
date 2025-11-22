package com.bhop4real.proton.client.learning.rl;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Q-learning model representation using a Q-table.
 * State: discretized recent click intervals and session info
 * Action: click delay in milliseconds
 */
public class RLModel
{
    // Q-table: state -> action -> Q-value
    private final Map<String, Map<Integer, Double>> qTable;
    
    // Action space: click delays from 50ms to 200ms in 10ms increments
    private static final int MIN_DELAY = 50;
    private static final int MAX_DELAY = 200;
    private static final int DELAY_STEP = 10;
    private static final int ACTION_COUNT = (MAX_DELAY - MIN_DELAY) / DELAY_STEP + 1;
    
    // Learning parameters
    private double learningRate = 0.1;
    private double discountFactor = 0.95;
    private double epsilon = 0.3; // Exploration rate
    
    public RLModel()
    {
        this.qTable = new HashMap<>();
    }
    
    public RLModel(Map<String, Map<Integer, Double>> qTable, double learningRate, double discountFactor, double epsilon)
    {
        this.qTable = qTable != null ? new HashMap<>(qTable) : new HashMap<>();
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.epsilon = epsilon;
    }
    
    /**
     * Get Q-value for state-action pair
     */
    public double getQValue(String state, int action)
    {
        Map<Integer, Double> stateActions = qTable.get(state);
        if (stateActions == null)
        {
            return 0.0;
        }
        Double qValue = stateActions.get(action);
        return qValue != null ? qValue : 0.0;
    }
    
    /**
     * Update Q-value using Q-learning update rule
     */
    public void updateQValue(String state, int action, double reward, String nextState)
    {
        Map<Integer, Double> stateActions = qTable.computeIfAbsent(state, k -> new HashMap<>());
        
        double currentQ = stateActions.getOrDefault(action, 0.0);
        double maxNextQ = getMaxQValue(nextState);
        double newQ = currentQ + learningRate * (reward + discountFactor * maxNextQ - currentQ);
        
        stateActions.put(action, newQ);
    }
    
    /**
     * Get maximum Q-value for a state (best action value)
     */
    private double getMaxQValue(String state)
    {
        Map<Integer, Double> stateActions = qTable.get(state);
        if (stateActions == null || stateActions.isEmpty())
        {
            return 0.0;
        }
        return stateActions.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }
    
    /**
     * Select action using epsilon-greedy policy
     */
    public int selectAction(String state, java.util.Random random)
    {
        if (random.nextDouble() < epsilon)
        {
            // Explore: random action
            return MIN_DELAY + random.nextInt(ACTION_COUNT) * DELAY_STEP;
        }
        else
        {
            // Exploit: best known action
            return getBestAction(state);
        }
    }
    
    /**
     * Get best action for a state
     */
    public int getBestAction(String state)
    {
        Map<Integer, Double> stateActions = qTable.get(state);
        if (stateActions == null || stateActions.isEmpty())
        {
            // Default to middle of range
            return (MIN_DELAY + MAX_DELAY) / 2;
        }
        
        int bestAction = MIN_DELAY;
        double bestQ = Double.NEGATIVE_INFINITY;
        
        for (int action = MIN_DELAY; action <= MAX_DELAY; action += DELAY_STEP)
        {
            double qValue = stateActions.getOrDefault(action, 0.0);
            if (qValue > bestQ)
            {
                bestQ = qValue;
                bestAction = action;
            }
        }
        
        return bestAction;
    }
    
    /**
     * Decay epsilon for exploration reduction over time
     */
    public void decayEpsilon(double decayRate)
    {
        epsilon = Math.max(0.05, epsilon * decayRate);
    }
    
    public double getEpsilon()
    {
        return epsilon;
    }
    
    public void setEpsilon(double epsilon)
    {
        this.epsilon = Math.max(0.0, Math.min(1.0, epsilon));
    }
    
    public double getLearningRate()
    {
        return learningRate;
    }
    
    public void setLearningRate(double learningRate)
    {
        this.learningRate = Math.max(0.0, Math.min(1.0, learningRate));
    }
    
    /**
     * Convert model to JSON for serialization
     */
    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("learningRate", learningRate);
        json.addProperty("discountFactor", discountFactor);
        json.addProperty("epsilon", epsilon);
        
        JsonObject qTableJson = new JsonObject();
        for (Map.Entry<String, Map<Integer, Double>> stateEntry : qTable.entrySet())
        {
            JsonObject stateActions = new JsonObject();
            for (Map.Entry<Integer, Double> actionEntry : stateEntry.getValue().entrySet())
            {
                stateActions.addProperty(String.valueOf(actionEntry.getKey()), actionEntry.getValue());
            }
            qTableJson.add(stateEntry.getKey(), stateActions);
        }
        json.add("qTable", qTableJson);
        
        return json;
    }
    
    /**
     * Create model from JSON
     */
    public static RLModel fromJson(JsonObject json)
    {
        double learningRate = json.has("learningRate") ? json.get("learningRate").getAsDouble() : 0.1;
        double discountFactor = json.has("discountFactor") ? json.get("discountFactor").getAsDouble() : 0.95;
        double epsilon = json.has("epsilon") ? json.get("epsilon").getAsDouble() : 0.3;
        
        Map<String, Map<Integer, Double>> qTable = new HashMap<>();
        if (json.has("qTable") && json.get("qTable").isJsonObject())
        {
            JsonObject qTableJson = json.getAsJsonObject("qTable");
            for (Map.Entry<String, com.google.gson.JsonElement> stateEntry : qTableJson.entrySet())
            {
                Map<Integer, Double> stateActions = new HashMap<>();
                if (stateEntry.getValue().isJsonObject())
                {
                    JsonObject actions = stateEntry.getValue().getAsJsonObject();
                    for (Map.Entry<String, com.google.gson.JsonElement> actionEntry : actions.entrySet())
                    {
                        try
                        {
                            int action = Integer.parseInt(actionEntry.getKey());
                            double qValue = actionEntry.getValue().getAsDouble();
                            stateActions.put(action, qValue);
                        }
                        catch (NumberFormatException ignored)
                        {
                        }
                    }
                }
                qTable.put(stateEntry.getKey(), stateActions);
            }
        }
        
        return new RLModel(qTable, learningRate, discountFactor, epsilon);
    }
    
    /**
     * Create state representation from recent click intervals
     */
    public static String createState(long[] recentIntervals, long sessionDuration)
    {
        // Discretize state: bucket intervals and session duration
        StringBuilder state = new StringBuilder("s");
        
        // Use last 3 intervals, discretized to 20ms buckets
        for (int i = 0; i < Math.min(3, recentIntervals.length); i++)
        {
            int bucket = (int)(recentIntervals[i] / 20);
            state.append("_").append(bucket);
        }
        
        // Add session duration bucket (in seconds, bucketed to 10s)
        int sessionBucket = (int)(sessionDuration / 10000);
        state.append("_t").append(sessionBucket);
        
        return state.toString();
    }
    
    public static int getMinDelay()
    {
        return MIN_DELAY;
    }
    
    public static int getMaxDelay()
    {
        return MAX_DELAY;
    }
    
    public static int getDelayStep()
    {
        return DELAY_STEP;
    }
}

