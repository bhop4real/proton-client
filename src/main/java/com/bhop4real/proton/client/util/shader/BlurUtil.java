package com.bhop4real.proton.client.util.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Utility for applying Gaussian blur effects using shaders
 */
public final class BlurUtil
{
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static ShaderUtil gaussianBlur;
    private static Framebuffer framebuffer;
    
    static
    {
        try
        {
            gaussianBlur = new ShaderUtil("#version 120\n" +
                "\n" +
                "uniform sampler2D textureIn;\n" +
                "uniform vec2 texelSize;\n" +
                "uniform vec2 direction;\n" +
                "uniform float radius;\n" +
                "uniform float weights[256];\n" +
                "\n" +
                "#define offset direction * texelSize\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 innerColor = texture2D(textureIn, gl_TexCoord[0].st);\n" +
                "    innerColor *= weights[0];\n" +
                "    \n" +
                "    for (float r = 1.0; r <= radius; r++) {\n" +
                "        vec4 colorCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);\n" +
                "        vec4 colorCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);\n" +
                "        innerColor += (colorCurrent1 + colorCurrent2) * weights[int(r)];\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = innerColor;\n" +
                "}\n");
        }
        catch (Exception e)
        {
            // Shader creation failed, blur will use fallback
        }
    }
    
    private BlurUtil() {} // Prevent instantiation
    
    private static void setupUniforms(float dir1, float dir2, int radius)
    {
        if (gaussianBlur == null) return;
        
        ScaledResolution sr = new ScaledResolution(mc);
        gaussianBlur.setUniformi("textureIn", 0);
        gaussianBlur.setUniformf("texelSize", 1.0F / (float) sr.getScaledWidth(), 1.0F / (float) sr.getScaledHeight());
        gaussianBlur.setUniformf("direction", dir1, dir2);
        gaussianBlur.setUniformf("radius", radius);
        
        FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);
        for (int i = 0; i <= radius; i++)
        {
            weightBuffer.put(calculateGaussianValue(i, radius / 2.0F));
        }
        weightBuffer.rewind();
        gaussianBlur.setUniform1fv("weights", weightBuffer);
    }
    
    private static float calculateGaussianValue(int x, float sigma)
    {
        double variance = sigma * sigma;
        return (float) (1.0 / Math.sqrt(2.0 * Math.PI * variance) * Math.exp(-(x * x) / (2.0 * variance)));
    }
    
    private static Framebuffer createFrameBuffer(Framebuffer framebuffer)
    {
        if (framebuffer == null)
        {
            ScaledResolution sr = new ScaledResolution(mc);
            framebuffer = new Framebuffer(sr.getScaledWidth(), sr.getScaledHeight(), false);
            framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
        
        ScaledResolution sr = new ScaledResolution(mc);
        if (framebuffer.framebufferWidth != sr.getScaledWidth() || framebuffer.framebufferHeight != sr.getScaledHeight())
        {
            framebuffer.deleteFramebuffer();
            framebuffer = new Framebuffer(sr.getScaledWidth(), sr.getScaledHeight(), false);
            framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
        
        return framebuffer;
    }
    
    /**
     * Applies Gaussian blur to the background
     * Properly separated layer that doesn't interfere with GUI rendering
     * @param intensity Blur intensity (0-10)
     */
    public static void applyBlur(float intensity)
    {
        if (gaussianBlur == null)
        {
            // Fallback to simple dark overlay
            applyDarkOverlay();
            return;
        }
        
        // Save OpenGL state completely - this is a separate layer
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        ScaledResolution sr = new ScaledResolution(mc);
        
        int radius = Math.max(1, (int)(intensity * 2.0F));
        radius = Math.min(radius, 64);
        float compression = 1.0F;
        
        // Create or update framebuffer
        framebuffer = createFrameBuffer(framebuffer);
        
        // Save current matrices - CRITICAL for proper restoration
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, sr.getScaledWidth_double(), sr.getScaledHeight_double(), 0.0, 1000.0, 3000.0);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        // Horizontal blur pass
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(compression, 0, radius);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getFramebuffer().framebufferTexture);
        ShaderUtil.drawQuads();
        framebuffer.unbindFramebuffer();
        gaussianBlur.unload();
        
        // Vertical blur pass - render to main framebuffer
        mc.getFramebuffer().bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(0, compression, radius);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
        ShaderUtil.drawQuads();
        gaussianBlur.unload();
        
        // CRITICAL: Restore matrices completely before drawing overlay
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        
        // Draw dark overlay on top of blurred background
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(0, 0);
        GL11.glVertex2d(sr.getScaledWidth(), 0);
        GL11.glVertex2d(sr.getScaledWidth(), sr.getScaledHeight());
        GL11.glVertex2d(0, sr.getScaledHeight());
        GL11.glEnd();
        
        // Restore OpenGL state completely
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }
    
    /**
     * Applies a dark overlay when blur isn't available
     * Simple implementation that doesn't modify OpenGL matrices
     */
    private static void applyDarkOverlay()
    {
        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();
        
        // Save state
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        // Draw dark overlay
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.75F);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(0, 0);
        GL11.glVertex2d(width, 0);
        GL11.glVertex2d(width, height);
        GL11.glVertex2d(0, height);
        GL11.glEnd();
        
        // Restore state
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }
}

