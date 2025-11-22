package com.bhop4real.proton.client.module.settings;

public class DoubleSetting extends Setting
{
	private double value;
	private final double min;
	private final double max;
	private final double step;
	
	public DoubleSetting(String name, String description, double defaultValue, double min, double max, double step)
	{
		super(name, description);
		this.min = min;
		this.max = max;
		this.step = step <= 0.0 ? 0.01 : step;
		setValue(defaultValue);
	}
	
	public double getValue()
	{
		return value;
	}
	
	public void setValue(double value)
	{
		// Clamp
		double clamped = Math.max(min, Math.min(max, value));
		// Quantize to step
		double quantized = Math.round(clamped / step) * step;
		// Round to two decimals to avoid floating artifacts
		this.value = Math.round(quantized * 100.0) / 100.0;
	}
	
	public double getMin()
	{
		return min;
	}
	
	public double getMax()
	{
		return max;
	}
	
	public double getStep()
	{
		return step;
	}
}


