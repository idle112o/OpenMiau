package myau.motionblur;

import net.minecraft.client.shader.ShaderGroup;

public interface MotionBlurShaderHook {
    ShaderGroup myau$getMotionBlurShader();

    void myau$setMotionBlurShader(ShaderGroup shaderGroup);
}
