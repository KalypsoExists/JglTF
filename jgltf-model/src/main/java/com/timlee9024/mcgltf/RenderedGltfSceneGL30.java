package com.timlee9024.mcgltf;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public class RenderedGltfSceneGL30 extends RenderedGltfScene {

	@Override
	public void renderForVanilla() {
		int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE = GameRenderer.getRendertypeEntitySolidShader();
		int entitySolidProgram = RenderedGltfModel.CURRENT_SHADER_INSTANCE.getId();
		GL20.glUseProgram(entitySolidProgram);
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.PROJECTION_MATRIX.upload();
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.INVERSE_VIEW_ROTATION_MATRIX.upload();
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_START.set(RenderSystem.getShaderFogStart());
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_START.upload();
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_END.set(RenderSystem.getShaderFogEnd());
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_END.upload();
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_COLOR.upload();
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.FOG_SHAPE.upload();
		
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.COLOR_MODULATOR.set(1.0F, 1.0F, 1.0F, 1.0F);
		RenderedGltfModel.CURRENT_SHADER_INSTANCE.COLOR_MODULATOR.upload();
		
		GL20.glUniform1i(GL20.glGetUniformLocation(entitySolidProgram, "Sampler0"), 0);
		GL20.glUniform1i(GL20.glGetUniformLocation(entitySolidProgram, "Sampler1"), 1);
		GL20.glUniform1i(GL20.glGetUniformLocation(entitySolidProgram, "Sampler2"), 2);
		
		RenderSystem.setupShaderLights(RenderedGltfModel.CURRENT_SHADER_INSTANCE);
		RenderedGltfModel.LIGHT0_DIRECTION = new Vector3f(RenderedGltfModel.CURRENT_SHADER_INSTANCE.LIGHT0_DIRECTION.getFloatBuffer());
		RenderedGltfModel.LIGHT1_DIRECTION = new Vector3f(RenderedGltfModel.CURRENT_SHADER_INSTANCE.LIGHT1_DIRECTION.getFloatBuffer());
		
		vanillaRenderCommands.forEach(Runnable::run);
		
		GL20.glUseProgram(currentProgram);
		
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

	@Override
	public void renderForShaderMod() {
		int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		
		RenderedGltfModel.MODEL_VIEW_MATRIX = GL20.glGetUniformLocation(currentProgram, "modelViewMatrix");
		RenderedGltfModel.MODEL_VIEW_MATRIX_INVERSE = GL20.glGetUniformLocation(currentProgram, "modelViewMatrixInverse");
		RenderedGltfModel.NORMAL_MATRIX = GL20.glGetUniformLocation(currentProgram, "normalMatrix");
		
		Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
		projectionMatrix.get(RenderedGltfModel.BUF_FLOAT_16);
		GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(currentProgram, "projectionMatrix"), false, RenderedGltfModel.BUF_FLOAT_16);
		(new Matrix4f(projectionMatrix)).invert().get(RenderedGltfModel.BUF_FLOAT_16);
		GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(currentProgram, "projectionMatrixInverse"), false, RenderedGltfModel.BUF_FLOAT_16);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE3);
		int currentTexture3 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		int currentTexture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		int currentTexture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		
		shaderModRenderCommands.forEach(Runnable::run);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE3);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture3);
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture1);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture0);
		
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

}
