package com.bhop4real.proton.client.util.render.blur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import static com.bhop4real.proton.client.util.render.blur.StencilUtil.checkSetupFBO;
import static org.lwjgl.opengl.GL11.*;

/**
 * Kawase blur implementation with ping-pong framebuffers.
 * Provides a softer blur than the gaussian shader for UI backgrounds.
 */
public final class KawaseBlur
{
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final BlurShader kawaseShader = new BlurShader("proton:shaders/kawase.frag");

    private static Framebuffer framebufferA = new Framebuffer(1, 1, false);
    private static Framebuffer framebufferB = new Framebuffer(1, 1, false);
    private static boolean preloaded;

    private KawaseBlur() {}

    private static Framebuffer ensureFramebuffer(Framebuffer framebuffer)
    {
        if (framebuffer == null)
        {
            framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
        }

        if (framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight)
        {
            framebuffer.deleteFramebuffer();
            framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
        }

        framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        return framebuffer;
    }

    public static void startBlur()
    {
        mc.getFramebuffer().bindFramebuffer(false);
        checkSetupFBO(mc.getFramebuffer());
        glClear(GL_STENCIL_BUFFER_BIT);
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_ALWAYS, 1, 1);
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
        glColorMask(false, false, false, false);
    }

    public static void endBlur(int iterations, float baseOffset)
    {
        if (iterations <= 0)
        {
            glColorMask(true, true, true, true);
            StencilUtil.uninitStencilBuffer();
            return;
        }

        iterations = Math.max(1, iterations);
        baseOffset = Math.max(0.5F, baseOffset);

        framebufferA = ensureFramebuffer(framebufferA);
        framebufferB = ensureFramebuffer(framebufferB);

        StencilUtil.readStencilBuffer(1);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableBlend();

        Framebuffer currentSource = mc.getFramebuffer();
        Framebuffer currentTarget = framebufferA;

        for (int i = 0; i < iterations; i++)
        {
            float passOffset = baseOffset + i * 0.6F;

            currentTarget.framebufferClear();
            currentTarget.bindFramebuffer(false);

            kawaseShader.init();
            kawaseShader.setUniformi("textureIn", 0);
            kawaseShader.setUniformf("texelSize", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
            kawaseShader.setUniformf("offset", passOffset);

            glBindTexture(GL_TEXTURE_2D, currentSource.framebufferTexture);
            BlurShader.drawQuads();
            kawaseShader.unload();

            currentTarget.unbindFramebuffer();

            currentSource = currentTarget;
            currentTarget = currentTarget == framebufferA ? framebufferB : framebufferA;
        }

        mc.getFramebuffer().bindFramebuffer(false);
        kawaseShader.init();
        kawaseShader.setUniformi("textureIn", 0);
        kawaseShader.setUniformf("texelSize", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
        kawaseShader.setUniformf("offset", 0.5F);

        glBindTexture(GL_TEXTURE_2D, currentSource.framebufferTexture);
        BlurShader.drawQuads();
        kawaseShader.unload();

        StencilUtil.uninitStencilBuffer();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void preload()
    {
        if (preloaded)
        {
            return;
        }

        framebufferA = ensureFramebuffer(framebufferA);
        framebufferB = ensureFramebuffer(framebufferB);
        try
        {
            kawaseShader.init();
        }
        finally
        {
            kawaseShader.unload();
        }
        preloaded = true;
    }
}


