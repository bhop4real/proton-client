package com.bhop4real.proton.client.util.render.blur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class BlurShader
{
    private final int programID;

    public BlurShader(String fragmentShaderLoc)
    {
        this(fragmentShaderLoc, "proton:shaders/vertex.vsh");
    }

    public BlurShader(String fragmentShaderLoc, String vertexShaderLoc)
    {
        int program = glCreateProgram();
        try
        {
            int fragmentShaderID = createShader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(fragmentShaderLoc)).getInputStream(), GL_FRAGMENT_SHADER);
            glAttachShader(program, fragmentShaderID);
            int vertexShaderID = createShader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(vertexShaderLoc)).getInputStream(), GL_VERTEX_SHADER);
            glAttachShader(program, vertexShaderID);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == 0)
        {
            throw new IllegalStateException("Shader failed to link!");
        }
        this.programID = program;
    }

    public void init()
    {
        glUseProgram(programID);
    }

    public void unload()
    {
        glUseProgram(0);
    }

    public int getUniform(String name)
    {
        return glGetUniformLocation(programID, name);
    }

    public void setUniformf(String name, float... args)
    {
        int loc = glGetUniformLocation(programID, name);
        switch (args.length)
        {
            case 1: glUniform1f(loc, args[0]); break;
            case 2: glUniform2f(loc, args[0], args[1]); break;
            case 3: glUniform3f(loc, args[0], args[1], args[2]); break;
            case 4: glUniform4f(loc, args[0], args[1], args[2], args[3]); break;
        }
    }

    public void setUniformi(String name, int... args)
    {
        int loc = glGetUniformLocation(programID, name);
        if (args.length > 1) glUniform2i(loc, args[0], args[1]);
        else glUniform1i(loc, args[0]);
    }

    public static void drawQuads()
    {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        float width = (float) sr.getScaledWidth_double();
        float height = (float) sr.getScaledHeight_double();
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glEnd();
    }

    private int createShader(InputStream inputStream, int shaderType)
    {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, readInputStream(inputStream));
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0)
        {
            throw new IllegalStateException("Shader failed to compile: " + glGetShaderInfoLog(shader, 4096));
        }
        return shader;
    }

    private String readInputStream(InputStream inputStream)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = br.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }
}



