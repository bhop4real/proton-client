package com.bhop4real.proton.client.util.render.blur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTPackedDepthStencil;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class StencilUtil
{
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void checkSetupFBO(Framebuffer framebuffer)
    {
        if (framebuffer != null && framebuffer.depthBuffer > -1)
        {
            setupFBO(framebuffer);
            framebuffer.depthBuffer = -1;
        }
    }

    public static void dispose()
    {
        GL11.glDisable(GL_STENCIL_TEST);
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

    public static void write(boolean renderClipLayer)
    {
        checkSetupFBO(mc.getFramebuffer());
        GL11.glClearStencil(0);
        GL11.glClear(GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFFFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        if (!renderClipLayer)
        {
            GlStateManager.colorMask(false, false, false, false);
        }
    }

    public static void bindReadStencilBuffer(final int ref)
    {
        glColorMask(true, true, true, true);
        glStencilFunc(GL_EQUAL, ref, 1);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
    }

    public static void setupFBO(Framebuffer framebuffer)
    {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthBuffer);
        final int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
        EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID);
        EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, mc.displayWidth, mc.displayHeight);
        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID);
        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID);
    }

    public static void initStencilToWrite()
    {
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        checkSetupFBO(Minecraft.getMinecraft().getFramebuffer());
        glClear(GL_STENCIL_BUFFER_BIT);
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_ALWAYS, 1, 1);
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
        glColorMask(false, false, false, false);
    }

    public static void readStencilBuffer(int ref)
    {
        glColorMask(true, true, true, true);
        glStencilFunc(GL_EQUAL, ref, 1);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
    }

    public static void uninitStencilBuffer()
    {
        glDisable(GL_STENCIL_TEST);
    }
}


