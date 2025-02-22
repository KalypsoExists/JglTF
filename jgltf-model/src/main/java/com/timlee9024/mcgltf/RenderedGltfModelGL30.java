package com.timlee9024.mcgltf;

import de.javagl.jgltf.model.*;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

public class RenderedGltfModelGL30 extends RenderedGltfModel {

	public RenderedGltfModelGL30(List<Runnable> gltfRenderData, GltfModel gltfModel) {
		super(gltfRenderData, gltfModel);
	}
	
	@Override
	protected void processSceneModels(List<Runnable> gltfRenderData, List<SceneModel> sceneModels) {
		for(SceneModel sceneModel : sceneModels) {
			RenderedGltfScene renderedGltfScene = new RenderedGltfSceneGL30();
			renderedGltfScenes.add(renderedGltfScene);
			
			for(NodeModel nodeModel : sceneModel.getNodeModels()) {
				Triple<List<Runnable>, List<Runnable>, List<Runnable>> commands = rootNodeModelToCommands.get(nodeModel);
				List<Runnable> vanillaRootRenderCommands;
				List<Runnable> shaderModRootRenderCommands;
				if(commands == null) {
					vanillaRootRenderCommands = new ArrayList<Runnable>();
					shaderModRootRenderCommands = new ArrayList<Runnable>();
					processNodeModel(gltfRenderData, nodeModel, vanillaRootRenderCommands, shaderModRootRenderCommands);
					rootNodeModelToCommands.put(nodeModel, Triple.of(null, vanillaRootRenderCommands, shaderModRootRenderCommands));
				}
				else {
					vanillaRootRenderCommands = commands.getMiddle();
					shaderModRootRenderCommands = commands.getRight();
				}
				renderedGltfScene.vanillaRenderCommands.addAll(vanillaRootRenderCommands);
				renderedGltfScene.shaderModRenderCommands.addAll(shaderModRootRenderCommands);
			}
		}
	}
	
	protected void processNodeModel(List<Runnable> gltfRenderData, NodeModel nodeModel, List<Runnable> vanillaRenderCommands, List<Runnable> shaderModRenderCommands) {
		ArrayList<Runnable> vanillaNodeRenderCommands = new ArrayList<Runnable>();
		ArrayList<Runnable> shaderModNodeRenderCommands = new ArrayList<Runnable>();
		SkinModel skinModel = nodeModel.getSkinModel();
		if(skinModel != null) {
			int jointCount = skinModel.getJoints().size();
			
			float[][] transforms = new float[jointCount][];
			float[] invertNodeTransform = new float[16];
			float[] bindShapeMatrix = new float[16];
			
			List<Runnable> jointMatricesTransformCommands = new ArrayList<Runnable>(jointCount);
			for(int joint = 0; joint < jointCount; joint++) {
				int i = joint;
				float[] transform = transforms[i] = new float[16];
				float[] inverseBindMatrix = new float[16];
				jointMatricesTransformCommands.add(() -> {
					MathUtils.mul4x4(invertNodeTransform, transform, transform);
					skinModel.getInverseBindMatrix(i, inverseBindMatrix);
					MathUtils.mul4x4(transform, inverseBindMatrix, transform);
					MathUtils.mul4x4(transform, bindShapeMatrix, transform);
				});
			}
			
			Runnable jointMatricesTransformCommand = () -> {
				for(int i = 0; i < transforms.length; i++) {
					System.arraycopy(findGlobalTransform(skinModel.getJoints().get(i)), 0, transforms[i], 0, 16);
				}
				MathUtils.invert4x4(findGlobalTransform(nodeModel), invertNodeTransform);
				skinModel.getBindShapeMatrix(bindShapeMatrix);
				jointMatricesTransformCommands.parallelStream().forEach(Runnable::run);
			};
			vanillaNodeRenderCommands.add(jointMatricesTransformCommand);
			shaderModNodeRenderCommands.add(jointMatricesTransformCommand);
			
			for(MeshModel meshModel : nodeModel.getMeshModels()) {
				for(MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
					processMeshPrimitiveModel(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, transforms, vanillaNodeRenderCommands, shaderModNodeRenderCommands);
				}
			}
		}
		else {
			if(!nodeModel.getMeshModels().isEmpty()) {
				for(MeshModel meshModel : nodeModel.getMeshModels()) {
					for(MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
						processMeshPrimitiveModel(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, vanillaNodeRenderCommands, shaderModNodeRenderCommands);
					}
				}
			}
		}
		nodeModel.getChildren().forEach((childNode) -> processNodeModel(gltfRenderData, childNode, vanillaNodeRenderCommands, shaderModNodeRenderCommands));
		if(!vanillaNodeRenderCommands.isEmpty()) {
			vanillaRenderCommands.add(() -> {
				float[] scale = nodeModel.getScale();
				if(scale == null || scale[0] != 0.0F || scale[1] != 0.0F || scale[2] != 0.0F) {
					applyTransformVanilla(nodeModel);
					
					vanillaNodeRenderCommands.forEach(Runnable::run);
				}
			});
			shaderModRenderCommands.add(() -> {
				float[] scale = nodeModel.getScale();
				if(scale == null || scale[0] != 0.0F || scale[1] != 0.0F || scale[2] != 0.0F) {
					applyTransformShaderMod(nodeModel);
					
					shaderModNodeRenderCommands.forEach(Runnable::run);
				}
			});
		}
	}
}
