package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitorAdapter;

public class ScenePolisher extends SceneGraphVisitorAdapter {

    private final AssetManager assetManager;
    // Hardcoded light direction to match your Main.java sun
    // In a bigger engine, you'd pass this in via a uniform binding system
    private final Vector3f lightDirection = new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal();

    public ScenePolisher(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public void visit(Geometry geom) {
        Material oldMat = geom.getMaterial();
        if (oldMat == null) return;

        // Try to recover the original color, otherwise default to gray
        ColorRGBA baseColor = ColorRGBA.Gray;
        
        // Check for PBR color
        if (oldMat.getParam("BaseColor") != null) {
            baseColor = (ColorRGBA) oldMat.getParam("BaseColor").getValue();
        } 
        // Check for Legacy Lighting color
        else if (oldMat.getParam("Diffuse") != null) {
            baseColor = (ColorRGBA) oldMat.getParam("Diffuse").getValue();
        }
        // Check for Unshaded color
        else if (oldMat.getParam("Color") != null) {
            baseColor = (ColorRGBA) oldMat.getParam("Color").getValue();
        }

        // Create our new Retro Material
        Material retroMat = new Material(assetManager, "MatDefs/RetroShader.j3md");
        
        // Set the parameters
        retroMat.setColor("BaseColor", baseColor);
        retroMat.setVector3("LightDir", lightDirection);
        retroMat.setFloat("ColorSteps", 4.0f); // 4 bands of light. Set to 0.0f for smooth Gouraud.

        geom.setMaterial(retroMat);
        
        System.out.println("Converted " + geom.getName() + " to Retro Vertex Shading.");
    }
}