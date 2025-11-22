# Auto Clicker Pattern Scripting API

This document describes the JavaScript API available for scripting custom click patterns in the Auto Clicker's "Open" randomization mode.

## Script Function

Your script must export a function named `getNextDelay` with the following signature:

```javascript
function getNextDelay(lastDelay, config, state) {
    // Your pattern logic here
    return delayInMs;
}
```

### Parameters

- `lastDelay` (number): The last calculated delay in milliseconds
- `config` (object): Current auto clicker configuration
  - `config.minCps` (number): Minimum CPS configured
  - `config.maxCps` (number): Maximum CPS configured
  - `config.minDelay` (number): Minimum delay in milliseconds
  - `config.maxDelay` (number): Maximum delay in milliseconds
  - `config.offsetMin` (number): Advanced offset minimum in milliseconds
  - `config.offsetMax` (number): Advanced offset maximum in milliseconds
- `state` (object): Current pattern state
  - `state.clickCount` (number): Total clicks since module was enabled
  - `state.sessionTime` (number): Session duration in milliseconds
  - `state.lastClickTime` (number): Timestamp of last click in milliseconds

### Return Value

- Must return a number representing the next click delay in milliseconds
- Should be at least 1ms (values below 1 will be clamped)

## Available Functions

### Random Utilities (RandomUtil)

- `randomInt(min, max)` - Returns a random integer between min (inclusive) and max (exclusive)
- `randomDouble()` - Returns a random double between 0.0 (inclusive) and 1.0 (exclusive)
- `randomDoubleMinMax(min, max)` - Returns a random double between min (inclusive) and max (exclusive)
- `randomLong()` - Returns a random long
- `randomBoolean()` - Returns a random boolean
- `randomGaussian()` - Returns a Gaussian distributed double (mean 0, std dev 1)

### Secure Random Utilities (SecureRandomUtil)

- `secureRandomInt(min, max)` - Returns a secure random integer
- `secureRandomDouble()` - Returns a secure random double [0.0, 1.0)
- `secureRandomDoubleMinMax(min, max)` - Returns a secure random double between min and max
- `secureRandomLong()` - Returns a secure random long
- `secureRandomBoolean()` - Returns a secure random boolean
- `secureRandomGaussian()` - Returns a secure Gaussian distributed double

### Time Utilities

- `getCurrentTime()` - Returns current system time in milliseconds
- `getLastDelay()` - Returns the last calculated delay in milliseconds
- `getElapsedTime()` - Returns time elapsed since module was enabled (milliseconds)
- `getLastClickTime()` - Returns timestamp of last click in milliseconds
- `delay(ms)` - Sleep/wait function (may be limited in Minecraft's thread)

### Math Utilities

- `average(array)` - Calculate average of number array
- `min(array)` - Find minimum value in array
- `max(array)` - Find maximum value in array
- `clamp(value, min, max)` - Clamp value between min and max
- `lerp(a, b, t)` - Linear interpolation between a and b (t is factor 0-1)
- `normalize(value, min, max)` - Normalize value to [0, 1] range
- `round(value)` - Round to nearest integer
- `floor(value)` - Floor value
- `ceil(value)` - Ceil value
- `abs(value)` - Absolute value
- `pow(base, exp)` - Power function
- `sqrt(value)` - Square root
- `sin(value)`, `cos(value)`, `tan(value)` - Trigonometry functions
- `log(value)`, `log10(value)` - Logarithm functions

## Example Scripts

### Basic Random Delay

```javascript
function getNextDelay(lastDelay, config, state) {
    // Random delay between configured min and max
    return randomInt(config.minDelay, config.maxDelay + 1);
}
```

### Gaussian Distribution

```javascript
function getNextDelay(lastDelay, config, state) {
    // Gaussian distribution around midpoint
    var mid = (config.minDelay + config.maxDelay) / 2;
    var spread = (config.maxDelay - config.minDelay) / 6;
    var delay = mid + randomGaussian() * spread;
    return clamp(delay, config.minDelay, config.maxDelay);
}
```

### Adaptive Pattern

```javascript
function getNextDelay(lastDelay, config, state) {
    // Slow down over time
    var baseDelay = config.minDelay;
    var timeFactor = Math.min(state.sessionTime / 60000, 1.0); // Normalize over 1 minute
    var adaptiveDelay = baseDelay + (config.maxDelay - baseDelay) * timeFactor;
    
    // Add some random variation
    var variation = randomInt(-10, 11);
    return clamp(adaptiveDelay + variation, config.minDelay, config.maxDelay);
}
```

### CPS-Based Pattern

```javascript
function getNextDelay(lastDelay, config, state) {
    // Convert CPS to delay
    var targetCPS = randomDoubleMinMax(config.minCps, config.maxCps);
    var delay = 1000 / targetCPS;
    
    // Add small random variation
    var variation = randomGaussian() * 5;
    return Math.max(1, delay + variation);
}
```

### Pattern with Memory

```javascript
var lastDelays = [];

function getNextDelay(lastDelay, config, state) {
    // Track last few delays
    lastDelays.push(lastDelay);
    if (lastDelays.length > 10) {
        lastDelays.shift();
    }
    
    // Use average of recent delays as base
    var baseDelay = lastDelays.length > 0 ? average(lastDelays) : config.minDelay;
    
    // Add random offset
    var offset = randomInt(-20, 21);
    return clamp(baseDelay + offset, config.minDelay, config.maxDelay);
}
```

## Error Handling

- If your script throws an error or returns an invalid value, the system will fall back to a default delay (50ms or the last delay)
- Always ensure your return value is a valid number
- Validate inputs and clamp outputs to reasonable ranges
- Check for null/undefined values when accessing config or state properties

## Notes

- Scripts are loaded dynamically when the pattern file path changes or the module is enabled
- Script execution is fast and should not block Minecraft's main thread
- The `delay()` function may not work as expected in Minecraft's thread context
- All delays are in milliseconds

