package com.bhop4real.proton.client.util.shader;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

/**
 * Utility for loading and using GLSL shaders
 */
public final class ShaderUtil
{
    private final int programID;
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    public ShaderUtil(String fragmentShaderSource)
    {
        int program = GL20.glCreateProgram();
        
        try
        {
            int fragmentShaderID = createShader(new java.io.ByteArrayInputStream(fragmentShaderSource.getBytes()), GL20.GL_FRAGMENT_SHADER);
            
            String vertexShader = "#version 120\n\nvoid main() {\n    gl_TexCoord[0] = gl_MultiTexCoord0;\n    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n}\n";
            int vertexShaderID = createShader(new java.io.ByteArrayInputStream(vertexShader.getBytes()), GL20.GL_VERTEX_SHADER);
            
            GL20.glAttachShader(program, fragmentShaderID);
            GL20.glAttachShader(program, vertexShaderID);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to create shader", e);
        }
        
        GL20.glLinkProgram(program);
        int status = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        
        if (status == 0)
        {
            String log = GL20.glGetProgramInfoLog(program, 1024);
            throw new IllegalStateException("Shader failed to link: " + log);
        }
        
        this.programID = program;
    }
    
    public void init()
    {
        GL20.glUseProgram(programID);
    }
    
    public void unload()
    {
        GL20.glUseProgram(0);
    }
    
    public int getUniform(String name)
    {
        return GL20.glGetUniformLocation(programID, name);
    }
    
    public void setUniformf(String name, float... args)
    {
        int loc = GL20.glGetUniformLocation(programID, name);
        if (loc == -1) return;
        
        switch (args.length)
        {
            case 1:
                GL20.glUniform1f(loc, args[0]);
                break;
            case 2:
                GL20.glUniform2f(loc, args[0], args[1]);
                break;
            case 3:
                GL20.glUniform3f(loc, args[0], args[1], args[2]);
                break;
            case 4:
                GL20.glUniform4f(loc, args[0], args[1], args[2], args[3]);
                break;
        }
    }
    
    public void setUniformi(String name, int... args)
    {
        int loc = GL20.glGetUniformLocation(programID, name);
        if (loc == -1) return;
        
        if (args.length > 1)
            GL20.glUniform2i(loc, args[0], args[1]);
        else
            GL20.glUniform1i(loc, args[0]);
    }
    
    public void setUniform1fv(String name, FloatBuffer buffer)
    {
        int loc = GL20.glGetUniformLocation(programID, name);
        if (loc == -1) return;
        GL20.glUniform1(loc, buffer);
    }
    
    public static void drawQuads(float x, float y, float width, float height)
    {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
    }
    
    public static void drawQuads()
    {
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        float width = (float) sr.getScaledWidth_double();
        float height = (float) sr.getScaledHeight_double();
        drawQuads(0, 0, width, height);
    }
    
    private int createShader(InputStream inputStream, int shaderType)
    {
        int shader = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shader, readInputStream(inputStream));
        GL20.glCompileShader(shader);
        
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0)
        {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            throw new IllegalStateException(String.format("Shader (%s) failed to compile: %s", shaderType, log));
        }
        
        return shader;
    }
    
    private String readInputStream(InputStream inputStream)
    {
        StringBuilder stringBuilder = new StringBuilder();
        
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream)))
        {
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line).append('\n');
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to read shader source", e);
        }
        
        return stringBuilder.toString();
    }
}

