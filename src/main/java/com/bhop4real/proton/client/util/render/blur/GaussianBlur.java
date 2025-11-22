package com.bhop4real.proton.client.util.render.blur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static com.bhop4real.proton.client.util.render.blur.StencilUtil.checkSetupFBO;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUniform1;

public class GaussianBlur
{
    private static final BlurShader gaussianBlur = new BlurShader("proton:shaders/gaussian.frag");
    private static Framebuffer framebuffer = new Framebuffer(1, 1, false);
    private static boolean preloaded;

    private static void setupUniforms(float dir1, float dir2, int radius)
    {
        Minecraft mc = Minecraft.getMinecraft();
        gaussianBlur.setUniformi("textureIn", 0);
        gaussianBlur.setUniformf("texelSize", 1.0F / (float) mc.displayWidth, 1.0F / (float) mc.displayHeight);
        gaussianBlur.setUniformf("direction", dir1, dir2);
        gaussianBlur.setUniformf("radius", radius);

        final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(64);
        for (int i = 0; i <= radius && i < 64; i++)
        {
            // simple gaussian weight approximation
            float sigma = Math.max(1f, radius / 2f);
            float x = i;
            float weight = (float) Math.exp(-(x * x) / (2.0f * sigma * sigma));
            weightBuffer.put(weight);
        }
        weightBuffer.rewind();
        glUniform1(gaussianBlur.getUniform("weights"), weightBuffer);
    }

    private static Framebuffer ensureFramebuffer(Framebuffer fb)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (fb == null || fb.framebufferWidth != mc.displayWidth || fb.framebufferHeight != mc.displayHeight)
        {
            fb = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
        }
        return fb;
    }

    public static void startBlur()
    {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getFramebuffer().bindFramebuffer(false);
        checkSetupFBO(mc.getFramebuffer());
        glClear(GL_STENCIL_BUFFER_BIT);
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_ALWAYS, 1, 1);
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
        glColorMask(false, false, false, false);
    }

    public static void endBlur(int radius, float compression)
    {
        Minecraft mc = Minecraft.getMinecraft();
        StencilUtil.readStencilBuffer(1);

        framebuffer = ensureFramebuffer(framebuffer);

        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(compression, 0, radius);

        glBindTexture(GL_TEXTURE_2D, mc.getFramebuffer().framebufferTexture);
        BlurShader.drawQuads();
        framebuffer.unbindFramebuffer();
        gaussianBlur.unload();

        mc.getFramebuffer().bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(0, compression, radius);

        glBindTexture(GL_TEXTURE_2D, framebuffer.framebufferTexture);
        BlurShader.drawQuads();
        gaussianBlur.unload();

        StencilUtil.uninitStencilBuffer();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void preload()
    {
        if (preloaded)
        {
            return;
        }

        framebuffer = ensureFramebuffer(framebuffer);
        try
        {
            gaussianBlur.init();
        }
        finally
        {
            gaussianBlur.unload();
        }
        preloaded = true;
    }
}


